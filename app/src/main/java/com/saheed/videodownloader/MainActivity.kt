package com.saheed.videodownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.saheed.videodownloader.ui.AppTheme
import com.saheed.videodownloader.ui.MainScreen
import com.saheed.videodownloader.ui.requestDownloadPermissions
import com.saheed.videodownloader.update.EngineUpdater
import kotlin.concurrent.thread

/** The screen you get when tapping the app icon. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestDownloadPermissions()
        // Weekly yt-dlp update check, every time the main screen opens.
        if (App.initError == null) thread(name = "engine-update") { EngineUpdater.updateIfDue(applicationContext) }
        // setContent { } hands the screen to Jetpack Compose: the UI is described by
        // @Composable functions instead of XML layouts.
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
