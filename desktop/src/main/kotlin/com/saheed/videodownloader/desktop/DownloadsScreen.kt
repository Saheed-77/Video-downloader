package com.saheed.videodownloader.desktop

import androidx.compose.foundation.background
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

/** Downloads: what's running (with progress and Cancel) and what's done (Open, Show in folder). */
@Composable
fun DownloadsScreen() {
    val records by DesktopDownloads.records.collectAsState()
    val progress by DesktopDownloads.progress.collectAsState()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium)
        if (records.isEmpty()) {
            Text(
                "Your downloads will show here. Paste a link on Home.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(records, key = { it.id }) { record -> DownloadCard(record, progress[record.id]) }
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
