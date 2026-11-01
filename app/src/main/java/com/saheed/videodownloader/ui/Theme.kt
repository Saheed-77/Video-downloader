package com.saheed.videodownloader.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The theme picked in Settings, kept as Compose state so changing it recolours every open screen
 * at once. Loaded from the settings when the app starts (see `App`).
 */
object ThemeState {
    var choice by mutableStateOf(ThemeChoice.PLAY_DOWN)
}

/**
 * App-wide colours, from Settings → Theme:
 * - Play-down: violet and pink, light or dark like the phone.
 * - Midnight: dark navy with lime and cyan, always dark.
 * - Wallpaper colours: "dynamic colour" from the wallpaper on Android 12+ (older phones get Play-down).
 *
 * [systemBars]: also match the status-bar icons to the colours (light icons on dark screens).
 * The share sheet passes false, because the app you shared from shows behind its status bar.
 */
@Composable
fun AppTheme(systemBars: Boolean = true, content: @Composable () -> Unit) {
    val choice = ThemeState.choice
    val dark = choice == ThemeChoice.MIDNIGHT || isSystemInDarkTheme()
    val colors = when {
        choice == ThemeChoice.MIDNIGHT -> MidnightColors
        choice == ThemeChoice.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> PlayDownDark
        else -> PlayDownLight
    }
    val view = LocalView.current
    if (systemBars && !view.isInEditMode) {
        // SideEffect runs after each redraw: a place to change things outside Compose, like the window.
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private val PlayDownLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF7B2FF7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF25005A),
    secondary = Color(0xFFB0127F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8EC),
    onSecondaryContainer = Color(0xFF3D0029),
    tertiary = Color(0xFFD1007F),
    onTertiary = Color.White,
)

private val PlayDownDark: ColorScheme = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF3B0091),
    primaryContainer = Color(0xFF5A16C9),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFFFAFD6),
    onSecondary = Color(0xFF5E1146),
    secondaryContainer = Color(0xFF7D2B60),
    onSecondaryContainer = Color(0xFFFFD8EC),
    tertiary = Color(0xFFFF8AC8),
    onTertiary = Color(0xFF5E1146),
)

private val Navy = Color(0xFF0F172A)

private val MidnightColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFA3E635),
    onPrimary = Color(0xFF1A2E05),
    primaryContainer = Color(0xFF365314),
    onPrimaryContainer = Color(0xFFD9F99D),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF083344),
    background = Navy,
    onBackground = Color(0xFFE2E8F0),
    surface = Navy,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B1120),
    surfaceContainerLow = Color(0xFF131C31),
    surfaceContainer = Color(0xFF172036),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF273449),
    surfaceBright = Color(0xFF273449),
    surfaceDim = Navy,
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Navy,
    inversePrimary = Color(0xFF4D7C0F),
    surfaceTint = Color(0xFFA3E635),
)
