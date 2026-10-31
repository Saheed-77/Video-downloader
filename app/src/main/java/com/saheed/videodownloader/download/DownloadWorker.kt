package com.saheed.videodownloader.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.saheed.videodownloader.AppSettings
import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

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
        val group = inputData.getString(KEY_GROUP)

        // Stopped or cancelled while it was waiting to start (a playlist's Stop/Cancel): don't download.
        val status = DownloadStore.find(recordId)?.status
        if (status != null && status != DownloadStatus.QUEUED && status != DownloadStatus.RUNNING) return Result.success()

        // Each download gets its own cache folder, so finding the result and cleaning up is easy.
        val workDir = File(applicationContext.cacheDir, "downloads/$id")
        try {
            setForeground(foregroundInfo(title))
        } catch (e: IllegalStateException) {
            // Android 12+ can refuse to start foreground work once our screen has closed. The download
            // still runs (progress is shown as a normal notification), it's just easier for Android to stop.
            Log.w(TAG, "Couldn't run as a foreground worker", e)
        }

        // error = null also clears a "Waiting for internet…" note from an earlier attempt.
        DownloadStore.update(recordId) { it.copy(status = DownloadStatus.RUNNING, error = null) }
        return try {
            var lastShown = -1
            val cancel = cancelIntent
            // runInterruptible: if the worker is cancelled, the blocking yt-dlp call gets interrupted,
            // which makes youtubedl-android kill the yt-dlp process.
            val file = runInterruptible(Dispatchers.IO) {
                Downloader.download(url, format, isAudio, extras, inputData.getBoolean(KEY_PAUSE, false), Cookies.fileFor(applicationContext, url), workDir) { progress ->
                    // yt-dlp reports many times a second; update only when the percent changes.
                    if (progress.percent != lastShown) {
                        lastShown = progress.percent
                        DownloadStore.setProgress(recordId, progress)
                        Notifications.show(
                            applicationContext, notificationId,
                            Notifications.progress(applicationContext, title, progress, cancel)
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
            // Only a running row: a playlist's Stop has already marked it Stopped (PAUSED).
            DownloadStore.update(recordId) { if (it.status == DownloadStatus.RUNNING) it.copy(status = DownloadStatus.CANCELLED) else it }
            throw e // cancelled by the user: no "failed" notification, cleanup still runs below
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $url", e)
            val message = ErrorMapper.toMessage(e.message)
            if (message == ErrorMapper.NO_INTERNET && runAttemptCount < MAX_NETWORK_RETRIES) {
                // The connection dropped: wait for it instead of failing. WorkManager runs this job
                // again once the network constraint (see [start]) is met; the row shows why it waits.
                DownloadStore.update(recordId) { it.copy(error = WAITING_FOR_INTERNET) }
                return Result.retry()
            }
            DownloadStore.update(recordId) { it.copy(status = DownloadStatus.FAILED, error = message) }
            Notifications.failed(applicationContext, resultNotificationId, title, message, recordId)
            // "Success" for WorkManager: the failure is already shown in the Downloads tab and the
            // notification, and WorkManager has nothing more to do with it.
            Result.success()
        } finally {
            // Removes partial files too. Also clear the progress notification in case a last
            // progress update raced with the end of the download.
            workDir.deleteRecursively()
            Notifications.cancel(applicationContext, notificationId)
            DownloadStore.clearProgress(recordId)
            // A playlist video: start the next waiting one of that playlist.
            if (group != null) DownloadStore.pump(applicationContext, group)
        }
    }

    private fun foregroundInfo(title: String): ForegroundInfo {
        val notification = Notifications.progress(applicationContext, title, null, cancelIntent)
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
        private const val KEY_PAUSE = "pause"
        private const val KEY_GROUP = "group"
        private const val MAX_NETWORK_RETRIES = 5

        /** Shown on a card while its download waits for the connection to come back. */
        const val WAITING_FOR_INTERNET = "Waiting for internet…"

        /** How many videos of a playlist download at the same time. */
        const val AT_A_TIME = 2

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
            val record = newRecord(url, title, thumbnailUrl, option, extras, group = null, groupTitle = null)
            DownloadStore.add(record)
            start(context, record)
        }

        /**
         * Adds every video of [playlist] to the Downloads tab as waiting, in its own section, and
         * starts the first [AT_A_TIME]. Each one that ends starts the next ([DownloadStore.pump]),
         * so cancelling one video doesn't touch the others.
         */
        fun enqueuePlaylist(context: Context, playlist: Playlist, option: QualityOption, extras: DownloadExtras) {
            if (playlist.entries.isEmpty()) return
            val group = UUID.randomUUID().toString()
            // addAll puts them on top in this order, so the playlist's first video is the top card.
            DownloadStore.addAll(
                playlist.entries.map { newRecord(it.url, it.title, it.thumbnailUrl, option, extras, group, playlist.title) }
            )
            DownloadStore.pump(context, group)
        }

        /** Hands the download in [record] to WorkManager. The job's id is the row's id. */
        fun start(context: Context, record: DownloadRecord) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setId(UUID.fromString(record.id))
                .setInputData(
                    workDataOf(
                        KEY_URL to record.url,
                        KEY_TITLE to record.title,
                        KEY_FORMAT to record.format,
                        KEY_AUDIO to record.isAudio,
                        KEY_SUBTITLES to record.subtitles,
                        // -1 = no clip (WorkManager input can't hold nulls for numbers).
                        KEY_CLIP_START to (record.clipStart ?: -1),
                        KEY_CLIP_END to (record.clipEnd ?: -1),
                        KEY_GROUP to record.group,
                        // Only playlist videos pause; single ones start at once.
                        KEY_PAUSE to (record.group != null),
                    )
                )
                // A *network constraint*: the job only runs while there's a connection, or Wi-Fi
                // when "Download only on Wi-Fi" is on. Until then it simply waits.
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (AppSettings.wifiOnly(context)) NetworkType.UNMETERED else NetworkType.CONNECTED
                        )
                        .build()
                )
                // After a dropped connection: try again in 30 s, then 1 min, 2 min…
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /** A new waiting row for the Downloads tab, not yet saved or started. */
        private fun newRecord(
            url: String,
            title: String,
            thumbnailUrl: String?,
            option: QualityOption,
            extras: DownloadExtras,
            group: String?,
            groupTitle: String?,
        ) = DownloadRecord(
            id = UUID.randomUUID().toString(),
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
            groupTitle = groupTitle,
            status = DownloadStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
        )
    }
}
