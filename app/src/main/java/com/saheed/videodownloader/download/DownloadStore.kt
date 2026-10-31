package com.saheed.videodownloader.download

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import java.io.File
import java.util.UUID

/**
 * The list behind the Downloads tab, saved as a small JSON file so it survives closing the app.
 * The download worker runs in the same app process, so its updates reach the screen straight away
 * through [records] and [progress].
 *
 * It also queues playlist videos: they wait here as QUEUED rows, and [pump] hands the next ones to
 * WorkManager whenever one ends, keeping [DownloadWorker.AT_A_TIME] running per playlist.
 */
object DownloadStore {

    private const val TAG = "DownloadStore"
    private const val MAX_RECORDS = 200

    private var file: File? = null

    private val _records = MutableStateFlow<List<DownloadRecord>>(emptyList())
    /** Newest first. */
    val records: StateFlow<List<DownloadRecord>> = _records

    // Percent per running download. Kept in memory only: it changes many times a second.
    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress

    /** Loads the saved list. Called once from App.onCreate (and by tests with a temp file). */
    @Synchronized
    fun init(file: File) {
        this.file = file
        _records.value = try {
            if (file.length() > 0) {
                val array = JSONArray(file.readText())
                List(array.length()) { DownloadRecord.fromJson(array.getJSONObject(it)) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read the downloads list, starting fresh", e)
            emptyList()
        }
    }

    @Synchronized
    fun add(record: DownloadRecord) = addAll(listOf(record))

    /** Adds several rows with one save; [records] end up on top in the order given. */
    @Synchronized
    fun addAll(records: List<DownloadRecord>) = save((records + _records.value).take(MAX_RECORDS))

    /** Applies [change] to the record with [id], if it's still in the list. */
    @Synchronized
    fun update(id: String, change: (DownloadRecord) -> DownloadRecord) =
        save(_records.value.map { if (it.id == id) change(it) else it })

    @Synchronized
    fun remove(id: String) = save(_records.value.filterNot { it.id == id })

    fun find(id: String): DownloadRecord? = _records.value.firstOrNull { it.id == id }

    fun setProgress(id: String, progress: Progress) = _progress.update { it + (id to progress) }

    fun clearProgress(id: String) = _progress.update { it - id }

    // ---- Playlist queue ----

    /** Starts the next waiting videos of playlist [group], up to [DownloadWorker.AT_A_TIME] at once. */
    @Synchronized
    fun pump(context: Context, group: String) {
        val next = nextToStart(_records.value, group)
        if (next.isEmpty()) return
        val ids = next.map { it.id }.toSet()
        save(_records.value.map { if (it.id in ids) it.copy(status = DownloadStatus.RUNNING) else it })
        next.forEach { DownloadWorker.start(context, it) }
    }

    /** The waiting videos of [group] to start now, in list order. Pure, so tests can check it. */
    fun nextToStart(records: List<DownloadRecord>, group: String): List<DownloadRecord> {
        val inGroup = records.filter { it.group == group }
        val free = DownloadWorker.AT_A_TIME - inGroup.count { it.status == DownloadStatus.RUNNING }
        return if (free <= 0) emptyList() else inGroup.filter { it.status == DownloadStatus.QUEUED }.take(free)
    }

    // ---- One download ----

    /** Stops one download. Only this one: the rest of its playlist carries on. */
    fun cancel(context: Context, id: String) {
        val record = find(id) ?: return
        // Marked first, so the worker's "start the next one" never restarts it.
        update(id) { if (it.isActive || it.status == DownloadStatus.PAUSED) it.copy(status = DownloadStatus.CANCELLED) else it }
        // A waiting playlist video has no WorkManager job yet; this is then harmless.
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(record.id))
    }

    /**
     * Starts a failed or cancelled download again. A playlist video goes back to waiting in the
     * same place in its playlist; a single video becomes a new row replacing the old one.
     * Used by the Downloads tab and the notification's Retry button.
     */
    fun retry(context: Context, id: String) {
        val old = find(id) ?: return
        if (old.group != null) {
            requeueInPlace(context, old) { it }
        } else {
            replace(context, id)
            DownloadWorker.enqueue(context, old.url, old.title, old.thumbnailUrl, old.toOption(), old.toExtras())
        }
    }

    /**
     * "Change quality": stops download [id] if it's still running and starts the same video again
     * with the newly picked [option] and [extras], replacing the old row (in place for a playlist video).
     */
    fun changeQuality(context: Context, id: String, meta: VideoMeta, option: QualityOption, extras: DownloadExtras) {
        val old = find(id) ?: return
        if (old.group != null) {
            requeueInPlace(context, old) {
                it.copy(
                    label = option.label,
                    format = option.format,
                    isAudio = option.isAudio,
                    subtitles = extras.subtitles,
                    clipStart = extras.clip?.startSeconds,
                    clipEnd = extras.clip?.endSeconds,
                )
            }
        } else {
            replace(context, id)
            DownloadWorker.enqueue(context, old.url, meta.title, meta.thumbnailUrl ?: old.thumbnailUrl, option, extras)
        }
    }

    /** Stops [id] if active, removes its row and its old result notification. */
    private fun replace(context: Context, id: String) {
        val old = find(id) ?: return
        if (old.isActive) WorkManager.getInstance(context).cancelWorkById(UUID.fromString(id))
        remove(id)
        // The old "Download failed" notification is out of date now.
        Notifications.cancel(context, Notifications.resultId(id))
    }

    /**
     * Puts playlist video [old] back to waiting where it is, with [change] applied, then lets the
     * queue start it. It gets a new id: WorkManager can't reuse the id of a job it already ran.
     */
    private fun requeueInPlace(context: Context, old: DownloadRecord, change: (DownloadRecord) -> DownloadRecord) {
        val fresh = change(old).asWaiting()
        save(_records.value.map { if (it.id == old.id) fresh else it })
        if (old.status == DownloadStatus.RUNNING) WorkManager.getInstance(context).cancelWorkById(UUID.fromString(old.id))
        Notifications.cancel(context, Notifications.resultId(old.id))
        pump(context, old.group ?: return)
    }

    /** The same download as a new waiting row: new id, no result or error yet. */
    private fun DownloadRecord.asWaiting() = copy(
        id = UUID.randomUUID().toString(),
        status = DownloadStatus.QUEUED,
        error = null,
        uri = null,
        mime = null,
        sizeBytes = null,
        createdAt = System.currentTimeMillis(),
    )

    // ---- A whole playlist ----

    /** "Stop": running and waiting videos become Stopped; nothing more downloads until Resume. */
    fun stopGroup(context: Context, group: String) =
        markThenCancelJobs(context, group, setOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED), DownloadStatus.PAUSED)

