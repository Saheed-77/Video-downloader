package com.saheed.videodownloader.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saheed.videodownloader.ui.DownloadBlock
import com.saheed.videodownloader.ui.filterRecords
import com.saheed.videodownloader.ui.playlistSummary
import com.saheed.videodownloader.ui.toBlocks
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saheed.videodownloader.download.DownloadRecord
import com.saheed.videodownloader.download.DownloadStatus
import com.saheed.videodownloader.download.Progress
import com.saheed.videodownloader.formats.QualityOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Downloads: what's running (with progress and Cancel) and what's done (Open, Show in folder).
 * Playlists get their own section, built with the same shared code as on Android.
 */
@Composable
fun DownloadsScreen() {
    val records by DesktopDownloads.records.collectAsState()
    val progress by DesktopDownloads.progress.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Downloads", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            // Only the buttons that would do something right now.
            if (records.any { it.isActive || it.status == DownloadStatus.PAUSED }) {
                OutlinedButton(onClick = DesktopDownloads::cancelAll) { Text("Cancel all") }
            }
            if (records.any { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }) {
                OutlinedButton(onClick = DesktopDownloads::retryAll) { Text("Retry all") }
            }
            ClearMenu(records)
        }
        if (records.size > 5 || query.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search downloads") },
                singleLine = true,
                trailingIcon = if (query.isEmpty()) null else { { TextButton(onClick = { query = "" }) { Text("Clear") } } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val shown = filterRecords(records, query)
        when {
            records.isEmpty() -> Text(
                "Your downloads will show here. Paste a link on Home, or drop one onto this window.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            shown.isEmpty() -> Text("No downloads match \"${query.trim()}\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(toBlocks(shown), key = { it.key }) { block ->
                when (block) {
                    is DownloadBlock.Single -> DownloadCard(block.record, progress[block.record.id])
                    is DownloadBlock.Playlist -> PlaylistSection(block, progress)
                }
            }
        }
    }
}

/** A playlist: its name, a summary, buttons for the whole playlist, and its videos (click the name to hide them). */
@Composable
private fun PlaylistSection(playlist: DownloadBlock.Playlist, progress: Map<String, Progress>) {
    var expanded by remember(playlist.group) { mutableStateOf(true) }
    val videos = playlist.videos
    val anyActive = videos.any { it.isActive }
    val anyPaused = videos.any { it.status == DownloadStatus.PAUSED }
    val anyStopped = videos.any { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = !expanded }
                        .padding(4.dp)
                ) {
                    Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        playlistSummary(videos) + if (expanded) "" else " · click to show",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (anyActive) TextButton(onClick = { DesktopDownloads.stopGroup(playlist.group) }) { Text("Stop") }
                if (anyPaused) TextButton(onClick = { DesktopDownloads.resumeGroup(playlist.group) }) { Text("Resume") }
                if (anyActive || anyPaused) TextButton(onClick = { DesktopDownloads.cancelGroup(playlist.group) }) { Text("Cancel") }
                if (anyStopped) TextButton(onClick = { DesktopDownloads.retryGroup(playlist.group) }) { Text("Retry") }
                if (!anyActive) TextButton(onClick = { DesktopDownloads.removeGroup(playlist.group) }) { Text("Remove playlist") }
            }
            // key(): ties each card's state to its own video.
            if (expanded) videos.forEach { video -> key(video.id) { DownloadCard(video, progress[video.id]) } }
        }
    }
}

/** "Clear…": removes many rows at once. The files stay in the download folder. */
@Composable
private fun ClearMenu(records: List<DownloadRecord>) {
    val finished = setOf(DownloadStatus.DONE)
    val stopped = setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)
    val notRunning = finished + stopped + DownloadStatus.PAUSED
    val items = listOfNotNull(
        ("Clear finished" to finished).takeIf { records.any { it.status in finished } },
        ("Clear failed and cancelled" to stopped).takeIf { records.any { it.status in stopped } },
        ("Clear all except running" to notRunning).takeIf { records.any { it.status in notRunning } },
    )
    if (items.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("Clear…") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, statuses) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; DesktopDownloads.clear(statuses) })
            }
        }
    }
}

@Composable
private fun DownloadCard(record: DownloadRecord, progress: Progress?) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(width = 128.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (record.isAudio) Text("MP3", style = MaterialTheme.typography.labelLarge)
                else record.thumbnailUrl?.let {
                    AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.weight(1f)) {
                Text(record.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    statusText(record, progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Only the buttons that make sense for this download right now.
            when (record.status) {
                DownloadStatus.QUEUED, DownloadStatus.RUNNING ->
                    TextButton(onClick = { DesktopDownloads.cancel(record.id) }) { Text("Cancel") }
                DownloadStatus.DONE -> {
                    TextButton(onClick = { DesktopDownloads.open(record) }) { Text("Open") }
                    TextButton(onClick = { DesktopDownloads.showInFolder(record) }) { Text("Show in folder") }
                    TextButton(onClick = { DesktopDownloads.remove(record.id) }) { Text("Remove") }
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED, DownloadStatus.PAUSED -> {
                    TextButton(onClick = { DesktopDownloads.retry(record.id) }) { Text("Retry") }
                    TextButton(onClick = { DesktopDownloads.remove(record.id) }) { Text("Remove") }
                }
            }
        }
        if (record.isActive) {
            // No percent yet: a moving bar instead of a stuck one.
            if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

/** "720p · 46% · 3.4 MB/s · 0:12 left", "720p · 45 MB · 1 Nov, 18:30", or the error. */
private fun statusText(record: DownloadRecord, progress: Progress?): String {
    val parts = listOfNotNull(record.label, record.clip?.displayText) + when (record.status) {
        DownloadStatus.QUEUED -> listOf("Waiting…")
        DownloadStatus.RUNNING -> listOf(progress?.text ?: "Starting…")
        DownloadStatus.DONE -> listOfNotNull(
            record.sizeBytes?.let { QualityOption.formatSize(it) },
            dateFormat.format(Instant.ofEpochMilli(record.createdAt).atZone(ZoneId.systemDefault())),
        )
        DownloadStatus.FAILED -> listOf(record.error ?: "Failed")
        DownloadStatus.CANCELLED -> listOf("Cancelled")
        DownloadStatus.PAUSED -> listOf("Stopped")
    }
    return parts.joinToString(" · ")
}
