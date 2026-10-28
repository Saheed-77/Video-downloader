package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The yt-dlp command-line options each kind of download gets, and finding the finished file. */
class DownloaderOptionsTest {

    @get:Rule
    val tmp = TemporaryFolder()

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
    fun clipCutsExactlyForVideoAndMp3() {
        val clip = DownloadExtras(clip = ClipRange(60, 90))
        for (isAudio in listOf(false, true)) {
            val options = Downloader.options(format, isAudio, clip)
            assertTrue("--download-sections" to "*60-90" in options)
            assertTrue("--force-keyframes-at-cuts" to null in options)
        }
        assertFalse(Downloader.options(format, false, DownloadExtras()).any { it.first == "--download-sections" })
    }

    @Test
    fun clipsGetNoSubtitles() {
        // yt-dlp doesn't cut subtitles to the clip, so they'd be out of sync.
        val options = Downloader.options(format, false, DownloadExtras(subtitles = true, clip = ClipRange(60, 90)))
        assertFalse(options.any { it.first.contains("sub") })
        assertTrue("--download-sections" to "*60-90" in options)
    }

    @Test
    fun playlistDownloadsPauseSingleOnesDont() {
        val paused = Downloader.options(format, false, DownloadExtras(), pause = true)
        assertTrue("--sleep-interval" to "5" in paused)
        assertTrue("--max-sleep-interval" to "10" in paused)
        assertTrue("--sleep-requests" to "1" in paused)
        assertFalse(Downloader.options(format, false, DownloadExtras()).any { it.first.startsWith("--sleep") })
    }

    @Test
    fun alwaysSingleVideo() {
        val options = Downloader.options(format, isAudio = false, DownloadExtras())
        assertEquals(1, options.count { it.first == "--no-playlist" })
    }

    @Test
    fun finishedFileIsTheVideoNotANewerSubtitleFile() {
        // What happened on the S24: the MP4 got an older time than the leftover .vtt.
        val dir = tmp.newFolder()
        val video = dir.resolve("talk.mp4").apply { writeBytes(ByteArray(50_000)); setLastModified(1_000_000L) }
        dir.resolve("talk.en.vtt").apply { writeBytes(ByteArray(2_000)); setLastModified(2_000_000L) }
        assertEquals(video, Downloader.mainFile(dir))
    }
}
