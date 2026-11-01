package com.saheed.videodownloader.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.formats.VideoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser

/** Settings: video format, where downloads go, and the yt-dlp engine. */
@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    var format by remember { mutableStateOf(DesktopSettings.videoFormat) }
    var folder by remember { mutableStateOf(DesktopSettings.downloadDir) }
    var version by remember { mutableStateOf(Engine.version()) }
    var updating by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        SettingsCard("Video format") {
            // No "Auto" on the PC: Windows can't reliably say whether it plays AV1 smoothly.
            listOf(VideoFormat.COMPATIBLE, VideoFormat.SMALLEST).forEach { choice ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = format == choice, onClick = { format = choice; DesktopSettings.videoFormat = choice })
                    Column {
                        Text(choice.label)
                        Text(choice.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SettingsCard("Download folder") {
            Text(folder.absolutePath)
            OutlinedButton(onClick = {
                // JFileChooser: Java's own "choose a folder" window.
                val chooser = JFileChooser(folder).apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    folder = chooser.selectedFile
                    DesktopSettings.downloadDir = chooser.selectedFile
                }
            }) { Text("Change…") }
        }

        SettingsCard("Downloader engine") {
            Text(
                "yt-dlp updates itself once a week. If downloads start failing, update it now.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Version: ${version ?: "not installed yet"}")
            OutlinedButton(enabled = !updating, onClick = {
                updating = true
                scope.launch {
                    updateMessage = withContext(Dispatchers.IO) {
                        runCatching { Engine.update() }.getOrElse { "Update failed: ${it.message}" }
                    }
                    version = withContext(Dispatchers.IO) { Engine.version() }
                    updating = false
                }
            }) { Text(if (updating) "Updating…" else "Update downloader engine") }
            updateMessage?.let { Text(it) }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
