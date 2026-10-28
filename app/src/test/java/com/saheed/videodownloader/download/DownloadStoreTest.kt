package com.saheed.videodownloader.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DownloadStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun record(id: String, status: DownloadStatus = DownloadStatus.QUEUED) = DownloadRecord(
        id = id,
        url = "https://youtu.be/abc",
        title = "A video",
        thumbnailUrl = null,
        label = "720p",
        format = "bestvideo[height<=720]+bestaudio/best[height<=720]",
        isAudio = false,
        status = status,
        createdAt = 1_000L,
    )

    @Test
    fun jsonRoundTripKeepsEveryField() {
        val done = record("1", DownloadStatus.DONE).copy(
            thumbnailUrl = "https://i.ytimg.com/x.jpg",
            uri = "content://media/external/downloads/42",
            mime = "video/mp4",
            sizeBytes = 45_000_000L,
            subtitles = true,
            clipStart = 90,
            clipEnd = 165,
            group = "playlist-1",
        )
        assertEquals(done, DownloadRecord.fromJson(done.toJson()))
    }

    @Test
    fun jsonRoundTripKeepsNullsAsNulls() {
        val mp3 = record("2").copy(label = "MP3 audio", format = null, isAudio = true)
        val back = DownloadRecord.fromJson(mp3.toJson())
        assertEquals(mp3, back)
        assertNull(back.format)
        assertNull(back.sizeBytes)
    }

    @Test
    fun recordsSurviveReloadingFromTheFile() {
        val file = tmp.newFile("downloads.json")
        DownloadStore.init(file)
        DownloadStore.add(record("a"))
        DownloadStore.add(record("b"))
        DownloadStore.update("a") { it.copy(status = DownloadStatus.FAILED, error = "This video is private.") }
        DownloadStore.remove("b")

        DownloadStore.init(file) // as if the app had been closed and opened again
        val records = DownloadStore.records.value
        assertEquals(listOf("a"), records.map { it.id })
        assertEquals(DownloadStatus.FAILED, records[0].status)
        assertEquals("This video is private.", records[0].error)
    }

    @Test
    fun newestComesFirst() {
        DownloadStore.init(tmp.newFile("downloads.json"))
        DownloadStore.add(record("old"))
        DownloadStore.add(record("new"))
        assertEquals(listOf("new", "old"), DownloadStore.records.value.map { it.id })
    }

    @Test
    fun cancellingAPlaylistVideoCancelsTheWholeGroup() {
        DownloadStore.init(tmp.newFile("downloads.json"))
        DownloadStore.addAll(
            listOf(
                record("p1", DownloadStatus.DONE).copy(group = "g"),
                record("p2", DownloadStatus.RUNNING).copy(group = "g"),
                record("p3").copy(group = "g"),
                record("other"),
            )
        )
        DownloadStore.cancelGroup("g")
        val status = DownloadStore.records.value.associate { it.id to it.status }
        assertEquals(DownloadStatus.DONE, status["p1"])
        assertEquals(DownloadStatus.CANCELLED, status["p2"]) // running ones too: both lanes stop
        assertEquals(DownloadStatus.CANCELLED, status["p3"])
        assertEquals(DownloadStatus.QUEUED, status["other"])
    }

    @Test
    fun addAllKeepsTheGivenOrderOnTop() {
        DownloadStore.init(tmp.newFile("downloads.json"))
        DownloadStore.add(record("old"))
        DownloadStore.addAll(listOf(record("a"), record("b")))
        assertEquals(listOf("a", "b", "old"), DownloadStore.records.value.map { it.id })
    }

    @Test
    fun aDamagedFileStartsAnEmptyList() {
        val file = tmp.newFile("downloads.json").apply { writeText("not json") }
        DownloadStore.init(file)
        assertEquals(emptyList<DownloadRecord>(), DownloadStore.records.value)
    }
}
