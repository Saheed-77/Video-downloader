package com.saheed.videodownloader.formats

import com.yausername.youtubedl_android.mapper.VideoInfo

/** What the picker shows above the quality list. */
data class VideoMeta(
    val title: String,
    val uploader: String?,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
) {
    /** "4:05", "1:02:03", or null when the site doesn't give a length. */
    val durationText: String?
        get() = formatDuration(durationSeconds)

    companion object {
        fun fromVideoInfo(info: VideoInfo) = VideoMeta(
            title = info.title ?: "video",
            uploader = info.uploader?.takeIf { it.isNotBlank() },
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnail,
        )

        fun formatDuration(seconds: Int): String? =
            if (seconds <= 0) null else ClipRange.formatTime(seconds)
    }
}
