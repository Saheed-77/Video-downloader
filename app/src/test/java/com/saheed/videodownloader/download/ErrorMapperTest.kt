package com.saheed.videodownloader.download

import org.junit.Assert.assertEquals
import org.junit.Test

/** Sample errors are in the shape yt-dlp prints them. */
class ErrorMapperTest {

    private fun map(s: String?) = ErrorMapper.toMessage(s)

    @Test
    fun noInternet() {
        assertEquals(
            ErrorMapper.NO_INTERNET,
            map("ERROR: [youtube] abc: Unable to download API page: <urlopen error [Errno 7] No address associated with hostname> (caused by TransportError)")
        )
        assertEquals(
            ErrorMapper.NO_INTERNET,
            map("ERROR: [generic] Unable to download webpage: [Errno -3] Temporary failure in name resolution")
        )
    }

    @Test
    fun privateVideo() {
        assertEquals(
            ErrorMapper.PRIVATE,
            map("ERROR: [youtube] abc: Private video. Sign in if you've been granted access to this video")
        )
    }

    @Test
    fun youtubeBotCheck() {
        assertEquals(
            ErrorMapper.BOT_CHECK,
            map("ERROR: [youtube] abc: Sign in to confirm you're not a bot. Use --cookies-from-browser or --cookies for the authentication.")
        )
    }

    @Test
    fun instagramLogin() {
        assertEquals(
            ErrorMapper.LOGIN,
            map("ERROR: [Instagram] C1a2b: Requested content is not available, rate-limit reached or login required. Use --cookies, --cookies-from-browser, --username and --password")
        )
    }

    @Test
    fun facebookLogin() {
        assertEquals(
            ErrorMapper.LOGIN,
            map("ERROR: [facebook] 123: This video is only available for registered users. You may want to use --cookies-from-browser to pass cookies")
        )
    }

    @Test
    fun unsupportedLink() {
        assertEquals(ErrorMapper.UNSUPPORTED, map("ERROR: Unsupported URL: https://example.com/page"))
        assertEquals(ErrorMapper.UNSUPPORTED, map("ERROR: 'hello' is not a valid URL."))
    }

    @Test
    fun unavailable() {
        assertEquals(ErrorMapper.UNAVAILABLE, map("ERROR: [youtube] abc: Video unavailable. This video has been removed by the uploader"))
    }

    @Test
    fun missingFormat() {
        assertEquals(ErrorMapper.NO_FORMAT, map("ERROR: [youtube] abc: Requested format is not available. Use --list-formats for a list of available formats"))
    }

    @Test
    fun anythingElseSuggestsUpdating() {
        assertEquals(ErrorMapper.UPDATE, map("ERROR: unable to extract player response; please report this issue"))
        assertEquals(ErrorMapper.UPDATE, map("ERROR: HTTP Error 403: Forbidden"))
        assertEquals(ErrorMapper.UPDATE, map(null))
    }

    @Test
    fun missingPlaylist() {
        // Seen on the S24 with a playlist id that doesn't exist.
        assertEquals(
            ErrorMapper.NO_PLAYLIST,
            ErrorMapper.toMessage("ERROR: [youtube:tab] PLabc: YouTube said: The playlist does not exist."),
        )
    }
}
