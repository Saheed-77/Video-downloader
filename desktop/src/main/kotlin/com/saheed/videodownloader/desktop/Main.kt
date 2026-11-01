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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** The tabs, shown down the left side (a *navigation rail*, the desktop version of bottom tabs). */
enum class Tab(val label: String) { HOME("Home"), DOWNLOADS("Downloads"), SETTINGS("Settings") }

/** Starts the desktop app: one window with the tabs. `application { }` keeps it running until closed. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Video Downloader",
        state = rememberWindowState(size = DpSize(1000.dp, 720.dp)),
    ) {
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
    var tab by remember { mutableStateOf(Tab.HOME) }
    val records by DesktopDownloads.records.collectAsState()
    val active = records.count { it.isActive }
    Surface(Modifier.fillMaxSize()) {
        Row {
            NavigationRail(Modifier.fillMaxHeight()) {
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.label.first().toString(), style = MaterialTheme.typography.titleMedium) },
                        // The number of running downloads next to "Downloads".
                        label = { Text(if (t == Tab.DOWNLOADS && active > 0) "${t.label} ($active)" else t.label) },
                    )
                }
            }
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(
                        onPickVideo = { url, meta, option ->
                            DesktopDownloads.enqueue(url, meta.title, meta.thumbnailUrl, option)
                            tab = Tab.DOWNLOADS
                        },
                        onPickPlaylist = { playlist, option ->
                            DesktopDownloads.enqueuePlaylist(playlist, option)
                            tab = Tab.DOWNLOADS
                        },
                    )
                    Tab.DOWNLOADS -> DownloadsScreen()
                    Tab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
