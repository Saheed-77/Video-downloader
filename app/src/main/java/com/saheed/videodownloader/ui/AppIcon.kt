package com.saheed.videodownloader.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Switches the home-screen icon to match the theme. The manifest has two *activity aliases*
 * (extra "front doors" to MainActivity, each with its own icon); exactly one is turned on.
 */
object AppIcon {

    private const val PLAY_DOWN = "com.saheed.videodownloader.LauncherPlayDown"
    private const val MIDNIGHT = "com.saheed.videodownloader.LauncherMidnight"

    fun apply(context: Context, theme: ThemeChoice) {
        val on = if (theme == ThemeChoice.MIDNIGHT) MIDNIGHT else PLAY_DOWN
        val pm = context.packageManager
        listOf(PLAY_DOWN, MIDNIGHT).forEach { alias ->
            val state = if (alias == on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            // DONT_KILL_APP: keep the app open while the launcher swaps the icon.
            pm.setComponentEnabledSetting(ComponentName(context, alias), state, PackageManager.DONT_KILL_APP)
        }
    }
}
