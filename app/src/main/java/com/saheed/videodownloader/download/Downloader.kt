package com.saheed.videodownloader.download

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Thin wrapper around yt-dlp: builds the command line and runs it. */
object Downloader {

    /** Best MP4 (video + audio merged by ffmpeg). */
    const val BEST_MP4 = "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best"

    /**
     * Downloads [url] into [outDir] and returns the finished file.
     * Blocking: call it from a background thread. [onProgress] gets 0–100.
     */
    fun download(url: String, outDir: File, onProgress: (Float) -> Unit): File {
        outDir.mkdirs()
        val request = YoutubeDLRequest(url).apply {
            addOption("-f", BEST_MP4)
            addOption("--merge-output-format", "mp4")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("-o", File(outDir, "%(title).100B.%(ext)s").absolutePath)
        }
        YoutubeDL.getInstance().execute(request) { progress, _, _ -> onProgress(progress) }
        // yt-dlp picks the final file name itself, so take the newest file it left behind.
        return outDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
            ?: error("Download finished but no file was found")
    }
}
