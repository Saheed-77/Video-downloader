package com.saheed.videodownloader.formats

/**
 * Settings → "Video format": which *codec* (the way the picture is compressed) to prefer.
 * The same picture is about 30% smaller as AV1 than VP9, and about half the size of H.264,
 * but only H.264 plays on every phone, TV and WhatsApp.
 */
enum class VideoFormat(val label: String, val description: String) {
    AUTO("Auto for this phone", "Smallest files when this phone can play them smoothly, otherwise most compatible."),
    SMALLEST("Smallest files", "AV1 or VP9: about half the size of H.264. Needs a newer phone, TV or iPhone 15 Pro+."),
    COMPATIBLE("Most compatible", "H.264: plays on every phone, TV and WhatsApp. Files are bigger.");

    /** The choice actually used: AUTO becomes SMALLEST on phones that decode AV1 in hardware. */
    fun resolve(hasHardwareAv1: Boolean): VideoFormat = when {
        this != AUTO -> this
        hasHardwareAv1 -> SMALLEST
        else -> COMPATIBLE
    }

    /**
     * yt-dlp's *format sort* (`-S`). Resolution first, so the picked quality is kept; then the codec.
     * `vcodec:av01` means "the best codec up to AV1": AV1, else VP9, else H.264.
     */
    val sort: String
        get() = if (this == SMALLEST) "res,vcodec:av01,acodec:opus" else "res,vcodec:h264,acodec:m4a"

    /** How well a format's video codec fits this choice: 0 is the preferred one. Used for picker sizes. */
    fun codecRank(vcodec: String?): Int {
        val c = vcodec.orEmpty().lowercase()
        val order = if (this == SMALLEST) listOf("av01", "vp9|vp09", "avc|h264") else listOf("avc|h264", "vp9|vp09", "av01")
        return order.indexOfFirst { names -> names.split('|').any { c.startsWith(it) } }.let { if (it < 0) order.size else it }
    }
}
