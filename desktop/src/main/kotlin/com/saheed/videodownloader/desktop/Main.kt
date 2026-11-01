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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    Surface(Modifier.fillMaxSize()) {
        Row {
            NavigationRail(Modifier.fillMaxHeight()) {
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Text(t.label.first().toString(), style = MaterialTheme.typography.titleMedium) },
                        label = { Text(t.label) },
                    )
                }
            }
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                Text("${tab.label} — coming in the next steps", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
