package com.saheed.videodownloader.desktop

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun aSecondCopyGetsANumber() {
        val dir = tmp.newFolder()
        assertEquals("Me at the zoo.mp4", uniqueFile(dir, "Me at the zoo.mp4").name)
        dir.resolve("Me at the zoo.mp4").writeText("x")
        assertEquals("Me at the zoo (1).mp4", uniqueFile(dir, "Me at the zoo.mp4").name)
        dir.resolve("Me at the zoo (1).mp4").writeText("x")
        assertEquals("Me at the zoo (2).mp4", uniqueFile(dir, "Me at the zoo.mp4").name)
    }

    @Test
    fun fileTypes() {
        assertEquals("video/mp4", mimeOf(java.io.File("a.MP4")))
        assertEquals("audio/mpeg", mimeOf(java.io.File("song.mp3")))
    }
}
