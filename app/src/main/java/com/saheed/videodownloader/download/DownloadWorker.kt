package com.saheed.videodownloader.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.UUID

/** One download to start: what [DownloadWorker.enqueueInOrder] takes. */
data class DownloadRequest(
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val option: QualityOption,
    val extras: DownloadExtras,
)

/**
 * Runs one download in the background with *WorkManager* (Android's job scheduler for work that
 * must finish even if the user leaves the app). It runs as a *foreground worker*: it keeps a
 * notification visible, which tells Android not to kill it.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationId = Notifications.progressId(id.toString())
    private val resultNotificationId = Notifications.resultId(id.toString())

    // The notification's Cancel button: asks WorkManager to stop this worker.
    private val cancelIntent get() = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "video"
        val format = inputData.getString(KEY_FORMAT)
        val isAudio = inputData.getBoolean(KEY_AUDIO, false)
        val clipStart = inputData.getInt(KEY_CLIP_START, -1)
        val clipEnd = inputData.getInt(KEY_CLIP_END, -1)
        val extras = DownloadExtras(
            subtitles = inputData.getBoolean(KEY_SUBTITLES, false),
            clip = if (clipStart >= 0 && clipEnd > clipStart) ClipRange(clipStart, clipEnd) else null,
        )
        val recordId = id.toString()

        // Each download gets its own cache folder, so finding the result and cleaning up is easy.
        val workDir = File(applicationContext.cacheDir, "downloads/$id")
        try {
            setForeground(foregroundInfo(title, -1))
        } catch (e: IllegalStateException) {
            // Android 12+ can refuse to start foreground work once our screen has closed. The download
            // still runs (progress is shown as a normal notification), it's just easier for Android to stop.
            Log.w(TAG, "Couldn't run as a foreground worker", e)
        }

        DownloadStore.update(recordId) { it.copy(status = DownloadStatus.RUNNING) }
        return try {
            var lastShown = -1
            val cancel = cancelIntent
            // runInterruptible: if the worker is cancelled, the blocking yt-dlp call gets interrupted,
            // which makes youtubedl-android kill the yt-dlp process.
            val file = runInterruptible(Dispatchers.IO) {
                Downloader.download(url, format, isAudio, extras, workDir) { progress ->
                    val percent = progress.toInt()
                    if (percent != lastShown) { // yt-dlp reports many times a second; update only on change
                        lastShown = percent
                        DownloadStore.setProgress(recordId, percent)
                        Notifications.show(
                            applicationContext, notificationId,
                            Notifications.progress(applicationContext, title, percent, cancel)
                        )
                    }
                }
            }
            val size = file.length()
            val mime = MediaStoreSaver.mimeTypeOf(file)
            val uri = MediaStoreSaver.save(applicationContext, file)
            DownloadStore.update(recordId) {
                it.copy(status = DownloadStatus.DONE, uri = uri?.toString(), mime = mime, sizeBytes = size)
            }
            Notifications.complete(applicationContext, resultNotificationId, title, uri, mime)
            Result.success()
        } catch (e: CancellationException) {
            DownloadStore.update(recordId) { it.copy(status = DownloadStatus.CANCELLED) }
            throw e // cancelled by the user: no "failed" notification, cleanup still runs below
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $url", e)
            val message = ErrorMapper.toMessage(e.message)
            DownloadStore.update(recordId) { it.copy(status = DownloadStatus.FAILED, error = message) }
            Notifications.failed(applicationContext, resultNotificationId, title, message, recordId)
            // "Success" for WorkManager even though the download failed: playlist videos run as a
            // chain, and a failed link would stop every video after it. The failure is already
            // shown in the Downloads tab and the notification.
            Result.success()
        } finally {
            // Removes partial files too. Also clear the progress notification in case a last
            // progress update raced with the end of the download.
            workDir.deleteRecursively()
            Notifications.cancel(applicationContext, notificationId)
            DownloadStore.clearProgress(recordId)
        }
    }

    private fun foregroundInfo(title: String, percent: Int): ForegroundInfo {
        val notification = Notifications.progress(applicationContext, title, percent, cancelIntent)
        // Android 10+ wants to know what kind of foreground work this is: a data transfer.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        private const val KEY_URL = "url"
        private const val KEY_TITLE = "title"
        private const val KEY_FORMAT = "format"
        private const val KEY_AUDIO = "audio"
        private const val KEY_SUBTITLES = "subtitles"
        private const val KEY_CLIP_START = "clipStart"
        private const val KEY_CLIP_END = "clipEnd"

        /**
         * Queues a download and adds it to the Downloads tab; it starts right away and survives
         * leaving the app.
         */
        fun enqueue(
            context: Context,
            url: String,
            title: String,
            thumbnailUrl: String?,
            option: QualityOption,
            extras: DownloadExtras,
        ) {
            val (request, record) = newDownload(url, title, thumbnailUrl, option, extras, group = null)
            DownloadStore.add(record)
            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Queues every video of [playlist] with the same quality, as a *chain*: WorkManager starts
         * each one after the one before has finished, so the phone downloads one video at a time.
         * They share a group id, so cancelling one marks the rest of the chain cancelled too.
         */
        fun enqueuePlaylist(context: Context, playlist: Playlist, option: QualityOption, extras: DownloadExtras) =
            enqueueInOrder(context, playlist.entries.map { DownloadRequest(it.url, it.title, it.thumbnailUrl, option, extras) })

        /**
         * Queues [requests] as a chain, one download at a time in this order (playlists, Retry all).
         * They share a group id, so cancelling one marks the rest of the chain cancelled too.
         */
        fun enqueueInOrder(context: Context, requests: List<DownloadRequest>) {
            if (requests.isEmpty()) return
            val group = UUID.randomUUID().toString()
            val downloads = requests.map { newDownload(it.url, it.title, it.thumbnailUrl, it.option, it.extras, group) }
            // addAll puts them on top in this order, so the first one to download is the top card.
            DownloadStore.addAll(downloads.map { it.second })
            val work = downloads.map { it.first }
            work.drop(1)
                .fold(WorkManager.getInstance(context).beginWith(work.first())) { chain, next -> chain.then(next) }
                .enqueue()
        }

        /** The WorkManager job and its Downloads-tab row, not yet queued or saved. */
        private fun newDownload(
            url: String,
            title: String,
            thumbnailUrl: String?,
            option: QualityOption,
            extras: DownloadExtras,
            group: String?,
        ): Pair<OneTimeWorkRequest, DownloadRecord> {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_URL to url,
                        KEY_TITLE to title,
                        KEY_FORMAT to option.format,
                        KEY_AUDIO to option.isAudio,
                        KEY_SUBTITLES to extras.subtitles,
                        // -1 = no clip (WorkManager input can't hold nulls for numbers).
                        KEY_CLIP_START to (extras.clip?.startSeconds ?: -1),
                        KEY_CLIP_END to (extras.clip?.endSeconds ?: -1),
                    )
                )
                .build()
            return request to DownloadRecord(
                id = request.id.toString(),
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                label = option.label,
                format = option.format,
                isAudio = option.isAudio,
                subtitles = extras.subtitles,
                clipStart = extras.clip?.startSeconds,
                clipEnd = extras.clip?.endSeconds,
                group = group,
                status = DownloadStatus.QUEUED,
                createdAt = System.currentTimeMillis(),
            )
        }
    }
}
