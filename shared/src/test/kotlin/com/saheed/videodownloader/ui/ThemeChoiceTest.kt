package com.saheed.videodownloader.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeChoiceTest {

    @Test
    fun savedName_isRead() {
        assertEquals(ThemeChoice.MIDNIGHT, ThemeChoice.fromName("MIDNIGHT"))
    }

    @Test
    fun nothingSaved_givesDefault() {
        assertEquals(ThemeChoice.PLAY_DOWN, ThemeChoice.fromName(null))
        assertEquals(ThemeChoice.MIDNIGHT, ThemeChoice.fromName(null, ThemeChoice.MIDNIGHT))
    }

    @Test
    fun unknownName_givesDefault() {
        assertEquals(ThemeChoice.PLAY_DOWN, ThemeChoice.fromName("SUNSET"))
    }
}
