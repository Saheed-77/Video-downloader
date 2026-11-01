package com.saheed.videodownloader.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.saheed.videodownloader.share.UrlExtractor
import com.saheed.videodownloader.ui.ThemeChoice
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import javax.imageio.ImageIO

/** The tabs, shown down the left side (a *navigation rail*, the desktop version of bottom tabs). */
enum class Tab(val label: String) { HOME("Home"), DOWNLOADS("Downloads"), SETTINGS("Settings") }

/** The open tab, kept outside the screen so a dropped link can switch to Home. */
object Nav {
    var tab by mutableStateOf(Tab.HOME)
}

/** The theme picked in Settings, as Compose state so a change recolours the window at once. */
object ThemeState {
    var choice by mutableStateOf(DesktopSettings.theme)
}

/** The window and taskbar icons, one per theme, loaded once from the app's resources. */
private val icons: Map<ThemeChoice, BitmapPainter> = mapOf(
    ThemeChoice.PLAY_DOWN to "icon-playdown.png",
    ThemeChoice.MIDNIGHT to "icon-midnight.png",
).mapValues { (_, file) ->
    val image = ImageIO.read(Thread.currentThread().contextClassLoader.getResource(file))
    BitmapPainter(image.toComposeImageBitmap())
}

/** Starts the desktop app: one window with the tabs. `application { }` keeps it running until closed. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Video Downloader",
        icon = icons[ThemeState.choice] ?: icons.getValue(ThemeChoice.PLAY_DOWN),
        state = rememberWindowState(size = DpSize(1000.dp, 720.dp)),
    ) {
        // A link dragged onto the window (from a browser's address bar or a web page) is looked up on Home.
        // DropTarget is Java's own drag-and-drop receiver; `window` is this app's window.
        LaunchedEffect(Unit) {
            window.dropTarget = object : DropTarget() {
                override fun drop(event: DropTargetDropEvent) {
                    event.acceptDrop(DnDConstants.ACTION_COPY)
                    val text = runCatching { event.transferable.getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
                    event.dropComplete(text != null)
                    if (text != null && UrlExtractor.extract(text) != null) {
                        HomeState.text = text.trim()
                        Nav.tab = Tab.HOME
                        if (HomeState.engineReady) HomeState.find()
                    }
                }
            }
        }
        DesktopTheme {
            MatchTitleBar(window)
            App()
        }
    }
}

/**
 * Colours from Settings → Theme: Play-down (violet and pink, light or dark like Windows) or
 * Midnight (dark navy with lime and cyan, always dark). The same colours as the Android app.
 */
@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    val colors = when {
        ThemeState.choice == ThemeChoice.MIDNIGHT -> MidnightColors
        isSystemInDarkTheme() -> PlayDownDark
        else -> PlayDownLight
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

@Composable
fun App() {
    val records by DesktopDownloads.records.collectAsState()
    val active = records.count { it.isActive }
    Surface(Modifier.fillMaxSize()) {
        Row {
            NavigationRail(Modifier.fillMaxHeight()) {
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = Nav.tab == t,
                        onClick = { Nav.tab = t },
                        icon = { Text(t.label.first().toString(), style = MaterialTheme.typography.titleMedium) },
                        // The number of running downloads next to "Downloads".
                        label = { Text(if (t == Tab.DOWNLOADS && active > 0) "${t.label} ($active)" else t.label) },
                    )
                }
            }
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                when (Nav.tab) {
                    Tab.HOME -> HomeScreen(
                        onPickVideo = { url, meta, option, extras ->
                            DesktopDownloads.enqueue(url, meta.title, meta.thumbnailUrl, option, extras)
                            Nav.tab = Tab.DOWNLOADS
                        },
                        onPickPlaylist = { playlist, option, extras ->
                            DesktopDownloads.enqueuePlaylist(playlist, option, extras)
                            Nav.tab = Tab.DOWNLOADS
                        },
                    )
                    Tab.DOWNLOADS -> DownloadsScreen()
                    Tab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
