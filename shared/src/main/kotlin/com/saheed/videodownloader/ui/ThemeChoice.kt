package com.saheed.videodownloader.ui

/**
 * Settings → "Theme": the app's colours and its icon, picked together.
 * WALLPAPER (colours from the phone's wallpaper) is Android-only and uses the Play-down icon.
 */
enum class ThemeChoice(val label: String, val description: String) {
    PLAY_DOWN("Play-down", "Violet and pink, light or dark like your phone."),
    MIDNIGHT("Midnight", "Dark navy with lime and cyan, always dark."),
    WALLPAPER("Wallpaper colours", "Colours from your wallpaper, light or dark like your phone.");

    companion object {
        /** The saved choice, or [default] when nothing (or something unknown) is saved. */
        fun fromName(name: String?, default: ThemeChoice = PLAY_DOWN): ThemeChoice =
            entries.firstOrNull { it.name == name } ?: default
    }
}
