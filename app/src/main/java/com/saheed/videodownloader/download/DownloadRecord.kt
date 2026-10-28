package com.saheed.videodownloader.download

import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.QualityOption
import org.json.JSONObject

enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

/**
 * One row of the Downloads tab. [id] is the WorkManager job's id.
 * The request fields ([url] … [isAudio]) are kept so a failed download can be retried.
 */
data class DownloadRecord(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val label: String,
    val format: String?,
    val isAudio: Boolean,
    val subtitles: Boolean = false,
    /** The clipped part in seconds, or both null for the whole video. */
    val clipStart: Int? = null,
    val clipEnd: Int? = null,
    /** Shared by the videos of one playlist download; null for a single video. */
    val group: String? = null,
    val status: DownloadStatus,
    val createdAt: Long,
    /** The saved file (content:// address), once DONE. Null on Android 7–9. */
    val uri: String? = null,
    val mime: String? = null,
    val sizeBytes: Long? = null,
    /** Friendly error message, when FAILED. */
    val error: String? = null,
) {
    val isActive: Boolean get() = status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING

    /** The quality row this download was started with (used by Retry). */
    fun toOption() = QualityOption(label, format, isAudio)

    val clip: ClipRange?
        get() = if (clipStart != null && clipEnd != null) ClipRange(clipStart, clipEnd) else null

    fun toExtras() = DownloadExtras(subtitles = subtitles, clip = clip)

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("url", url)
        .put("title", title)
        .put("thumbnailUrl", thumbnailUrl) // put(key, null) leaves the key out
        .put("label", label)
        .put("format", format)
        .put("isAudio", isAudio)
        .put("subtitles", subtitles)
        .put("clipStart", clipStart)
        .put("clipEnd", clipEnd)
        .put("group", group)
        .put("status", status.name)
        .put("createdAt", createdAt)
        .put("uri", uri)
        .put("mime", mime)
        .put("sizeBytes", sizeBytes)
        .put("error", error)

    companion object {
        fun fromJson(json: JSONObject) = DownloadRecord(
            id = json.getString("id"),
            url = json.getString("url"),
            title = json.getString("title"),
            thumbnailUrl = json.stringOrNull("thumbnailUrl"),
            label = json.getString("label"),
            format = json.stringOrNull("format"),
            isAudio = json.optBoolean("isAudio"),
            subtitles = json.optBoolean("subtitles"),
            clipStart = if (json.has("clipStart")) json.getInt("clipStart") else null,
            clipEnd = if (json.has("clipEnd")) json.getInt("clipEnd") else null,
            group = json.stringOrNull("group"),
            status = DownloadStatus.valueOf(json.getString("status")),
            createdAt = json.getLong("createdAt"),
            uri = json.stringOrNull("uri"),
            mime = json.stringOrNull("mime"),
            sizeBytes = if (json.has("sizeBytes")) json.getLong("sizeBytes") else null,
            error = json.stringOrNull("error"),
        )

        // An *extension function*: adds a method to JSONObject without subclassing it.
        private fun JSONObject.stringOrNull(key: String): String? =
            if (has(key) && !isNull(key)) getString(key) else null
    }
}
