package com.saheed.videodownloader.formats

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
        fun formatDuration(seconds: Int): String? =
            if (seconds <= 0) null else ClipRange.formatTime(seconds)
    }
}
