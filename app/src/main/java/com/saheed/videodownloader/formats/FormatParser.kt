package com.saheed.videodownloader.formats

import com.yausername.youtubedl_android.mapper.VideoInfo

/** The few fields of a yt-dlp format we need. Our own type, so tests don't need yt-dlp. */
data class FormatInfo(
    val width: Int,
    val height: Int,
    val vcodec: String?,
    val acodec: String?,
    /** filesize, or filesize_approx, or 0 when unknown. */
    val sizeBytes: Long,
)

/** Turns yt-dlp's format list into the rows of the quality menu. */
object FormatParser {

    fun fromVideoInfo(info: VideoInfo, prefer: VideoFormat = VideoFormat.COMPATIBLE): List<QualityOption> = parse(
        info.formats.orEmpty().map {
            FormatInfo(
                width = it.width,
                height = it.height,
                vcodec = it.vcodec,
                acodec = it.acodec,
                sizeBytes = if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate,
            )
        },
        prefer,
    )

    /**
     * Returns: "Best quality", then one row per resolution the video really has (highest first),
     * then "MP3 audio". Sizes are estimates: the file at that resolution in the codec [prefer] asks
     * for (the biggest one if that codec isn't there), plus the best audio track when the video file
     * has no sound of its own (YouTube keeps them separate).
     */
    fun parse(formats: List<FormatInfo>, prefer: VideoFormat = VideoFormat.COMPATIBLE): List<QualityOption> {
        val audioOnly = formats.filter { it.vcodec == "none" && it.acodec != null && it.acodec != "none" }
        val bestAudioSize = audioOnly.maxOfOrNull { it.sizeBytes } ?: 0L

        // Video formats with a known height. vcodec can be null on some sites; only "none" means no video.
        val video = formats.filter { it.vcodec != "none" && it.height > 0 }

        // Name each resolution like YouTube does: by the shorter side, so a vertical 1080x1920
        // Short is "1080p", not "1920p". The yt-dlp filter still uses the real height.
        val rows = video
            .groupBy { pLabel(it) }
            .map { (p, group) ->
                // The format yt-dlp will pick: preferred codec first, then the biggest of those.
                val chosen = group.minWith(compareBy<FormatInfo> { prefer.codecRank(it.vcodec) }.thenByDescending { it.sizeBytes })
                val size = when {
                    chosen.sizeBytes <= 0 -> null
                    chosen.acodec == "none" -> chosen.sizeBytes + bestAudioSize
                    else -> chosen.sizeBytes
                }
                p to QualityOption.forHeight("${p}p", group.maxOf { it.height }, size, resolution = p)
            }
            .sortedByDescending { it.first }
            .map { it.second }

        val best = QualityOption.BEST.copy(sizeBytes = rows.firstOrNull()?.sizeBytes)
        val mp3 = QualityOption.MP3.copy(sizeBytes = bestAudioSize.takeIf { it > 0 })
        return listOf(best) + rows + mp3
    }

    private fun pLabel(f: FormatInfo): Int = if (f.width > 0) minOf(f.width, f.height) else f.height
}
