package com.saheed.videodownloader.formats

import java.util.Locale

/**
 * One row in the quality list.
 * `data class` = Kotlin generates equals/hashCode/toString/copy, like a Java record.
 *
 * @param format yt-dlp's `-f` value; null for MP3 (audio is extracted with `-x` instead).
 * @param sizeBytes approximate file size, or null when the site doesn't say.
 * @param resolution the "p" number shown in the label (720 for "720p"); null for Best and MP3.
 */
data class QualityOption(
    val label: String,
    val format: String?,
    val isAudio: Boolean,
    val sizeBytes: Long? = null,
    val resolution: Int? = null,
) {
    /** "720p · 45 MB", or just "720p" when the size is unknown. */
    val displayText: String
        get() = sizeBytes?.let { "$label · ${formatSize(it)}" } ?: label

    /** Short tag in front of the row: "Best", "4K", "2K", "HD" or "SD"; null for MP3 (shown as an icon). */
    val badge: String?
        get() = when {
            isAudio -> null
            resolution == null -> "Best"
            resolution >= 2160 -> "4K"
            resolution >= 1440 -> "2K"
            resolution >= 720 -> "HD"
            else -> "SD"
        }

    companion object {
        val BEST = QualityOption("Best quality", "bestvideo+bestaudio/best", isAudio = false)
        val MP3 = QualityOption("MP3 audio", null, isAudio = true)

        /** [resolution] defaults to [height]; it differs for vertical videos (1080x1920 is "1080p"). */
        fun forHeight(label: String, height: Int, sizeBytes: Long?, resolution: Int = height) = QualityOption(
            label = label,
            format = "bestvideo[height<=$height]+bestaudio/best[height<=$height]",
            isAudio = false,
            sizeBytes = sizeBytes,
            resolution = resolution,
        )

        fun formatSize(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1e9)
            bytes >= 1_000_000 -> "${bytes / 1_000_000} MB"
            else -> "<1 MB"
        }
    }
}
