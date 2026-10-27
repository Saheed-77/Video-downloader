package com.saheed.videodownloader.formats

/** Choices made in the picker on top of the quality row. */
data class DownloadExtras(
    /** Embed English subtitles into the MP4, when the video has them. Ignored for MP3. */
    val subtitles: Boolean = false,
    /** Download only this part of the video (or audio); null for all of it. */
    val clip: ClipRange? = null,
)
