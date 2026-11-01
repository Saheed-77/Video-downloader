package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.VideoFormat
import java.io.File

/**
 * The yt-dlp command-line options for a download, used by both the Android app (through its
 * library) and the desktop app (running yt-dlp.exe). The output file name (`-o`) is added by each.
 */
object YtDlpOptions {

    /**
     * yt-dlp picks the final file name itself, so find it: the biggest file in [outDir] is the
     * video or MP3. Not "the newest": after embedding subtitles, yt-dlp gives the MP4 the time of
     * its oldest input, so a leftover subtitle file (.vtt) can look newer.
     */
    fun mainFile(outDir: File): File? = outDir.listFiles()?.filter { it.isFile }?.maxByOrNull { it.length() }

    /** yt-dlp options as (name, value) pairs; value is null for plain flags. */
    fun build(
        format: String?,
        isAudio: Boolean,
        extras: DownloadExtras,
        pause: Boolean = false,
        cookies: File? = null,
        videoFormat: VideoFormat = VideoFormat.COMPATIBLE,
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
            // *Format sort*: at the chosen resolution, prefer the codec from Settings → Video format
            // (H.264 for "Most compatible", AV1/VP9 for "Smallest files"). Resolution comes first, so a
            // 4K pick still gets 4K even when only VP9/AV1 exists there.
            options += "-S" to videoFormat.sort
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
            // Many downloads in a row from one device make YouTube answer "429 Too Many Requests /
            // confirm you're not a bot". A random 5–10 s wait before each video, plus 1 s between
            // yt-dlp's page requests, looks more like a person and makes that much less likely.
            options += "--sleep-interval" to "5"
            options += "--max-sleep-interval" to "10"
            options += "--sleep-requests" to "1"
        }
        // Signed in to Instagram/Facebook (Settings → Accounts): send those cookies along.
        cookies?.let { options += "--cookies" to it.absolutePath }
        options += "--no-playlist" to null
        options += "--no-mtime" to null
        return options
    }
}
