package com.saheed.videodownloader.desktop

import com.saheed.videodownloader.formats.VideoFormat
import java.io.File
import java.util.Properties

/**
 * Where the desktop app keeps its files, and its settings (a small `settings.properties` file,
 * the desktop version of Android's SharedPreferences).
 */
object DesktopSettings {

    /** `%LOCALAPPDATA%\VideoDownloader`: engine, settings and the downloads list. */
    val dataDir: File = File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "VideoDownloader")
        .apply { mkdirs() }

    /** yt-dlp.exe (and ffmpeg, when this PC has none) live here. */
    val binDir: File = File(dataDir, "bin").apply { mkdirs() }

    private val file = File(dataDir, "settings.properties")
    private val props = Properties().apply { if (file.exists()) file.inputStream().use { load(it) } }

    private fun save() = file.outputStream().use { props.store(it, "Video Downloader settings") }

    /**
     * Video format. "Auto" isn't offered on the PC: Windows can't reliably say if it plays AV1
     * smoothly, so the default is Most compatible (H.264), which every player opens.
     */
    var videoFormat: VideoFormat
        get() = props.getProperty("videoFormat")?.let { name -> VideoFormat.entries.firstOrNull { it.name == name } }
            ?.takeIf { it != VideoFormat.AUTO } ?: VideoFormat.COMPATIBLE
        set(value) { props.setProperty("videoFormat", value.name); save() }

    /** The folder downloads are saved to; the user's Downloads folder by default. */
    var downloadDir: File
        get() = props.getProperty("downloadDir")?.let(::File) ?: File(System.getProperty("user.home"), "Downloads")
        set(value) { props.setProperty("downloadDir", value.absolutePath); save() }

    /** When yt-dlp was last updated (milliseconds), or null if never. */
    var lastEngineUpdate: Long?
        get() = props.getProperty("lastEngineUpdate")?.toLongOrNull()
        set(value) { props.setProperty("lastEngineUpdate", value.toString()); save() }
}
