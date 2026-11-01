package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Badges and the duration text shown in the quality picker. */
class PickerDisplayTest {

    @Test
    fun badgesFollowTheResolution() {
        assertEquals("Best", QualityOption.BEST.badge)
        assertNull(QualityOption.MP3.badge)
        assertEquals("4K", QualityOption.forHeight("2160p", 2160, null).badge)
        assertEquals("2K", QualityOption.forHeight("1440p", 1440, null).badge)
        assertEquals("HD", QualityOption.forHeight("1080p", 1080, null).badge)
        assertEquals("HD", QualityOption.forHeight("720p", 720, null).badge)
        assertEquals("SD", QualityOption.forHeight("480p", 480, null).badge)
    }

    @Test
    fun verticalVideoBadgeUsesTheLabelNotTheHeight() {
        // A 1080x1920 Short is "1080p" → HD, not 2K.
        val rows = FormatParser.parse(listOf(FormatInfo(1080, 1920, "avc1", "mp4a", 0)))
        assertEquals("HD", rows.first { it.label == "1080p" }.badge)
    }

    @Test
    fun durationText() {
        assertNull(VideoMeta.formatDuration(0))
        assertEquals("0:09", VideoMeta.formatDuration(9))
        assertEquals("4:05", VideoMeta.formatDuration(245))
        assertEquals("1:02:03", VideoMeta.formatDuration(3723))
    }
}
