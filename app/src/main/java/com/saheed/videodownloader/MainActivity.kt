package com.saheed.videodownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.saheed.videodownloader.ui.AppTheme
import com.saheed.videodownloader.ui.MainScreen

/** The screen you get when tapping the app icon. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent { } hands the screen to Jetpack Compose: the UI is described by
        // @Composable functions instead of XML layouts.
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}
