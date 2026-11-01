package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFormatTest {

    @Test
    fun autoDependsOnThePhone() {
        assertEquals(VideoFormat.SMALLEST, VideoFormat.AUTO.resolve(hasHardwareAv1 = true))
        assertEquals(VideoFormat.COMPATIBLE, VideoFormat.AUTO.resolve(hasHardwareAv1 = false))
        // A chosen format is kept whatever the phone.
        assertEquals(VideoFormat.COMPATIBLE, VideoFormat.COMPATIBLE.resolve(hasHardwareAv1 = true))
        assertEquals(VideoFormat.SMALLEST, VideoFormat.SMALLEST.resolve(hasHardwareAv1 = false))
    }

    @Test
    fun sortStrings() {
        assertEquals("res,vcodec:av01,acodec:opus", VideoFormat.SMALLEST.sort)
        assertEquals("res,vcodec:h264,acodec:m4a", VideoFormat.COMPATIBLE.sort)
    }

    @Test
    fun codecRanking() {
        assertEquals(0, VideoFormat.SMALLEST.codecRank("av01.0.08M.08"))
        assertEquals(1, VideoFormat.SMALLEST.codecRank("vp09.00.40.08"))
        assertEquals(0, VideoFormat.COMPATIBLE.codecRank("avc1.64001F"))
        assertEquals(2, VideoFormat.COMPATIBLE.codecRank("av01.0.05M.08"))
        assertEquals(3, VideoFormat.COMPATIBLE.codecRank(null))
    }

    @Test
    fun pickerSizeFollowsThePreferredCodec() {
        // One 720p in three codecs, plus a separate audio track.
        val formats = listOf(
            FormatInfo(1280, 720, "avc1.64001F", "none", 50_000_000),
            FormatInfo(1280, 720, "vp09.00.31.08", "none", 35_000_000),
            FormatInfo(1280, 720, "av01.0.05M.08", "none", 25_000_000),
            FormatInfo(0, 0, "none", "opus", 5_000_000),
        )
        val compatible = FormatParser.parse(formats, VideoFormat.COMPATIBLE).first { it.label == "720p" }
        val smallest = FormatParser.parse(formats, VideoFormat.SMALLEST).first { it.label == "720p" }
        assertEquals(55_000_000L, compatible.sizeBytes)
        assertEquals(30_000_000L, smallest.sizeBytes)
    }
}
