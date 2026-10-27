package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.DownloadExtras
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
        extras: DownloadExtras,
        outDir: File,
        onProgress: (Float) -> Unit,
    ): File {
        outDir.mkdirs()
        val request = YoutubeDLRequest(url)
        options(format, isAudio, extras).forEach { (name, value) ->
            if (value == null) request.addOption(name) else request.addOption(name, value)
        }
        request.addOption("-o", File(outDir, "%(title).100B.%(ext)s").absolutePath)
        EngineUpdater.useEngine {
            YoutubeDL.getInstance().execute(request) { progress, _, _ -> onProgress(progress) }
        }
        // yt-dlp picks the final file name itself (subtitles end up inside the MP4, so the
        // newest file is still the video).
        return outDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.lastModified() }
            ?: error("Download finished but no file was found")
    }

    /** yt-dlp options as (name, value) pairs; value is null for plain flags. Kept separate so tests can check it. */
    fun options(format: String?, isAudio: Boolean, extras: DownloadExtras): List<Pair<String, String?>> {
        val options = mutableListOf<Pair<String, String?>>()
        if (isAudio) {
            options += "-f" to "bestaudio/best"
            options += "-x" to null
            options += "--audio-format" to "mp3"
            options += "--audio-quality" to "0"
        } else {
            options += "-f" to (format ?: "bestvideo+bestaudio/best")
            options += "--merge-output-format" to "mp4"
            if (extras.subtitles) {
                // English only (en, en-US, en-GB…): one or two requests, so YouTube is unlikely
                // to answer "too many requests", which would fail the whole download.
                // Only real subtitles, not YouTube's automatic captions.
                options += "--write-subs" to null
                options += "--sub-langs" to "en.*"
                options += "--embed-subs" to null
            }
        }
        options += "--no-playlist" to null
        options += "--no-mtime" to null
        return options
    }
}
