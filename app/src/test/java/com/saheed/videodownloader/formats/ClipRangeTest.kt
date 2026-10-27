package com.saheed.videodownloader.formats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipRangeTest {

    @Test
    fun parsesTheWaysPeopleTypeTimes() {
        assertEquals(90, ClipRange.parse("90"))
        assertEquals(90, ClipRange.parse("1:30"))
        assertEquals(90, ClipRange.parse("1.30"))
        assertEquals(90, ClipRange.parse(" 01:30 "))
        assertEquals(3723, ClipRange.parse("1:02:03"))
        assertEquals(0, ClipRange.parse("0:00"))
    }

    @Test
    fun rejectsUnreadableTimes() {
        assertNull(ClipRange.parse(""))
        assertNull(ClipRange.parse("abc"))
        assertNull(ClipRange.parse("1:75"))     // 75 seconds isn't a valid "seconds" part
        assertNull(ClipRange.parse("1::30"))
        assertNull(ClipRange.parse("1:2:3:4"))
        assertNull(ClipRange.parse("-5"))
    }

    @Test
    fun sectionForYtDlp() {
        assertEquals("*90-165", ClipRange(90, 165).toSection())
        assertEquals("1:30–2:45", ClipRange(90, 165).displayText)
    }

    @Test
    fun emptyFieldsMeanStartAndEndOfTheVideo() {
        assertEquals(ClipRange.Result.Valid(ClipRange(0, 90)), ClipRange.check("", "1:30", 600))
        assertEquals(ClipRange.Result.Valid(ClipRange(60, 600)), ClipRange.check("1:00", "", 600))
    }

    @Test
    fun explainsWhatToFix() {
        assertEquals(ClipRange.Result.Invalid("End must be after Start"), ClipRange.check("2:00", "1:00", 600))
        assertEquals(ClipRange.Result.Invalid("The video is only 10:00 long"), ClipRange.check("1:00", "11:00", 600))
        // A Start past the end, with End left empty, names the real problem.
        assertEquals(ClipRange.Result.Invalid("The video is only 10:00 long"), ClipRange.check("12:00", "", 600))
        assertEquals(ClipRange.Result.Invalid("Use minutes:seconds, like 1:30"), ClipRange.check("x", "1:00", 600))
        // Unknown length: an end time is needed.
        assertEquals(ClipRange.Result.Invalid("Enter an end time"), ClipRange.check("1:00", "", 0))
        assertEquals(ClipRange.Result.Valid(ClipRange(60, 120)), ClipRange.check("1:00", "2:00", 0))
    }

    @Test
    fun timeText() {
        assertEquals("0:00", ClipRange.formatTime(0))
        assertEquals("4:05", ClipRange.formatTime(245))
        assertEquals("1:02:03", ClipRange.formatTime(3723))
    }
}
