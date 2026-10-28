package com.saheed.videodownloader.ui

import com.saheed.videodownloader.download.DownloadRecord
import com.saheed.videodownloader.download.DownloadStatus

/** One entry of the Downloads list: a single video, or a playlist section with its videos. */
sealed interface DownloadBlock {
    /** Stable id for the list, so Compose keeps each block's state (open menus, collapsed sections). */
    val key: String

    data class Single(val record: DownloadRecord) : DownloadBlock {
        override val key get() = record.id
    }

    data class Playlist(val group: String, val title: String, val videos: List<DownloadRecord>) : DownloadBlock {
        override val key get() = "playlist-$group"
    }
}

/**
 * Groups the rows: each playlist becomes one section, placed where its first video is in the list,
 * with its videos in their own order. Rows without a playlist stay single.
 */
fun toBlocks(records: List<DownloadRecord>): List<DownloadBlock> {
    val seen = mutableSetOf<String>()
    return records.mapNotNull { record ->
        val group = record.group ?: return@mapNotNull DownloadBlock.Single(record)
        // `add` is false when this playlist already has its section.
        if (!seen.add(group)) return@mapNotNull null
        val videos = records.filter { it.group == group }
        DownloadBlock.Playlist(group, videos.firstNotNullOfOrNull { it.groupTitle } ?: "Playlist", videos)
    }
}

/** "6 videos · 2 done · 2 downloading · 1 failed": only the counts that aren't zero. */
fun playlistSummary(videos: List<DownloadRecord>): String {
    fun count(status: DownloadStatus) = videos.count { it.status == status }
    val parts = listOf(
        if (videos.size == 1) "1 video" else "${videos.size} videos",
        "${count(DownloadStatus.DONE)} done",
        "${count(DownloadStatus.RUNNING)} downloading",
        "${count(DownloadStatus.QUEUED)} waiting",
        "${count(DownloadStatus.PAUSED)} stopped",
        "${count(DownloadStatus.FAILED)} failed",
        "${count(DownloadStatus.CANCELLED)} cancelled",
    )
    return (parts.take(1) + parts.drop(1).filterNot { it.startsWith("0 ") }).joinToString(" · ")
}
