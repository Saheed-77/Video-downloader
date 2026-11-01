package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.ClipRange
import java.util.Locale

/** How far a running download is: shown on its card and in its notification. */
data class Progress(val percent: Int, val speed: String? = null, val etaSeconds: Long? = null) {

    /** "46% · 3.4 MB/s · 0:12 left", leaving out whatever yt-dlp hasn't reported yet. */
    val text: String
        get() = listOfNotNull(
            "$percent%",
            speed,
            etaSeconds?.let { "${ClipRange.formatTime(it.toInt())} left" },
        ).joinToString(" · ")

    companion object {
        // yt-dlp's progress line: "[download]  46.3% of  11.46MiB at    3.40MiB/s ETA 00:03".
        private val SPEED = Regex("""at\s+~?\s*([\d.]+)\s*([KMG])i?B/s""")

        private val PERCENT = Regex("""\[download]\s+([\d.]+)%""")
        private val ETA = Regex("""ETA\s+(\d+(?::\d+){1,2})""")

        /**
         * Reads one line of yt-dlp's progress output (desktop: run with `--newline`), or null when
         * the line isn't a progress line. The Android library does this for the phone.
         */
        fun fromLine(line: String): Progress? {
            val percent = PERCENT.find(line)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            val eta = ETA.find(line)?.groupValues?.get(1)?.let { ClipRange.parse(it) }
            return Progress(percent.toInt(), parseSpeed(line), eta?.toLong()?.takeIf { it > 0 })
        }

        /** "3.4 MB/s" from yt-dlp's progress line, or null when the line has no speed yet. */
        fun parseSpeed(line: String): String? {
            val match = SPEED.find(line) ?: return null
            val value = match.groupValues[1].toDoubleOrNull() ?: return null
            return String.format(Locale.US, "%.1f %sB/s", value, match.groupValues[2])
        }
    }
}
