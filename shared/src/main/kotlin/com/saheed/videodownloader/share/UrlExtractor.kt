package com.saheed.videodownloader.share

/** Pulls the link out of shared text, e.g. "Check this out https://fb.watch/abc/ !" → the URL. */
object UrlExtractor {

    private val URL = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    /** Returns the first http(s) link in [text], or null if there is none. */
    fun extract(text: String?): String? {
        if (text == null) return null
        val match = URL.find(text) ?: return null
        // Sentence punctuation right after a link isn't part of it.
        return match.value.trimEnd('.', ',', ')', '!', '?', '"', '\'')
    }
}