    /** "Cancel": everything not finished yet becomes Cancelled. */
    fun cancelGroup(context: Context, group: String) = markThenCancelJobs(
        context, group,
        setOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED, DownloadStatus.PAUSED), DownloadStatus.CANCELLED,
    )

    /** "Resume": stopped videos wait again (they restart from the beginning). */
    fun resumeGroup(context: Context, group: String) =
        requeueGroup(context, group, setOf(DownloadStatus.PAUSED))

    /** "Retry": failed and cancelled videos of the playlist wait again, in place. */
    fun retryGroup(context: Context, group: String) =
        requeueGroup(context, group, setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED))

    /** "Remove playlist from list": its rows go; the downloaded files stay in Downloads. */
    @Synchronized
    fun removeGroup(group: String) = save(_records.value.filterNot { it.group == group })

    /**
     * Marks [group]'s rows in [from] as [to] first, then stops their WorkManager jobs. In this order
     * a stopped job's "start the next one" finds nothing waiting, so it can't start a new video.
     */
    private fun markThenCancelJobs(context: Context, group: String?, from: Set<DownloadStatus>, to: DownloadStatus) {
        val hit = synchronized(this) {
            val hit = _records.value.filter { (group == null || it.group == group) && it.status in from }
            val ids = hit.map { it.id }.toSet()
            save(_records.value.map { if (it.id in ids) it.copy(status = to) else it })
            hit
        }
        val workManager = WorkManager.getInstance(context)
        hit.filter { it.status == DownloadStatus.RUNNING || it.group == null }
            .forEach { workManager.cancelWorkById(UUID.fromString(it.id)) }
    }

    private fun requeueGroup(context: Context, group: String, from: Set<DownloadStatus>) {
        synchronized(this) {
            save(_records.value.map { if (it.group == group && it.status in from) it.asWaiting() else it })
        }
        pump(context, group)
    }

    // ---- Everything ----

    /** "Cancel all": stops every download that's running, waiting or stopped. */
    fun cancelAll(context: Context) = markThenCancelJobs(
        context, group = null,
        setOf(DownloadStatus.RUNNING, DownloadStatus.QUEUED, DownloadStatus.PAUSED), DownloadStatus.CANCELLED,
    )

    /**
     * "Retry all": playlist videos go back to waiting in their playlist (which keeps them two at a
     * time); single videos start again as new rows.
     */
    fun retryAll(context: Context) {
        val stopped = _records.value.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        stopped.mapNotNull { it.group }.toSet().forEach { retryGroup(context, it) }
        stopped.filter { it.group == null }.forEach { retry(context, it.id) }
        stopped.forEach { Notifications.cancel(context, Notifications.resultId(it.id)) }
    }

    /** Clear menu: removes every row with one of [statuses]. Files stay in Downloads. */
    @Synchronized
    fun clear(statuses: Set<DownloadStatus>) = save(_records.value.filterNot { it.status in statuses })

    /**
     * Rows still marked Waiting/Downloading whose job has ended without saying so (for example
     * the phone killed the app mid-download) become Failed, so they get a Retry button. Waiting
     * playlist videos have no job yet, so they're skipped; their playlists are started up again.
     * Blocking: call from a background thread.
     */
    fun markInterrupted(context: Context) {
        val workManager = WorkManager.getInstance(context)
        _records.value
            .filter { it.status == DownloadStatus.RUNNING || (it.status == DownloadStatus.QUEUED && it.group == null) }
            .forEach { record ->
                val info = workManager.getWorkInfoById(UUID.fromString(record.id)).get()
                if (info == null || info.state.isFinished) {
                    update(record.id) {
                        if (it.isActive) it.copy(status = DownloadStatus.FAILED, error = "Interrupted. Tap Retry.") else it
                    }
                }
            }
        _records.value.filter { it.status == DownloadStatus.QUEUED }.mapNotNull { it.group }.toSet()
            .forEach { pump(context, it) }
    }

    private fun save(list: List<DownloadRecord>) {
        _records.value = list
        val target = file ?: return
        try {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            target.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't save the downloads list", e)
        }
    }
}
