package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultQualityTest {

    private val options = listOf(
        QualityOption.BEST,
        QualityOption.forHeight("1080p", 1080, null),
        QualityOption.forHeight("480p", 480, null),
        QualityOption.forHeight("240p", 240, null),
        QualityOption.MP3,
    )

    @Test
    fun askPicksNothing() = assertNull(DefaultQuality.ASK.pick(options))

    @Test
    fun exactOrNextLowerResolution() {
        assertEquals("1080p", DefaultQuality.P1080.pick(options)?.label)
        // No 720p: the highest below it.
        assertEquals("480p", DefaultQuality.P720.pick(options)?.label)
        // Nothing at or below 480p would need the lowest one; here 480p exists.
        assertEquals("480p", DefaultQuality.P480.pick(options)?.label)
    }

    @Test
    fun lowestWhenNothingIsBelow() {
        val hd = listOf(QualityOption.BEST, QualityOption.forHeight("1080p", 1080, null), QualityOption.MP3)
        assertEquals("1080p", DefaultQuality.P480.pick(hd)?.label)
    }

    @Test
    fun bestAndMp3() {
        assertEquals(QualityOption.BEST, DefaultQuality.BEST.pick(options))
        assertEquals(QualityOption.MP3, DefaultQuality.MP3.pick(options))
    }

    @Test
    fun siteWithoutResolutionsFallsBackToBest() {
        assertEquals(QualityOption.BEST, DefaultQuality.P720.pick(listOf(QualityOption.BEST, QualityOption.MP3)))
    }

    @Test
    fun worksOnThePlaylistList() {
        assertEquals("720p", DefaultQuality.P720.pick(Playlist.QUALITY_OPTIONS)?.label)
    }
}
