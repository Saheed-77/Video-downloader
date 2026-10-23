package com.saheed.videodownloader.formats

import java.util.Locale

/**
 * One row in the quality list.
 * `data class` = Kotlin generates equals/hashCode/toString/copy, like a Java record.
 *
 * @param format yt-dlp's `-f` value; null for MP3 (audio is extracted with `-x` instead).
 * @param sizeBytes approximate file size, or null when the site doesn't say.
 */
data class QualityOption(
    val label: String,
    val format: String?,
    val isAudio: Boolean,
    val sizeBytes: Long? = null,
) {
    /** "720p · 45 MB", or just "720p" when the size is unknown. */
    val displayText: String
        get() = sizeBytes?.let { "$label · ${formatSize(it)}" } ?: label

    companion object {
        val BEST = QualityOption("Best quality", "bestvideo+bestaudio/best", isAudio = false)
        val MP3 = QualityOption("MP3 audio", null, isAudio = true)

        fun forHeight(label: String, height: Int, sizeBytes: Long?) = QualityOption(
            label = label,
            format = "bestvideo[height<=$height]+bestaudio/best[height<=$height]",
            isAudio = false,
            sizeBytes = sizeBytes,
        )

        fun formatSize(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
            bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
            else -> "<1 MB"
        }
    }
}
