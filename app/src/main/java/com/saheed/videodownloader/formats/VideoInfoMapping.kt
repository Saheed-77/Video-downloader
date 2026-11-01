package com.saheed.videodownloader.formats

import com.yausername.youtubedl_android.mapper.VideoInfo

// The Android library's VideoInfo turned into the shared types (the desktop app reads yt-dlp's JSON
// itself). These are *extension functions*: they add `fromVideoInfo` to the shared classes from here.

/** The quality rows for a video looked up with youtubedl-android. */
fun FormatParser.fromVideoInfo(info: VideoInfo, prefer: VideoFormat = VideoFormat.COMPATIBLE): List<QualityOption> =
    parse(
        info.formats.orEmpty().map {
            FormatInfo(
                width = it.width,
                height = it.height,
                vcodec = it.vcodec,
                acodec = it.acodec,
                sizeBytes = if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate,
            )
        },
        prefer,
    )

/** The title, channel, length and thumbnail shown above the quality list. */
fun VideoMeta.Companion.fromVideoInfo(info: VideoInfo) = VideoMeta(
    title = info.title ?: "video",
    uploader = info.uploader?.takeIf { it.isNotBlank() },
    durationSeconds = info.duration,
    thumbnailUrl = info.thumbnail,
)
