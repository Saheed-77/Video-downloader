package com.saheed.videodownloader.download

import android.util.Log
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/** Thin wrapper around yt-dlp: builds the command line and runs it. */
object Downloader {

    private const val TAG = "Downloader"

    /**
     * Downloads [url] into [outDir] and returns the finished file.
     * Video: [format] is yt-dlp's `-f` value, merged into an MP4 by ffmpeg.
     * Audio ([isAudio]): the best audio track, converted to MP3 by ffmpeg.
     * [pause]: wait a little first and between requests (playlists, Retry all), see [options].
     * Blocking: call it from a background thread. [onProgress] gets 0–100.
     */
    fun download(
        url: String,
        format: String?,
        isAudio: Boolean,
        extras: DownloadExtras,
        pause: Boolean,
        outDir: File,
        onProgress: (Float) -> Unit,
    ): File {
        outDir.mkdirs()
        val request = YoutubeDLRequest(url)
        options(format, isAudio, extras, pause).forEach { (name, value) ->
            if (value == null) request.addOption(name) else request.addOption(name, value)
        }
        request.addOption("-o", File(outDir, "%(title).100B.%(ext)s").absolutePath)
        val response = EngineUpdater.useEngine {
            YoutubeDL.getInstance().execute(request) { progress, _, _ -> onProgress(progress) }
        }
        // The last lines of yt-dlp's output show which steps ran (merging, embedding subtitles).
        Log.d(TAG, "yt-dlp output (end):\n" + response.out.takeLast(2000))
        return mainFile(outDir) ?: error("Download finished but no file was found")
    }

    /**
     * yt-dlp picks the final file name itself, so find it: the biggest file in [outDir] is the
     * video or MP3. Not "the newest": after embedding subtitles, yt-dlp gives the MP4 the time of
     * its oldest input, so a leftover subtitle file (.vtt) can look newer.
     */
    fun mainFile(outDir: File): File? = outDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.length() }

    /** yt-dlp options as (name, value) pairs; value is null for plain flags. Kept separate so tests can check it. */
    fun options(
        format: String?,
        isAudio: Boolean,
        extras: DownloadExtras,
        pause: Boolean = false,
    ): List<Pair<String, String?>> {
        val options = mutableListOf<Pair<String, String?>>()
        if (isAudio) {
            options += "-f" to "bestaudio/best"
            options += "-x" to null
            options += "--audio-format" to "mp3"
            options += "--audio-quality" to "0"
            // Cover art (the video's thumbnail) and title/artist inside the MP3, so music players show them.
            options += "--embed-thumbnail" to null
            options += "--embed-metadata" to null
        } else {
            options += "-f" to (format ?: "bestvideo+bestaudio/best")
            // *Format sort*: at the chosen resolution, prefer H.264 video and AAC audio, which play on
            // every phone, TV and WhatsApp (YouTube often serves AV1/VP9 otherwise). Resolution comes
            // first, so a 4K pick still gets 4K even when only VP9/AV1 exists there.
            options += "-S" to "res,vcodec:h264,acodec:m4a"
            options += "--merge-output-format" to "mp4"
            // Not for clips: yt-dlp doesn't cut subtitles, so they'd start from 0:00 of the full video.
            if (extras.subtitles && extras.clip == null) {
                // English only (en, en-US, en-GB…): one or two requests, so YouTube is unlikely
                // to answer "too many requests", which would fail the whole download.
                // Only real subtitles, not YouTube's automatic captions.
                options += "--write-subs" to null
                options += "--sub-langs" to "en.*"
                options += "--embed-subs" to null
            }
        }
        extras.clip?.let {
            options += "--download-sections" to it.toSection()
            // Cut exactly at the chosen times. ffmpeg re-encodes the clip for that, so it's slower;
            // without it the clip would start at the nearest keyframe, a few seconds off.
            options += "--force-keyframes-at-cuts" to null
        }
        if (pause) {
            // Many downloads in a row from one phone make YouTube answer "429 Too Many Requests /
            // confirm you're not a bot". A random 5–10 s wait before each video, plus 1 s between
            // yt-dlp's page requests, looks more like a person and makes that much less likely.
            options += "--sleep-interval" to "5"
            options += "--max-sleep-interval" to "10"
            options += "--sleep-requests" to "1"
        }
        options += "--no-playlist" to null
        options += "--no-mtime" to null
        return options
    }
}
