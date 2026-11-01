package com.saheed.videodownloader.desktop

import com.saheed.videodownloader.download.ErrorMapper
import com.saheed.videodownloader.formats.FormatInfo
import com.saheed.videodownloader.formats.FormatParser
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import org.json.JSONObject

/** What looking up a link found: one video with its qualities, a playlist, or an error to show. */
sealed interface LookupResult {
    data class Video(val meta: VideoMeta, val options: List<QualityOption>) : LookupResult
    data class PlaylistFound(val playlist: Playlist) : LookupResult
    data class Failed(val message: String) : LookupResult
}

object Lookup {

    /** Asks yt-dlp about [url] (`-J` prints everything as JSON). Blocking: call off the UI thread. */
    fun lookup(url: String): LookupResult {
        return try {
            if (Playlist.isPlaylistUrl(url)) {
                val result = Engine.run(listOf("-J", "--flat-playlist", url))
                if (result.exitCode != 0) return LookupResult.Failed(ErrorMapper.toMessage(result.errors))
                val playlist = Playlist.parse(result.output)
                if (playlist.entries.isEmpty()) LookupResult.Failed("This playlist has no videos that can be downloaded.")
                else LookupResult.PlaylistFound(playlist)
            } else {
                val result = Engine.run(listOf("-J", "--no-playlist", url))
                if (result.exitCode != 0) return LookupResult.Failed(ErrorMapper.toMessage(result.errors))
                val (meta, formats) = parseVideo(result.output)
                LookupResult.Video(meta, FormatParser.parse(formats, DesktopSettings.videoFormat))
            }
        } catch (e: Exception) {
            LookupResult.Failed(ErrorMapper.toMessage(e.message))
        }
    }

    /**
     * yt-dlp's JSON for one video → the details shown above the list, and its formats in the shared
     * [FormatInfo] shape (the Android app gets the same from its library's VideoInfo).
     */
    fun parseVideo(json: String): Pair<VideoMeta, List<FormatInfo>> {
        val root = JSONObject(json)
        val meta = VideoMeta(
            title = root.optString("title").ifBlank { "video" },
            uploader = root.optString("uploader").ifBlank { null },
            durationSeconds = root.optDouble("duration", 0.0).toInt(),
            thumbnailUrl = root.optString("thumbnail").ifBlank { null },
        )
        val array = root.optJSONArray("formats")
        val formats = List(array?.length() ?: 0) { i ->
            val f = array!!.getJSONObject(i)
            FormatInfo(
                width = f.optInt("width", 0),
                height = f.optInt("height", 0),
                vcodec = f.optString("vcodec").ifBlank { null },
                acodec = f.optString("acodec").ifBlank { null },
                sizeBytes = f.optLong("filesize", 0).takeIf { it > 0 } ?: f.optLong("filesize_approx", 0),
            )
        }
        return meta to formats
    }
}
