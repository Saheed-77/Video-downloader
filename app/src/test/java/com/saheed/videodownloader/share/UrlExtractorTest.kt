package com.saheed.videodownloader.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    @Test
    fun youtuBe() {
        assertEquals(
            "https://youtu.be/dQw4w9WgXcQ?si=abc123",
            UrlExtractor.extract("https://youtu.be/dQw4w9WgXcQ?si=abc123")
        )
    }

    @Test
    fun youtubeShorts() {
        assertEquals(
            "https://youtube.com/shorts/aBcDeFgHiJk?si=xyz",
            UrlExtractor.extract("https://youtube.com/shorts/aBcDeFgHiJk?si=xyz")
        )
    }

    @Test
    fun instagramReel() {
        assertEquals(
            "https://www.instagram.com/reel/C1a2b3c4d5e/?igsh=MTc4",
            UrlExtractor.extract("https://www.instagram.com/reel/C1a2b3c4d5e/?igsh=MTc4")
        )
    }

    @Test
    fun fbWatch() {
        assertEquals("https://fb.watch/abcDEF123/", UrlExtractor.extract("https://fb.watch/abcDEF123/"))
    }

    @Test
    fun textAroundLink() {
        assertEquals(
            "https://fb.watch/abcDEF123/",
            UrlExtractor.extract("Check this out https://fb.watch/abcDEF123/ so funny")
        )
    }

    @Test
    fun linkOnNewLineAfterTitle() {
        assertEquals(
            "https://youtu.be/dQw4w9WgXcQ",
            UrlExtractor.extract("Never Gonna Give You Up\nhttps://youtu.be/dQw4w9WgXcQ")
        )
    }

    @Test
    fun trailingPunctuationDropped() {
        assertEquals("https://youtu.be/dQw4w9WgXcQ", UrlExtractor.extract("Watch https://youtu.be/dQw4w9WgXcQ."))
    }

    @Test
    fun firstLinkWins() {
        assertEquals("http://a.com/1", UrlExtractor.extract("http://a.com/1 and https://b.com/2"))
    }

    @Test
    fun noLink() {
        assertNull(UrlExtractor.extract("just some text, no link here"))
    }

    @Test
    fun nullOrEmpty() {
        assertNull(UrlExtractor.extract(null))
        assertNull(UrlExtractor.extract(""))
    }
}
