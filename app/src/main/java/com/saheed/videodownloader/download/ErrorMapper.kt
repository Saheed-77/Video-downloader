package com.saheed.videodownloader.download

/** Turns yt-dlp's technical error output into a short message a person can act on. */
object ErrorMapper {

    const val NO_INTERNET = "No internet connection"
    const val PRIVATE = "This video is private"
    const val BOT_CHECK = "YouTube blocked this download for now. Try updating the downloader engine, or try again later."
    const val LOGIN = "Login required. Sign in under Settings → Accounts."
    const val UNSUPPORTED = "Unsupported link"
    const val UNAVAILABLE = "This video is unavailable"
    const val NO_PLAYLIST = "This playlist doesn't exist or is private"
    const val NO_FORMAT = "That quality isn't available for this video. Try another one."
    const val UPDATE = "Download failed. Try updating the downloader engine."

    // Checked in order: the first rule whose words appear in the error wins.
    // Order matters, e.g. "Private video. Sign in if you've been granted access" must hit PRIVATE, not LOGIN.
    private val rules = listOf(
        NO_INTERNET to listOf(
            "errno 7", "errno -2", "errno -3", "errno 101", "name or service not known",
            "temporary failure in name resolution", "no address associated with hostname",
            "network is unreachable", "failed to resolve", "timed out", "connection refused",
        ),
        PRIVATE to listOf("private video", "video is private"),
        BOT_CHECK to listOf("confirm you're not a bot", "confirm you’re not a bot"),
        LOGIN to listOf(
            "login required", "log in", "sign in", "requires authentication",
            "use --cookies", "cookies-from-browser", "account authentication",
        ),
        UNSUPPORTED to listOf("unsupported url", "is not a valid url"),
        NO_PLAYLIST to listOf("playlist does not exist", "this playlist is private"),
        UNAVAILABLE to listOf("video unavailable", "video is unavailable", "has been removed", "not available in your country"),
        NO_FORMAT to listOf("requested format is not available"),
    )

    fun toMessage(error: String?): String {
        val text = error?.lowercase() ?: return UPDATE
        return rules.firstOrNull { (_, words) -> words.any { it in text } }?.first ?: UPDATE
    }
}
