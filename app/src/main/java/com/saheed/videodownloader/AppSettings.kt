package com.saheed.videodownloader

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.edit
import com.saheed.videodownloader.formats.DefaultQuality
import com.saheed.videodownloader.formats.VideoFormat

/** The app's settings (Settings tab), saved in SharedPreferences. */
object AppSettings {

    private const val PREFS = "settings"
    private const val KEY_WIFI_ONLY = "wifiOnly"
    private const val KEY_DEFAULT_QUALITY = "defaultQuality"
    private const val KEY_VIDEO_FORMAT = "videoFormat"

    /** Download only on Wi-Fi (an *unmetered* network); off by default. */
    fun wifiOnly(context: Context): Boolean = prefs(context).getBoolean(KEY_WIFI_ONLY, false)

    fun setWifiOnly(context: Context, on: Boolean) = prefs(context).edit { putBoolean(KEY_WIFI_ONLY, on) }

    /** What sharing a link does: ask for a quality (default), or start this one straight away. */
    fun defaultQuality(context: Context): DefaultQuality =
        prefs(context).getString(KEY_DEFAULT_QUALITY, null)
            ?.let { name -> DefaultQuality.entries.firstOrNull { it.name == name } }
            ?: DefaultQuality.ASK

    fun setDefaultQuality(context: Context, quality: DefaultQuality) =
        prefs(context).edit { putString(KEY_DEFAULT_QUALITY, quality.name) }

    /** Settings → "Video format"; Auto by default. */
    fun videoFormat(context: Context): VideoFormat =
        prefs(context).getString(KEY_VIDEO_FORMAT, null)
            ?.let { name -> VideoFormat.entries.firstOrNull { it.name == name } }
            ?: VideoFormat.AUTO

    fun setVideoFormat(context: Context, format: VideoFormat) =
        prefs(context).edit { putString(KEY_VIDEO_FORMAT, format.name) }

    /** The format downloads really use: "Auto" turned into Smallest or Most compatible for this phone. */
    fun effectiveVideoFormat(context: Context): VideoFormat = videoFormat(context).resolve(hasHardwareAv1)

    /**
     * Whether this phone has an AV1 *decoder* built into its chip (not done in software, which
     * stutters and drains the battery). Android only says so from Android 10; older phones count as no.
     * `by lazy`: worked out the first time it's needed, then remembered.
     */
    val hasHardwareAv1: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { codec ->
                !codec.isEncoder && codec.isHardwareAccelerated &&
                    codec.supportedTypes.any { it.equals("video/av01", ignoreCase = true) }
            }
    }

    /**
     * Why a download that hasn't started can't start right now, or null if the network is fine:
     * "Waiting for internet…" or, with Wi-Fi only on, "Waiting for Wi-Fi…".
     */
    fun networkWait(context: Context): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        return when {
            cm.activeNetwork == null -> "Waiting for internet…"
            // "Metered" = mobile data (or a hotspot marked as metered); Wi-Fi is unmetered.
            wifiOnly(context) && cm.isActiveNetworkMetered -> "Waiting for Wi-Fi…"
            else -> null
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
