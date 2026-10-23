package com.saheed.videodownloader

import android.app.Application
import android.util.Log
import com.saheed.videodownloader.download.Notifications
import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlin.concurrent.thread

/**
 * The Application class runs once when the app process starts, before any screen.
 * It's the place for one-time setup, here unpacking yt-dlp, Python and ffmpeg.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "Failed to initialise the downloader engine", e)
            initError = e.message ?: "Failed to start the downloader engine"
            return
        }
        // Weekly yt-dlp update, checked every time the app starts (opened or shared to).
        thread(name = "engine-update") { EngineUpdater.updateIfDue(this) }
    }

    // `companion object` is Kotlin's version of Java static members.
    companion object {
        private const val TAG = "App"

        /** Null when the engine started fine, otherwise a message the screens can show. */
        var initError: String? = null
            private set
    }
}
