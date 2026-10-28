package com.saheed.videodownloader.ui

import com.saheed.videodownloader.download.DownloadRecord
import com.saheed.videodownloader.download.DownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadBlocksTest {

    private fun row(id: String, status: DownloadStatus = DownloadStatus.DONE, group: String? = null, title: String? = null) =
        DownloadRecord(
            id = id, url = "https://youtu.be/$id", title = id, thumbnailUrl = null, label = "720p",
            format = null, isAudio = false, group = group, groupTitle = title, status = status, createdAt = 0L,
        )

    @Test
    fun playlistsBecomeOneSectionWhereTheirFirstVideoIs() {
        val blocks = toBlocks(
            listOf(
                row("single1"),
                row("a1", group = "A", title = "Songs"),
                row("a2", group = "A", title = "Songs"),
                row("single2"),
                row("b1", group = "B"),
            )
        )
        assertEquals(listOf("single1", "playlist-A", "single2", "playlist-B"), blocks.map { it.key })
        val songs = blocks[1] as DownloadBlock.Playlist
        assertEquals("Songs", songs.title)
        assertEquals(listOf("a1", "a2"), songs.videos.map { it.id })
        // Rows saved before playlists had names.
        assertEquals("Playlist", (blocks[3] as DownloadBlock.Playlist).title)
    }

    @Test
    fun summaryShowsOnlyCountsThatAreNotZero() {
        val videos = listOf(
            row("1", DownloadStatus.DONE), row("2", DownloadStatus.DONE),
            row("3", DownloadStatus.RUNNING), row("4", DownloadStatus.RUNNING),
            row("5", DownloadStatus.QUEUED), row("6", DownloadStatus.FAILED),
        )
        assertEquals("6 videos · 2 done · 2 downloading · 1 waiting · 1 failed", playlistSummary(videos))
        assertEquals("1 video · 1 stopped", playlistSummary(listOf(row("x", DownloadStatus.PAUSED))))
    }
}
