package com.saheed.videodownloader.update

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Keeps yt-dlp fresh. Sites like YouTube change often, and the fix is almost always a newer yt-dlp.
 * - Automatic: when the app starts, update if the last update is more than 7 days old.
 * - Manual: the "Update downloader engine" button on the main screen.
 */
object EngineUpdater {

    private const val TAG = "EngineUpdater"
    private const val PREFS = "engine"
    private const val KEY_LAST_UPDATED = "lastUpdated"
    private val UPDATE_EVERY_MS = TimeUnit.DAYS.toMillis(7)

    /**
     * yt-dlp runs (lookups, downloads) take the read side; an update takes the write side,
     * so the yt-dlp file is never swapped while it's being used.
     */
    private val engineLock = ReentrantReadWriteLock()

    /** Run [block] (anything that calls yt-dlp) without clashing with an update. */
    fun <T> useEngine(block: () -> T): T = engineLock.read(block)

    // StateFlow = an observable value; the main screen watches it to show "Updating…".
    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating

    /** True when there's no record of an update or the last one is more than 7 days old. */
    fun isUpdateDue(lastUpdatedMillis: Long?, nowMillis: Long): Boolean =
        lastUpdatedMillis == null || nowMillis - lastUpdatedMillis > UPDATE_EVERY_MS

    fun lastUpdated(context: Context): Long? =
        prefs(context).getLong(KEY_LAST_UPDATED, 0L).takeIf { it > 0 }

    /** yt-dlp version, e.g. "2026.10.10", or null while still on the version bundled with the app. */
    fun version(context: Context): String? = YoutubeDL.getInstance().version(context)

    /**
     * Weekly auto-update, checked whenever the app is opened or a link is shared. A failed attempt
     * (e.g. no internet yet) saves no date, so the next open/share simply tries again.
     * Blocking; call from a background thread. Failures are only logged.
     * @Synchronized: if two screens ask at once, the second waits and then sees it's no longer due.
     */
    @Synchronized
    fun updateIfDue(context: Context) {
        if (!isUpdateDue(lastUpdated(context), System.currentTimeMillis())) return
        try {
            updateNow(context)
        } catch (e: Exception) {
            // Message included because Android leaves out the stack trace for network errors.
            Log.w(TAG, "Automatic engine update failed: $e", e)
        }
    }

    /** Updates now and returns a message for the user. Blocking; throws on failure. */
    fun updateNow(context: Context): String {
        _updating.value = true
        try {
            val status = engineLock.write {
                YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            }
            prefs(context).edit { putLong(KEY_LAST_UPDATED, System.currentTimeMillis()) }
            return when (status) {
                YoutubeDL.UpdateStatus.DONE -> "Updated to ${version(context)}"
                else -> "Already up to date"
            }
        } finally {
            _updating.value = false
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
