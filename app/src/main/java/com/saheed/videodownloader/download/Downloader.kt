package com.saheed.videodownloader.download

import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Thin wrapper around yt-dlp: builds the command line and runs it. */
object Downloader {

    /**
     * Downloads [url] into [outDir] and returns the finished file.
     * Video: [format] is yt-dlp's `-f` value, merged into an MP4 by ffmpeg.
     * Audio ([isAudio]): the best audio track, converted to MP3 by ffmpeg.
     * Blocking: call it from a background thread. [onProgress] gets 0–100.
     */
    fun download(
        url: String,
        format: String?,
        isAudio: Boolean,
        outDir: File,
        onProgress: (Float) -> Unit,
    ): File {
        outDir.mkdirs()
        val request = YoutubeDLRequest(url).apply {
            if (isAudio) {
                addOption("-f", "bestaudio/best")
                addOption("-x")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", "0")
            } else {
                addOption("-f", format ?: "bestvideo+bestaudio/best")
                addOption("--merge-output-format", "mp4")
            }
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("-o", File(outDir, "%(title).100B.%(ext)s").absolutePath)
        }
        EngineUpdater.useEngine {
            YoutubeDL.getInstance().execute(request) { progress, _, _ -> onProgress(progress) }
        }
        // yt-dlp picks the final file name itself, so take the newest file it left behind.
        return outDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
            ?: error("Download finished but no file was found")
    }
}
