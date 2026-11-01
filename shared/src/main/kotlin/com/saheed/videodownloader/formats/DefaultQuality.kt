package com.saheed.videodownloader.formats

/**
 * Settings → "When sharing a link": ask every time, or start this quality straight away.
 * [height] is the "p" number for the resolution choices.
 */
enum class DefaultQuality(val label: String, val height: Int? = null) {
    ASK("Always ask"),
    BEST("Best quality"),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    MP3("MP3 audio");

    /**
     * The row to start from [options] (the picker's list for one video or a playlist), or null for
     * "Always ask". If the video doesn't have this resolution, the highest one below it is used,
     * or the lowest one when none is below.
     */
    fun pick(options: List<QualityOption>): QualityOption? {
        val best = options.firstOrNull { !it.isAudio && it.resolution == null }
        val sized = options.filter { !it.isAudio && it.resolution != null }
        return when (this) {
            ASK -> null
            BEST -> best
            MP3 -> options.firstOrNull { it.isAudio }
            // Some sites don't report resolutions: then only "Best quality" exists.
            else -> sized.filter { it.resolution!! <= height!! }.maxByOrNull { it.resolution!! }
                ?: sized.minByOrNull { it.resolution!! }
                ?: best
        }
    }
}
