package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatParserTest {

    private fun video(w: Int, h: Int, size: Long = 0, acodec: String = "none") =
        FormatInfo(width = w, height = h, vcodec = "avc1", acodec = acodec, sizeBytes = size)

    private fun audio(size: Long) =
        FormatInfo(width = 0, height = 0, vcodec = "none", acodec = "opus", sizeBytes = size)

    @Test
    fun youtubeStyleListIsDedupedAndSortedHighestFirst() {
        val formats = listOf(
            audio(3_000_000), audio(4_000_000),
            video(640, 360, 10_000_000), video(640, 360, 12_000_000),
            video(1280, 720, 40_000_000), video(1280, 720, 30_000_000),
            video(1920, 1080, 80_000_000),
            video(854, 480, 20_000_000),
        )
        val labels = FormatParser.parse(formats).map { it.label }
        assertEquals(listOf("Best quality", "1080p", "720p", "480p", "360p", "MP3 audio"), labels)
    }

    @Test
    fun only720pVideoShowsNo1080p() {
        val labels = FormatParser.parse(listOf(video(1280, 720), video(640, 360), audio(1))).map { it.label }
        assertEquals(listOf("Best quality", "720p", "360p", "MP3 audio"), labels)
    }

    @Test
    fun videoOnlySizeAddsBestAudio() {
        val rows = FormatParser.parse(listOf(video(1280, 720, 40_000_000), audio(5_000_000)))
        assertEquals(45_000_000L, rows.first { it.label == "720p" }.sizeBytes)
        assertEquals(45_000_000L, rows.first().sizeBytes) // Best = top resolution
        assertEquals(5_000_000L, rows.last().sizeBytes)   // MP3 ≈ audio track
    }

    @Test
    fun formatWithItsOwnAudioIsNotDoubleCounted() {
        val rows = FormatParser.parse(listOf(video(640, 360, 12_000_000, acodec = "mp4a"), audio(5_000_000)))
        assertEquals(12_000_000L, rows.first { it.label == "360p" }.sizeBytes)
    }

    @Test
    fun unknownSizesStayNull() {
        val rows = FormatParser.parse(listOf(video(1280, 720)))
        assertNull(rows.first { it.label == "720p" }.sizeBytes)
        assertEquals("720p", rows.first { it.label == "720p" }.displayText)
    }

    @Test
    fun verticalShortIsLabelledByShorterSideButFiltersOnRealHeight() {
        val rows = FormatParser.parse(listOf(video(1080, 1920), video(720, 1280)))
        assertEquals(listOf("Best quality", "1080p", "720p", "MP3 audio"), rows.map { it.label })
        assertEquals(
            "bestvideo[height<=1920]+bestaudio/best[height<=1920]",
            rows.first { it.label == "1080p" }.format
        )
    }

    @Test
    fun noHeightsGivesBestAndMp3Only() {
        // e.g. Instagram/Facebook formats without resolution info
        val noHeight = FormatInfo(width = 0, height = 0, vcodec = null, acodec = null, sizeBytes = 0)
        val rows = FormatParser.parse(listOf(noHeight))
        assertEquals(listOf("Best quality", "MP3 audio"), rows.map { it.label })
    }

    @Test
    fun emptyListStillOffersBestAndMp3() {
        assertEquals(listOf("Best quality", "MP3 audio"), FormatParser.parse(emptyList()).map { it.label })
    }

    @Test
    fun mp3RowIsAudioAndVideoRowsAreNot() {
        val rows = FormatParser.parse(listOf(video(1280, 720)))
        assertTrue(rows.last().isAudio)
        assertFalse(rows.first().isAudio)
        assertFalse(rows[1].isAudio)
    }

    @Test
    fun sizeText() {
        assertEquals("<1 MB", QualityOption.formatSize(500_000))
        assertEquals("85 MB", QualityOption.formatSize(85_400_000))
        assertEquals("1.2 GB", QualityOption.formatSize(1_234_000_000))
        assertEquals("720p · 45 MB", QualityOption.forHeight("720p", 720, 45_000_000).displayText)
    }
}
