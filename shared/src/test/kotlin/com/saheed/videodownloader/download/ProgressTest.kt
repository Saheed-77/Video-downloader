package com.saheed.videodownloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTest {

    @Test
    fun readsTheSpeedFromYtDlpsLine() {
        assertEquals("3.4 MB/s", Progress.parseSpeed("[download]  46.3% of  11.46MiB at    3.40MiB/s ETA 00:03"))
        assertEquals("957.8 KB/s", Progress.parseSpeed("[download]  84.9% of   11.46MiB at  957.82KiB/s ETA 00:02"))
        assertEquals("1.2 GB/s", Progress.parseSpeed("at 1.20GiB/s"))
        // Estimated sizes ("~") and lines without a speed.
        assertEquals("2.0 MB/s", Progress.parseSpeed("[download]  10.0% of ~ 50.00MiB at  2.00MiB/s ETA 00:20"))
        assertNull(Progress.parseSpeed("[download] 100% of   11.46MiB in 00:00:03"))
        assertNull(Progress.parseSpeed("[download]   0.0% of 11.46MiB at Unknown B/s"))
    }

    @Test
    fun cardText() {
        assertEquals("46% · 3.4 MB/s · 0:12 left", Progress(46, "3.4 MB/s", 12).text)
        assertEquals("5%", Progress(5).text)
        assertEquals("80% · 1:05 left", Progress(80, etaSeconds = 65).text)
    }

    @Test
    fun readsWholeProgressLines() {
        assertEquals(
            Progress(46, "3.4 MB/s", 3),
            Progress.fromLine("[download]  46.3% of   11.46MiB at    3.40MiB/s ETA 00:03"),
        )
        assertEquals(Progress(100, "3.2 MB/s"), Progress.fromLine("[download] 100% of   11.46MiB in 00:00:03 at 3.21MiB/s"))
        assertEquals(Progress(5, etaSeconds = 3723), Progress.fromLine("[download]   5.0% of ~ 2.00GiB at Unknown B/s ETA 1:02:03"))
        assertNull(Progress.fromLine("[youtube] jNQXAC9IVRw: Downloading webpage"))
    }
}
