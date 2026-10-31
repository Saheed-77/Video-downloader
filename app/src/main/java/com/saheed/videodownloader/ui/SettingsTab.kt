package com.saheed.videodownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.saheed.videodownloader.AppSettings
import com.saheed.videodownloader.formats.DefaultQuality
import com.saheed.videodownloader.update.EngineUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Settings tab: the downloader engine (yt-dlp) card and the app version. */
@Composable
fun SettingsTab() {
    val context = LocalContext.current
    // rememberCoroutineScope gives a scope tied to this screen, for starting work from a button tap.
    val scope = rememberCoroutineScope()
    var updateMessage by remember { mutableStateOf<String?>(null) }
    // collectAsState turns the updater's StateFlow into Compose state, so the screen redraws on change.
    val updating by EngineUpdater.updating.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        DownloadsCard()

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Downloader engine", style = MaterialTheme.typography.titleMedium)
                Text(
                    "yt-dlp updates itself once a week. If downloads start failing, update it now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Re-read after an update finishes (`updating` flips back to false → this block re-runs).
                val version = if (updating) null else EngineUpdater.version(context)
                val lastUpdated = if (updating) null else EngineUpdater.lastUpdated(context)
                Text("Version: " + if (updating) "…" else version ?: "built-in")
                Text(
                    "Last updated: " + when {
                        updating -> "…"
                        lastUpdated == null -> "never"
                        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(lastUpdated))
                    }
                )
                App.initError?.let {
                    Text("Engine error: $it", color = MaterialTheme.colorScheme.error)
                }
                FilledTonalButton(
                    enabled = !updating && App.initError == null,
                    onClick = {
                        updateMessage = null
                        scope.launch {
                            updateMessage = try {
                                withContext(Dispatchers.IO) { EngineUpdater.updateNow(context) }
                            } catch (e: Exception) {
                                "Update failed: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(if (updating) "Updating…" else "Update downloader engine") }
                updateMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                val appVersion = remember {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }
                Text("Video Downloader $appVersion")
                Text(
                    "For personal use. Downloads run on this phone with yt-dlp and ffmpeg.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Download settings: Wi-Fi only, and the default quality for sharing. */
@Composable
private fun DownloadsCard() {
    val context = LocalContext.current
    var wifiOnly by remember { mutableStateOf(AppSettings.wifiOnly(context)) }
    var defaultQuality by remember { mutableStateOf(AppSettings.defaultQuality(context)) }
    var menuOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Downloads", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Download only on Wi-Fi")
                    Text(
                        "Saves mobile data. New downloads wait until you're on Wi-Fi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // A *Switch* is Material's on/off toggle.
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it; AppSettings.setWifiOnly(context, it) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("When sharing a link")
                    Text(
                        "Pick a quality to download in one tap from the Share menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text(defaultQuality.label) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DefaultQuality.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label) },
                                onClick = {
                                    menuOpen = false
                                    defaultQuality = choice
                                    AppSettings.setDefaultQuality(context, choice)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
