package com.saheed.videodownloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CookiesTest {

    @Test
    fun linksMatchTheirSite() {
        assertEquals(LoginSite.INSTAGRAM, Cookies.siteFor("https://www.instagram.com/reel/abc/"))
        assertEquals(LoginSite.INSTAGRAM, Cookies.siteFor("https://instagram.com/p/xyz"))
        assertEquals(LoginSite.FACEBOOK, Cookies.siteFor("https://m.facebook.com/watch/?v=1"))
        assertEquals(LoginSite.FACEBOOK, Cookies.siteFor("https://fb.watch/abc/"))
        assertNull(Cookies.siteFor("https://www.youtube.com/watch?v=abc"))
        // A look-alike host must not match.
        assertNull(Cookies.siteFor("https://notinstagram.com/reel/abc"))
        assertNull(Cookies.siteFor("not a link"))
    }

    @Test
    fun readsTheBrowsersCookieHeader() {
        assertEquals(
            listOf("sessionid" to "abc%3A123", "csrftoken" to "x=y"),
            Cookies.parseCookieHeader("sessionid=abc%3A123; csrftoken=x=y"),
        )
        assertEquals(emptyList<Pair<String, String>>(), Cookies.parseCookieHeader(null))
    }

    @Test
    fun writesNetscapeCookiesTxt() {
        val text = Cookies.toNetscape(".instagram.com", listOf("sessionid" to "abc", "ds_user_id" to "42"), 1_900_000_000)
        val lines = text.trimEnd().lines()
        assertTrue(lines[0].startsWith("# Netscape HTTP Cookie File"))
        assertEquals(".instagram.com\tTRUE\t/\tTRUE\t1900000000\tsessionid\tabc", lines[1])
        assertEquals(3, lines.size)
    }

    @Test
    fun downloadsSendTheCookiesFile() {
        val file = java.io.File("cookies-instagram.txt")
        val options = Downloader.options("best", false, com.saheed.videodownloader.formats.DownloadExtras(), cookies = file)
        assertTrue("--cookies" to file.absolutePath in options)
    }
}
