package com.saheed.videodownloader.desktop

import com.saheed.videodownloader.formats.FormatParser
import com.saheed.videodownloader.formats.VideoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LookupTest {

    // A cut-down version of what `yt-dlp -J` prints for a YouTube video.
    private val json = """
        {
          "title": "Me at the zoo", "uploader": "jawed", "duration": 19.0,
          "thumbnail": "https://i.ytimg.com/vi/jNQXAC9IVRw/hqdefault.jpg",
          "formats": [
            {"format_id": "140", "vcodec": "none", "acodec": "mp4a.40.2", "filesize": 300000},
            {"format_id": "160", "width": 192, "height": 144, "vcodec": "avc1.4d400c", "acodec": "none", "filesize": 100000},
            {"format_id": "133", "width": 320, "height": 240, "vcodec": "avc1.4d400d", "acodec": "none", "filesize_approx": 200000},
            {"format_id": "sb0", "vcodec": "none", "acodec": "none"}
          ]
        }
    """.trimIndent()

    @Test
    fun readsTheVideoDetails() {
        val (meta, _) = Lookup.parseVideo(json)
        assertEquals("Me at the zoo", meta.title)
        assertEquals("jawed", meta.uploader)
        assertEquals(19, meta.durationSeconds)
        assertEquals("0:19", meta.durationText)
    }

    @Test
    fun formatsBecomeTheSameQualityRowsAsOnAndroid() {
        val (_, formats) = Lookup.parseVideo(json)
        val rows = FormatParser.parse(formats, VideoFormat.COMPATIBLE)
        assertEquals(listOf("Best quality", "240p", "144p", "MP3 audio"), rows.map { it.label })
        // filesize_approx is used when filesize is missing; the audio track is added to video-only formats.
        assertEquals(500_000L, rows.first { it.label == "240p" }.sizeBytes)
    }

    @Test
    fun missingFieldsAreNull() {
        val (meta, formats) = Lookup.parseVideo("""{"formats": []}""")
        assertEquals("video", meta.title)
        assertNull(meta.uploader)
        assertNull(meta.thumbnailUrl)
        assertEquals(0, formats.size)
    }
}
