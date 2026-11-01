package com.saheed.videodownloader.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import java.awt.Window

/**
 * Colours Windows' own title bar (the strip with minimise / maximise / close) to match the theme.
 * Call inside the theme, so it reads the current colours and repaints the bar when they change.
 */
@Composable
fun MatchTitleBar(window: Window) {
    val background = MaterialTheme.colorScheme.surface
    val text = MaterialTheme.colorScheme.onSurface
    LaunchedEffect(background, text) { TitleBar.paint(window, background, text) }
}

/**
 * Asks Windows' window manager (DWM, the Desktop Window Manager) to draw the title bar in our colours,
 * through `DwmSetWindowAttribute` in dwmapi.dll, called with JNA.
 * - Dark mode (Windows 10 20H1+): light buttons on a dark bar.
 * - Bar and title colours (Windows 11 only): ignored by Windows 10, which keeps its plain dark/light bar.
 * Does nothing on other systems, and any failure just leaves the normal bar.
 */
private object TitleBar {
    private const val USE_IMMERSIVE_DARK_MODE = 20
    private const val CAPTION_COLOR = 35
    private const val TEXT_COLOR = 36

    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    fun paint(window: Window, background: Color, text: Color) {
        if (!isWindows) return
        runCatching {
            val hwnd = Native.getWindowPointer(window)
            val setAttribute = Function.getFunction("dwmapi", "DwmSetWindowAttribute")
            fun set(attribute: Int, value: Int) {
                setAttribute.invokeInt(arrayOf(hwnd, attribute, IntByReference(value), 4))
            }
            set(USE_IMMERSIVE_DARK_MODE, if (background.luminance() < 0.5f) 1 else 0)
            set(CAPTION_COLOR, background.toColorRef())
            set(TEXT_COLOR, text.toColorRef())
        }
    }

    /** Windows colours are 0x00BBGGRR (blue first), the reverse of Compose's 0xAARRGGBB. */
    private fun Color.toColorRef(): Int {
        val argb = toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (b shl 16) or (g shl 8) or r
    }
}
