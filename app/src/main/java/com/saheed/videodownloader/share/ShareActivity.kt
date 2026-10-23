package com.saheed.videodownloader.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.download.DownloadWorker
import com.saheed.videodownloader.ui.AppTheme
import com.saheed.videodownloader.ui.requestDownloadPermissions

/**
 * Opened from another app's Share menu. The manifest's *intent filter* (ACTION_SEND + text/plain)
 * is what makes Android list this app there. The window is transparent, so the bottom sheet
 * slides up over YouTube/Instagram instead of opening a full screen.
 */
class ShareActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestDownloadPermissions()
        val url = UrlExtractor.extract(intent.getStringExtra(Intent.EXTRA_TEXT))

        setContent {
            AppTheme {
                // Swiping the sheet away closes the activity and returns to the other app.
                ModalBottomSheet(onDismissRequest = { finish() }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 32.dp)
                    ) {
                        if (url == null) {
                            Text("No link found", style = MaterialTheme.typography.titleMedium)
                        } else {
                            FormatPicker(url) { title, option ->
                                // Hand the download to the background worker and get out of the way.
                                DownloadWorker.enqueue(this@ShareActivity, url, title, option)
                                Toast.makeText(this@ShareActivity, "Downloading ${option.label}…", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }
}
