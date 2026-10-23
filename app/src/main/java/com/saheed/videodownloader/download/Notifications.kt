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

/** Builds and shows the download notifications. */
object Notifications {

    private const val CHANNEL_PROGRESS = "progress"
    private const val CHANNEL_RESULT = "result"

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

    /** "Downloading <title> — 42%". [percent] < 0 shows an indeterminate bar (e.g. while starting). */
    fun progress(context: Context, title: String, percent: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(if (percent < 0) "Starting…" else "Downloading — $percent%")
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    /** "Download complete" — tapping opens the file (or the Downloads app on Android 7–9). */
    fun complete(context: Context, id: Int, title: String, uri: Uri?, mimeType: String) {
        val open = if (uri != null) {
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // A PendingIntent is a "tap action" handed to the system to run on our behalf later.
        val tap = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE)
        show(
            context, id,
            NotificationCompat.Builder(context, CHANNEL_RESULT)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download complete")
                .setContentText(title)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
        )
    }

    fun failed(context: Context, id: Int, title: String, message: String) {
        show(
            context, id,
            NotificationCompat.Builder(context, CHANNEL_RESULT)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Download failed")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$message"))
                .setAutoCancel(true)
                .build()
        )
    }

    fun show(context: Context, id: Int, notification: Notification) {
        // Android 13+ needs the user's OK to post notifications; skip quietly if they said no.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
