package com.saheed.videodownloader.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Runs when "Retry" is tapped on a "Download failed" notification. A *BroadcastReceiver* is a
 * small piece of code Android can run without opening any screen; the notification sends it an
 * Intent holding the failed download's id.
 */
class RetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        DownloadStore.retry(context, id)
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
