package com.saheed.videodownloader.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.saheed.videodownloader.share.UrlExtractor
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent

/** The tabs, shown down the left side (a *navigation rail*, the desktop version of bottom tabs). */
enum class Tab(val label: String) { HOME("Home"), DOWNLOADS("Downloads"), SETTINGS("Settings") }

/** The open tab, kept outside the screen so a dropped link can switch to Home. */
object Nav {
    var tab by mutableStateOf(Tab.HOME)
}

/** Starts the desktop app: one window with the tabs. `application { }` keeps it running until closed. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Video Downloader",
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
        DesktopTheme { App() }
    }
}

/** Material 3 colours, light or dark like Windows. (No wallpaper colours on desktop.) */
@Composable
fun DesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content)
}

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
