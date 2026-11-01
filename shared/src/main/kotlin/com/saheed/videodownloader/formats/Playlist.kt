package com.saheed.videodownloader.formats

import org.json.JSONObject
import java.net.URI

/** A YouTube playlist: its title and the videos in it (at most [MAX_VIDEOS]). */
data class Playlist(
    val title: String,
    val entries: List<Entry>,
    /** How many videos the playlist really has; more than [entries] when it was cut to [MAX_VIDEOS]. */
    val totalCount: Int,
) {
    data class Entry(val url: String, val title: String, val thumbnailUrl: String?)

    val isCut: Boolean get() = totalCount > entries.size

    companion object {
        const val MAX_VIDEOS = 100

        /** Fixed choices for a playlist: the videos' own qualities differ, so these are "up to" limits. */
        val QUALITY_OPTIONS = listOf(
            QualityOption.BEST,
            QualityOption.forHeight("1080p", 1080, null),
            QualityOption.forHeight("720p", 720, null),
            QualityOption.forHeight("480p", 480, null),
            QualityOption.forHeight("360p", 360, null),
            QualityOption.MP3,
        )

        /**
         * True for a link to a whole YouTube playlist ("youtube.com/playlist?list=…").
         * A video opened from inside a playlist ("watch?v=…&list=…", "youtu.be/ID?list=…") is still
         * just that one video, as before.
         */
        fun isPlaylistUrl(url: String): Boolean {
            // java.net.URI rather than Android's Uri, so this also runs in plain unit tests.
            val uri = try { URI(url) } catch (e: Exception) { return false }
            val host = uri.host?.lowercase() ?: return false
            if (host != "youtube.com" && !host.endsWith(".youtube.com")) return false
            val params = uri.rawQuery.orEmpty().split('&').associate {
                it.substringBefore('=') to it.substringAfter('=', "")
            }
            return !params["list"].isNullOrBlank() && "v" !in params &&
                (uri.path == "/playlist" || uri.path.isNullOrEmpty() || uri.path == "/")
        }

        /**
         * Reads yt-dlp's `-J --flat-playlist` output: one JSON object with the playlist title and a
         * short entry per video. Private and deleted videos are skipped; they can't be downloaded.
         */
        fun parse(json: String): Playlist {
            val root = JSONObject(json)
            val array = root.optJSONArray("entries")
            val entries = mutableListOf<Entry>()
            var usable = 0
            for (i in 0 until (array?.length() ?: 0)) {
                val e = array!!.optJSONObject(i) ?: continue
                val title = e.optString("title").ifBlank { "video" }
                if (title == "[Private video]" || title == "[Deleted video]") continue
                val id = e.optString("id")
                val url = e.optString("url").ifBlank {
                    if (id.isBlank()) "" else "https://www.youtube.com/watch?v=$id"
                }
                if (url.isBlank()) continue
                usable++
                if (entries.size < MAX_VIDEOS) entries += Entry(url, title, thumbnailOf(e))
            }
            return Playlist(
                title = root.optString("title").ifBlank { "Playlist" },
                entries = entries,
                totalCount = usable,
            )
        }

        /** The last (biggest) thumbnail yt-dlp lists for an entry, if any. */
        private fun thumbnailOf(entry: JSONObject): String? {
            val thumbs = entry.optJSONArray("thumbnails") ?: return null
            if (thumbs.length() == 0) return null
            return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")?.ifBlank { null }
        }
    }
}
