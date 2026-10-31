package com.saheed.videodownloader.download

import android.Manifest
import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.saheed.videodownloader.R
import java.util.UUID

/** Builds and shows the download notifications. */
object Notifications {

    private const val CHANNEL_PROGRESS = "progress"
    private const val CHANNEL_RESULT = "result"

    /** Notification ids, worked out from the download's id so any part of the app can find them. */
    fun progressId(recordId: String) = UUID.fromString(recordId).hashCode()
    fun resultId(recordId: String) = progressId(recordId) + 1

    /**
     * Android 8+ groups notifications into *channels* the user can mute separately in Settings.
     * Progress is silent (low importance); "complete"/"failed" use the normal sound.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Download progress", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RESULT, "Finished downloads", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /**
     * "Downloading <title> — 42%" with a Cancel button that runs [cancel].
     * [percent] < 0 shows an indeterminate bar (e.g. while starting).
     */
    fun progress(context: Context, title: String, progress: Progress?, cancel: PendingIntent): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            // "Downloading — 46% · 3.4 MB/s · 0:12 left"; a moving bar until the first percent arrives.
            .setContentText(progress?.let { "Downloading — ${it.text}" } ?: "Starting…")
            .setProgress(100, progress?.percent ?: 0, progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel)
            .build()

    /**
     * "Download complete" — tapping opens the file (or the Downloads app on Android 7–9).
     * Has a Share button when there's a file address to share.
     */
    fun complete(context: Context, id: Int, title: String, uri: Uri?, mimeType: String) {
        val open = if (uri != null) {
            FileActions.openIntent(uri, mimeType)
        } else {
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // A PendingIntent is a "tap action" handed to the system to run on our behalf later.
        val tap = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(context, CHANNEL_RESULT)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(title)
            .setContentIntent(tap)
            .setAutoCancel(true)
        if (uri != null) {
            val share = PendingIntent.getActivity(
                context, id, FileActions.shareIntent(uri, mimeType),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_share, "Share", share)
        }
        show(context, id, builder.build())
    }

    /** "Download failed" with the friendly reason and a Retry button for download [recordId]. */
    fun failed(context: Context, id: Int, title: String, message: String, recordId: String) {
        val retry = PendingIntent.getBroadcast(
            context, id,
            Intent(context, RetryReceiver::class.java).putExtra(RetryReceiver.EXTRA_ID, recordId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        show(
            context, id,
            NotificationCompat.Builder(context, CHANNEL_RESULT)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download failed")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$message"))
                .setAutoCancel(true)
                .addAction(R.drawable.ic_refresh, "Retry", retry)
                .build()
        )
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    fun show(context: Context, id: Int, notification: Notification) {
        // Android 13+ needs the user's OK to post notifications; skip quietly if they said no.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
