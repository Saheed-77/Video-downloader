package com.saheed.videodownloader.formats

import java.util.Locale

/** The part of a video to download, in seconds from the start. */
data class ClipRange(val startSeconds: Int, val endSeconds: Int) {

    /** yt-dlp's `--download-sections` value, e.g. "*90-165" (the `*` means "times, not chapters"). */
    fun toSection(): String = "*$startSeconds-$endSeconds"

    /** "1:30–2:45", for the Downloads tab. */
    val displayText: String get() = "${formatTime(startSeconds)}–${formatTime(endSeconds)}"

    companion object {

        /**
         * Reads a time typed by the user: "90" (seconds), "1:30", "1:02:03". A "." or "," also works
         * as the separator, because the phone's number keyboard may not have ":". Null if unreadable.
         */
        fun parse(text: String): Int? {
            val parts = text.trim().split(':', '.', ',')
            if (parts.isEmpty() || parts.size > 3 || parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            // Every part after the first is minutes or seconds, so it must be below 60.
            if (numbers.drop(1).any { it >= 60 }) return null
            return numbers.fold(0) { total, n -> total * 60 + n }
        }

        /**
         * Checks the Start/End fields against the video's length ([durationSeconds], 0 if unknown).
         * An empty Start means the beginning, an empty End means the end of the video.
         * Returns the range, or a message saying what to fix.
         */
        fun check(startText: String, endText: String, durationSeconds: Int): Result {
            val start = if (startText.isBlank()) 0 else parse(startText) ?: return Result.Invalid(FORMAT_HELP)
            val end = when {
                endText.isNotBlank() -> parse(endText) ?: return Result.Invalid(FORMAT_HELP)
                durationSeconds > 0 -> durationSeconds
                else -> return Result.Invalid("Enter an end time")
            }
            return when {
                durationSeconds > 0 && (start >= durationSeconds || end > durationSeconds) ->
                    Result.Invalid("The video is only ${formatTime(durationSeconds)} long")
                end <= start -> Result.Invalid("End must be after Start")
                else -> Result.Valid(ClipRange(start, end))
            }
        }

        /** "0:09", "4:05", "1:02:03". */
        fun formatTime(seconds: Int): String {
            val h = seconds / 3600
            val m = seconds % 3600 / 60
            val s = seconds % 60
            return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
            else String.format(Locale.US, "%d:%02d", m, s)
        }

        private const val FORMAT_HELP = "Use minutes:seconds, like 1:30"
    }

    /** The outcome of [check]: a usable range, or what's wrong with the input. */
    sealed interface Result {
        data class Valid(val range: ClipRange) : Result
        data class Invalid(val message: String) : Result
    }
}
