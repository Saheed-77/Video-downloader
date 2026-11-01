package com.saheed.videodownloader.download

import android.util.Log
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.VideoFormat
import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Runs yt-dlp through the Android library, with the options [YtDlpOptions] builds (shared with the desktop app). */
object Downloader {

    private const val TAG = "Downloader"

    /**
     * Downloads [url] into [outDir] and returns the finished file.
     * Video: [format] is yt-dlp's `-f` value, merged into an MP4 by ffmpeg.
     * Audio ([isAudio]): the best audio track, converted to MP3 by ffmpeg.
     * [pause]: wait a little first and between requests (playlists), see [YtDlpOptions.build].
     * [cookies]: the sign-in cookies file for this site, if signed in (see [Cookies]).
     * [videoFormat]: Smallest files or Most compatible (already resolved from "Auto").
     * Blocking: call it from a background thread. [onProgress] gets the percent, speed and time left.
     */
    fun download(
        url: String,
        format: String?,
        isAudio: Boolean,
        extras: DownloadExtras,
        pause: Boolean,
        cookies: File?,
        videoFormat: VideoFormat,
        outDir: File,
        onProgress: (Progress) -> Unit,
    ): File {
        outDir.mkdirs()
        val request = YoutubeDLRequest(url)
        YtDlpOptions.build(format, isAudio, extras, pause, cookies, videoFormat).forEach { (name, value) ->
            if (value == null) request.addOption(name) else request.addOption(name, value)
        }
        request.addOption("-o", File(outDir, "%(title).100B.%(ext)s").absolutePath)
        val response = EngineUpdater.useEngine {
            YoutubeDL.getInstance().execute(request) { progress, etaSeconds, line ->
                onProgress(Progress(progress.toInt(), Progress.parseSpeed(line), etaSeconds.takeIf { it > 0 }))
            }
        }
        // The last lines of yt-dlp's output show which steps ran (merging, embedding subtitles).
        Log.d(TAG, "yt-dlp output (end):\n" + response.out.takeLast(2000))
        return YtDlpOptions.mainFile(outDir) ?: error("Download finished but no file was found")
    }
}
