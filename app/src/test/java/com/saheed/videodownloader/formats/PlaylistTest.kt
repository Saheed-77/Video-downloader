package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTest {

    @Test
    fun wholePlaylistLinksArePlaylists() {
        assertTrue(Playlist.isPlaylistUrl("https://www.youtube.com/playlist?list=PL123"))
        assertTrue(Playlist.isPlaylistUrl("https://m.youtube.com/playlist?list=PL123&si=abc"))
        assertTrue(Playlist.isPlaylistUrl("https://music.youtube.com/playlist?list=OLAK5uy"))
        assertTrue(Playlist.isPlaylistUrl("https://youtube.com/playlist?list=PL123"))
    }

    @Test
    fun videoLinksStaySingleVideos() {
        // A video opened from inside a playlist: still just that video, like before U7.
        assertFalse(Playlist.isPlaylistUrl("https://www.youtube.com/watch?v=abc&list=PL123"))
        assertFalse(Playlist.isPlaylistUrl("https://youtu.be/abc?list=PL123"))
        assertFalse(Playlist.isPlaylistUrl("https://www.youtube.com/watch?v=abc"))
        assertFalse(Playlist.isPlaylistUrl("https://www.youtube.com/shorts/abc"))
        assertFalse(Playlist.isPlaylistUrl("https://www.instagram.com/reel/abc/?list=x"))
        assertFalse(Playlist.isPlaylistUrl("not a link"))
    }

    private val sample = """
        {
          "_type": "playlist",
          "title": "Kerala songs",
          "entries": [
            {"id": "aaa", "url": "https://www.youtube.com/watch?v=aaa", "title": "First song",
             "thumbnails": [{"url": "https://i.ytimg.com/aaa/small.jpg"}, {"url": "https://i.ytimg.com/aaa/big.jpg"}]},
            {"id": "bbb", "title": "[Private video]"},
            {"id": "ccc", "title": "Third song"},
            {"id": "ddd", "url": "https://www.youtube.com/watch?v=ddd", "title": "[Deleted video]"}
          ]
        }
    """.trimIndent()

    @Test
    fun readsTitleAndUsableVideos() {
        val playlist = Playlist.parse(sample)
        assertEquals("Kerala songs", playlist.title)
        assertEquals(listOf("First song", "Third song"), playlist.entries.map { it.title })
        assertEquals(2, playlist.totalCount)
        assertFalse(playlist.isCut)
    }

    @Test
    fun buildsTheLinkFromTheIdAndTakesTheBiggestThumbnail() {
        val (first, third) = Playlist.parse(sample).entries
        assertEquals("https://i.ytimg.com/aaa/big.jpg", first.thumbnailUrl)
        assertEquals("https://www.youtube.com/watch?v=ccc", third.url)
        assertNull(third.thumbnailUrl)
    }

    @Test
    fun longPlaylistsAreCut() {
        val entries = (1..150).joinToString(",") { """{"id": "v$it", "title": "Video $it"}""" }
        val playlist = Playlist.parse("""{"title": "Long", "entries": [$entries]}""")
        assertEquals(Playlist.MAX_VIDEOS, playlist.entries.size)
        assertEquals(150, playlist.totalCount)
        assertTrue(playlist.isCut)
    }

    @Test
    fun fixedQualityChoices() {
        assertEquals(
            listOf("Best quality", "1080p", "720p", "480p", "360p", "MP3 audio"),
            Playlist.QUALITY_OPTIONS.map { it.label },
        )
    }
}
