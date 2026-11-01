package com.saheed.videodownloader.desktop

import com.saheed.videodownloader.download.DownloadRecord
import com.saheed.videodownloader.download.DownloadStatus
import com.saheed.videodownloader.download.ErrorMapper
import com.saheed.videodownloader.download.Progress
import com.saheed.videodownloader.download.YtDlpOptions
import com.saheed.videodownloader.download.nextToStart
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The desktop's downloads: the list (saved as `downloads.json`, same record format as Android),
 * and running them. Each download is one yt-dlp.exe process; playlists run two at a time.
 */
object DesktopDownloads {

    private const val AT_A_TIME = 2
    private val file = File(DesktopSettings.dataDir, "downloads.json")

    // Downloads run here, off the window's thread. SupervisorJob: one failing doesn't stop the others.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _records = MutableStateFlow(load())
    /** Newest first, like on Android. */
    val records: StateFlow<List<DownloadRecord>> = _records

    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress

    /** The yt-dlp process of each running download, so Cancel can stop it. */
    private val processes = ConcurrentHashMap<String, Process>()

    // ---- Starting ----

    fun enqueue(url: String, title: String, thumbnailUrl: String?, option: QualityOption, extras: DownloadExtras = DownloadExtras()) {
        val record = newRecord(url, title, thumbnailUrl, option, extras, group = null, groupTitle = null)
        save(listOf(record) + _records.value)
        start(record)
    }

    /** Every video of [playlist] waits in its own section; [AT_A_TIME] download at once. */
    fun enqueuePlaylist(playlist: Playlist, option: QualityOption, extras: DownloadExtras = DownloadExtras()) {
        if (playlist.entries.isEmpty()) return
        val group = UUID.randomUUID().toString()
        val rows = playlist.entries.map { newRecord(it.url, it.title, it.thumbnailUrl, option, extras, group, playlist.title) }
        save(rows + _records.value)
        pump(group)
    }

    /** Starts the next waiting videos of playlist [group] (the shared rule, as on Android). */
    @Synchronized
    fun pump(group: String) {
        nextToStart(_records.value, group, AT_A_TIME).forEach { start(it) }
    }

    @Synchronized
    private fun start(record: DownloadRecord) {
        update(record.id) { it.copy(status = DownloadStatus.RUNNING, error = null) }
        scope.launch { run(record) }
    }

    /** One download: yt-dlp into a work folder, then the finished file moves to the download folder. */
    private fun run(record: DownloadRecord) {
        val work = File(DesktopSettings.dataDir, "work/${record.id}").apply { deleteRecursively(); mkdirs() }
        try {
            val options = YtDlpOptions.build(
                record.format, record.isAudio, record.toExtras(),
                pause = record.group != null,
                videoFormat = DesktopSettings.videoFormat,
            ).flatMap { (name, value) -> listOfNotNull(name, value) }
            // --newline: one progress line at a time, which [Progress.fromLine] reads.
            val args = options + listOf("--newline", "-o", File(work, "%(title).100B.%(ext)s").absolutePath, record.url)
            var lastPercent = -1
            val result = Engine.run(args, onStart = { processes[record.id] = it }) { line ->
                Progress.fromLine(line)?.let { p ->
                    if (p.percent != lastPercent) {
                        lastPercent = p.percent
                        _progress.update { it + (record.id to p) }
                    }
                }
            }
            // Cancelled (or stopped) while it ran: the row already says so.
            if (find(record.id)?.status != DownloadStatus.RUNNING) return
            if (result.exitCode != 0) {
                update(record.id) { it.copy(status = DownloadStatus.FAILED, error = ErrorMapper.toMessage(result.errors)) }
                return
            }
            val produced = YtDlpOptions.mainFile(work) ?: error("Download finished but no file was found")
            val target = uniqueFile(DesktopSettings.downloadDir.apply { mkdirs() }, produced.name)
            Files.move(produced.toPath(), target.toPath())
            update(record.id) {
                it.copy(status = DownloadStatus.DONE, uri = target.absolutePath, mime = mimeOf(target), sizeBytes = target.length())
            }
        } catch (e: Exception) {
            update(record.id) {
                if (it.status == DownloadStatus.RUNNING) it.copy(status = DownloadStatus.FAILED, error = ErrorMapper.toMessage(e.message)) else it
            }
        } finally {
            processes.remove(record.id)
            _progress.update { it - record.id }
            work.deleteRecursively()
            record.group?.let { pump(it) }
        }
    }

    // ---- Actions ----

    /** Stops a download: marks it first, then ends yt-dlp (and the ffmpeg it may have started). */
    fun cancel(id: String) {
        update(id) { if (it.isActive || it.status == DownloadStatus.PAUSED) it.copy(status = DownloadStatus.CANCELLED) else it }
        processes[id]?.let(::kill)
    }

    /** Starts a failed or cancelled download again, in the same row. */
    fun retry(id: String) {
        val old = find(id) ?: return
        val fresh = old.copy(status = DownloadStatus.QUEUED, error = null, uri = null, mime = null, sizeBytes = null)
        save(_records.value.map { if (it.id == id) fresh else it })
        if (old.group != null) pump(old.group!!) else start(fresh)
    }

    /** Removes a row; the file stays in the download folder. */
    fun remove(id: String) = save(_records.value.filterNot { it.id == id })

    // ---- A whole playlist, and everything ----

