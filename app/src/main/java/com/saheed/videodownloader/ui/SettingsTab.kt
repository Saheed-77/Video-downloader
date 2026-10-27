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
