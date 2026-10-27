package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.DownloadExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The yt-dlp command-line options each kind of download gets. */
class DownloaderOptionsTest {

    private val format = "bestvideo[height<=720]+bestaudio/best[height<=720]"

    @Test
    fun videoWithoutSubtitles() {
        val options = Downloader.options(format, isAudio = false, DownloadExtras())
        assertTrue("-f" to format in options)
        assertTrue("--merge-output-format" to "mp4" in options)
        assertFalse(options.any { it.first == "--embed-subs" })
    }

    @Test
    fun videoWithSubtitlesEmbedsEnglishOnly() {
        val options = Downloader.options(format, isAudio = false, DownloadExtras(subtitles = true))
        assertTrue("--write-subs" to null in options)
        assertTrue("--sub-langs" to "en.*" in options)
        assertTrue("--embed-subs" to null in options)
        assertFalse(options.any { it.first == "--write-auto-subs" })
    }

    @Test
    fun mp3IgnoresSubtitles() {
        val options = Downloader.options(null, isAudio = true, DownloadExtras(subtitles = true))
        assertTrue("--audio-format" to "mp3" in options)
        assertFalse(options.any { it.first.contains("sub") })
    }

    @Test
    fun alwaysSingleVideo() {
        val options = Downloader.options(format, isAudio = false, DownloadExtras())
        assertEquals(1, options.count { it.first == "--no-playlist" })
    }
}