    /** "Stop": running and waiting videos become Stopped; nothing more starts until Resume. */
    fun stopGroup(group: String) =
        markThenKill({ it.group == group }, setOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED), DownloadStatus.PAUSED)

    /** "Cancel" on a playlist: everything not finished becomes Cancelled. */
    fun cancelGroup(group: String) = markThenKill({ it.group == group }, UNFINISHED, DownloadStatus.CANCELLED)

    /** "Cancel all": every running, waiting or stopped download. */
    fun cancelAll() = markThenKill({ true }, UNFINISHED, DownloadStatus.CANCELLED)

    /** "Resume": stopped videos wait again (they restart from the beginning) and the queue starts them. */
    fun resumeGroup(group: String) = requeue(group, setOf(DownloadStatus.PAUSED))

    /** "Retry" on a playlist: its failed and cancelled videos wait again, in place. */
    fun retryGroup(group: String) = requeue(group, setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED))

    /** "Retry all": playlists retry in place (two at a time); single videos start again. */
    fun retryAll() {
        val stopped = _records.value.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        stopped.mapNotNull { it.group }.toSet().forEach(::retryGroup)
        stopped.filter { it.group == null }.forEach { retry(it.id) }
    }

    /** "Remove playlist from list": its rows go; the files stay in the download folder. */
    fun removeGroup(group: String) = save(_records.value.filterNot { it.group == group })

    /** Clear menu: removes every row with one of [statuses]; the files stay. */
    fun clear(statuses: Set<DownloadStatus>) = save(_records.value.filterNot { it.status in statuses })

    private val UNFINISHED = setOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED, DownloadStatus.PAUSED)

    /**
     * Marks the matching rows in [from] as [to] first, then ends their yt-dlp processes. In this order a
     * stopped download's "start the next one" finds nothing waiting, so no new video starts.
     */
    private fun markThenKill(match: (DownloadRecord) -> Boolean, from: Set<DownloadStatus>, to: DownloadStatus) {
        val hit = synchronized(this) {
            val hit = _records.value.filter { match(it) && it.status in from }.map { it.id }.toSet()
            save(_records.value.map { if (it.id in hit) it.copy(status = to) else it })
            hit
        }
        hit.forEach { id -> processes[id]?.let(::kill) }
    }

    private fun requeue(group: String, from: Set<DownloadStatus>) {
        synchronized(this) {
            save(_records.value.map {
                if (it.group == group && it.status in from) it.copy(status = DownloadStatus.QUEUED, error = null) else it
            })
        }
        pump(group)
    }

    /** Opens a finished download in the PC's default player. */
    fun open(record: DownloadRecord) {
        record.uri?.let { path -> runCatching { Desktop.getDesktop().open(File(path)) } }
    }

    /** Opens File Explorer with the finished file selected. */
    fun showInFolder(record: DownloadRecord) {
        record.uri?.let { path -> runCatching { ProcessBuilder("explorer.exe", "/select,", path).start() } }
    }

    private fun kill(process: Process) {
        process.descendants().forEach { it.destroyForcibly() }
        process.destroyForcibly()
    }

    // ---- The saved list ----

    fun find(id: String): DownloadRecord? = _records.value.firstOrNull { it.id == id }

    @Synchronized
    private fun update(id: String, change: (DownloadRecord) -> DownloadRecord) =
        save(_records.value.map { if (it.id == id) change(it) else it })

    @Synchronized
    private fun save(list: List<DownloadRecord>) {
        _records.value = list.take(500)
        runCatching { file.writeText(JSONArray().apply { _records.value.forEach { put(it.toJson()) } }.toString()) }
    }

    /** Reads the saved list. Downloads that were running when the app closed can't continue: they show as failed. */
    private fun load(): List<DownloadRecord> = runCatching {
        val array = JSONArray(file.readText())
        List(array.length()) { DownloadRecord.fromJson(array.getJSONObject(it)) }
    }.getOrDefault(emptyList()).map {
        if (it.isActive) it.copy(status = DownloadStatus.FAILED, error = "Interrupted when the app closed. Retry to start again.") else it
    }

    private fun newRecord(
        url: String, title: String, thumbnailUrl: String?, option: QualityOption, extras: DownloadExtras,
        group: String?, groupTitle: String?,
    ) = DownloadRecord(
        id = UUID.randomUUID().toString(),
        url = url, title = title, thumbnailUrl = thumbnailUrl,
        label = option.label, format = option.format, isAudio = option.isAudio,
        subtitles = extras.subtitles, clipStart = extras.clip?.startSeconds, clipEnd = extras.clip?.endSeconds,
        group = group, groupTitle = groupTitle,
        status = DownloadStatus.QUEUED, createdAt = System.currentTimeMillis(),
    )
}

/** [name] in [dir], or "name (1).ext", "name (2).ext"… when that file already exists, like Android does. */
fun uniqueFile(dir: File, name: String): File {
    val first = File(dir, name)
    if (!first.exists()) return first
    val base = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
    return generateSequence(1) { it + 1 }.map { File(dir, "$base ($it)$ext") }.first { !it.exists() }
}

fun mimeOf(file: File): String = when (file.extension.lowercase()) {
    "mp4" -> "video/mp4"
    "webm" -> "video/webm"
    "mkv" -> "video/x-matroska"
    "mp3" -> "audio/mpeg"
    "m4a" -> "audio/mp4"
    else -> "application/octet-stream"
}
