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
 */
object DownloadStore {

    private const val TAG = "DownloadStore"
    private const val MAX_RECORDS = 200

    private var file: File? = null

    private val _records = MutableStateFlow<List<DownloadRecord>>(emptyList())
    /** Newest first. */
    val records: StateFlow<List<DownloadRecord>> = _records

    // Percent per running download. Kept in memory only: it changes many times a second.
    private val _progress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val progress: StateFlow<Map<String, Int>> = _progress

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

    fun setProgress(id: String, percent: Int) = _progress.update { it + (id to percent) }

    fun clearProgress(id: String) = _progress.update { it - id }

    /** Stops an active download. Marked Cancelled straight away, in case it hadn't started yet. */
    fun cancel(context: Context, id: String) {
        val record = find(id)
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(id))
        update(id) { if (it.isActive) it.copy(status = DownloadStatus.CANCELLED) else it }
        // WorkManager also cancels the playlist videos waiting after this one; show that.
        record?.group?.let { cancelWaitingInGroup(it) }
    }

    /** Marks the still-waiting videos of playlist download [group] as cancelled. */
    @Synchronized
    fun cancelWaitingInGroup(group: String) = save(
        _records.value.map {
            if (it.group == group && it.status == DownloadStatus.QUEUED) it.copy(status = DownloadStatus.CANCELLED) else it
        }
    )

    /**
     * Starts the same download again as a new row, replacing the failed/cancelled one.
     * Used by the Downloads tab and the notification's Retry button.
     */
    fun retry(context: Context, id: String) {
        val old = replace(context, id) ?: return
        DownloadWorker.enqueue(context, old.url, old.title, old.thumbnailUrl, old.toOption(), old.toExtras())
    }

    /**
     * "Change quality": stops download [id] if it's still running and starts the same video again
     * with the newly picked [option] and [extras], as a new row replacing the old one.
     */
    fun changeQuality(context: Context, id: String, meta: VideoMeta, option: QualityOption, extras: DownloadExtras) {
        val old = replace(context, id) ?: return
        DownloadWorker.enqueue(context, old.url, meta.title, meta.thumbnailUrl ?: old.thumbnailUrl, option, extras)
    }

    /** Stops [id] if active, removes its row and its old result notification; returns the old row. */
    private fun replace(context: Context, id: String): DownloadRecord? {
        val old = find(id) ?: return null
        if (old.isActive) WorkManager.getInstance(context).cancelWorkById(UUID.fromString(id))
        remove(id)
        // The old "Download failed" notification is out of date now.
        Notifications.cancel(context, Notifications.resultId(id))
        return old
    }

    /**
     * Rows still marked Waiting/Downloading whose job has ended without saying so (for example
     * the phone killed the app mid-download) become Failed, so they get a Retry button.
     * Blocking: call from a background thread.
     */
    fun markInterrupted(context: Context) {
        val workManager = WorkManager.getInstance(context)
        _records.value.filter { it.isActive }.forEach { record ->
            val info = workManager.getWorkInfoById(UUID.fromString(record.id)).get()
            if (info == null || info.state.isFinished) {
                update(record.id) {
                    if (it.isActive) it.copy(status = DownloadStatus.FAILED, error = "Interrupted. Tap Retry.") else it
                }
            }
        }
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
