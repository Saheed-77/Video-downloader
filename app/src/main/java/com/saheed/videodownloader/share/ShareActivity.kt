package com.saheed.videodownloader.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.saheed.videodownloader.AppSettings
import com.saheed.videodownloader.download.DownloadWorker
import com.saheed.videodownloader.formats.DefaultQuality
import com.saheed.videodownloader.ui.AppTheme
import com.saheed.videodownloader.ui.requestDownloadPermissions

/**
 * Opened from another app's Share menu. The manifest's *intent filter* (ACTION_SEND + text/plain)
 * is what makes Android list this app there. The window is transparent, so the bottom sheet
 * slides up over YouTube/Instagram instead of opening a full screen.
 */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestDownloadPermissions()
        val url = UrlExtractor.extract(intent.getStringExtra(Intent.EXTRA_TEXT))
        // Settings → "When sharing a link": with a default, the sheet picks it by itself.
        val autoPick = AppSettings.defaultQuality(this).takeIf { it != DefaultQuality.ASK }

        setContent {
            AppTheme {
                // Swiping the sheet away closes the activity and returns to the other app.
                PickerSheet(
                    url,
                    onDismiss = { finish() },
                    autoPick = autoPick,
                    onPickPlaylist = { playlist, option, extras ->
                        DownloadWorker.enqueuePlaylist(this, playlist, option, extras)
                        Toast.makeText(this, "Downloading ${playlist.entries.size} videos…", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                ) { pickedUrl, meta, option, extras ->
                    // Hand the download to the background worker and get out of the way.
                    DownloadWorker.enqueue(this, pickedUrl, meta.title, meta.thumbnailUrl, option, extras)
                    Toast.makeText(this, "Downloading ${option.label}…", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}
