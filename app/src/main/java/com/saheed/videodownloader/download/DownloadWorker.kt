package com.saheed.videodownloader.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.saheed.videodownloader.formats.QualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Runs one download in the background with *WorkManager* (Android's job scheduler for work that
 * must finish even if the user leaves the app). It runs as a *foreground worker*: it keeps a
 * notification visible, which tells Android not to kill it.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationId = id.hashCode()
    private val resultNotificationId = notificationId + 1

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "video"
        val format = inputData.getString(KEY_FORMAT)
        val isAudio = inputData.getBoolean(KEY_AUDIO, false)

        // Each download gets its own cache folder, so finding the result and cleaning up is easy.
        val workDir = File(applicationContext.cacheDir, "downloads/$id")
        setForeground(foregroundInfo(title, -1))

        return try {
            var lastShown = -1
            val file = withContext(Dispatchers.IO) {
                Downloader.download(url, format, isAudio, workDir) { progress ->
                    val percent = progress.toInt()
                    if (percent != lastShown) { // yt-dlp reports many times a second; update only on change
                        lastShown = percent
                        Notifications.show(applicationContext, notificationId, Notifications.progress(applicationContext, title, percent))
                    }
                }
            }
            val uri = MediaStoreSaver.save(applicationContext, file)
            Notifications.complete(applicationContext, resultNotificationId, title, uri, MediaStoreSaver.mimeTypeOf(file))
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $url", e)
            Notifications.failed(applicationContext, resultNotificationId, title, e.message ?: "Unknown error")
            Result.failure()
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun foregroundInfo(title: String, percent: Int): ForegroundInfo {
        val notification = Notifications.progress(applicationContext, title, percent)
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

        /** Queues a download; it starts right away and survives leaving the app. */
        fun enqueue(context: Context, url: String, title: String, option: QualityOption) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_URL to url,
                        KEY_TITLE to title,
                        KEY_FORMAT to option.format,
                        KEY_AUDIO to option.isAudio,
                    )
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
