package com.saheed.videodownloader.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.saheed.videodownloader.R
import com.saheed.videodownloader.download.DownloadRecord
import com.saheed.videodownloader.download.DownloadStatus
import com.saheed.videodownloader.download.DownloadStore
import com.saheed.videodownloader.download.FileActions
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.share.PickerSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Downloads tab: running downloads with live progress, and past ones to open, share or retry. */
@Composable
fun DownloadsTab() {
    val context = LocalContext.current
    val records by DownloadStore.records.collectAsState()
    val progress by DownloadStore.progress.collectAsState()
    // The row whose quality is being changed; non-null while the quality sheet is open.
    var changing by remember { mutableStateOf<DownloadRecord?>(null) }

    // Once, when the tab opens: fix up rows left "Downloading" by a download the phone killed.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { DownloadStore.markInterrupted(context.applicationContext) }
    }

    if (records.isEmpty()) {
        EmptyState()
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Downloads", style = MaterialTheme.typography.headlineMedium)
            // Only shown when there's something to cancel or retry.
            val anyActive = records.any { it.isActive }
            val anyStopped = records.any { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
            if (anyActive || anyStopped) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (anyActive) {
                        OutlinedButton(onClick = { DownloadStore.cancelAll(context) }) { Text("Cancel all") }
                    }
                    if (anyStopped) {
                        OutlinedButton(onClick = { DownloadStore.retryAll(context) }) { Text("Retry all") }
                    }
                }
            }
        }
        // key: lets Compose keep each card's state (like its open menu) with the right row.
        items(records, key = { it.id }) { record ->
            DownloadCard(record, progress[record.id], onChangeQuality = { changing = record })
        }
    }

    changing?.let { record ->
        // Same sheet as Home and Share, starting with this download's subtitles and clip choices.
        PickerSheet(record.url, onDismiss = { changing = null }, initial = record.toExtras()) { _, meta, option, extras ->
            changing = null
            DownloadStore.changeQuality(context, record.id, meta, option, extras)
        }
    }
}

@Composable
private fun DownloadCard(record: DownloadRecord, percent: Int?, onChangeQuality: () -> Unit) {
    val context = LocalContext.current
    val uri = record.uri?.toUri()
    val mime = record.mime
    val canOpen = record.status == DownloadStatus.DONE && uri != null && mime != null

    // Tap a finished download to play it. (Card's own onClick would grey out the other cards.)
    Card(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = canOpen) { FileActions.open(context, uri!!, mime!!) }
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Thumbnail(record)
            Column(Modifier.weight(1f)) {
                Text(
                    record.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val failed = record.status == DownloadStatus.FAILED
                Text(
                    statusText(record, percent, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (record.status) {
                DownloadStatus.QUEUED, DownloadStatus.RUNNING -> {
                    IconButton(onClick = { DownloadStore.cancel(context, record.id) }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Cancel")
                    }
                    // Keeps downloading until a new quality is actually picked. Not for a waiting
                    // playlist video: stopping it would also stop the videos queued after it.
                    if (record.group == null) MoreMenu("Change quality" to onChangeQuality)
                }

                DownloadStatus.DONE -> {
                    if (canOpen) {
                        IconButton(onClick = { FileActions.share(context, uri!!, mime!!) }) {
                            Icon(painterResource(R.drawable.ic_share), contentDescription = "Share")
                        }
                    }
                    MoreMenu("Remove from list" to { DownloadStore.remove(record.id) })
                }

                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    TextButton(onClick = { DownloadStore.retry(context, record.id) }) { Text("Retry") }
                    MoreMenu(
                        "Change quality" to onChangeQuality,
                        "Remove from list" to { DownloadStore.remove(record.id) },
                    )
                }
            }
        }
        if (record.isActive) {
            // No percent yet (waiting or starting): a moving bar instead of a stuck one.
            if (percent == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Thumbnail(record: DownloadRecord) {
    Box(
        Modifier
            .size(width = 80.dp, height = 45.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (record.isAudio) R.drawable.ic_music else R.drawable.ic_movie),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        if (!record.isAudio && record.thumbnailUrl != null) {
            AsyncImage(
                model = record.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The "⋮" button and its menu of (label, action) items. `vararg` = any number of arguments,
 * like Java's `String...`. "Remove from list" leaves the file itself in Downloads.
 */
@Composable
private fun MoreMenu(vararg items: Pair<String, () -> Unit>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; action() })
            }
        }
    }
}

private fun statusText(record: DownloadRecord, percent: Int?, context: android.content.Context): String {
    // "720p · 1:30–2:45 · 46%" for a clip.
    val parts = listOfNotNull(record.label, record.clip?.displayText) + when (record.status) {
        DownloadStatus.QUEUED -> listOf("Waiting…")
        DownloadStatus.RUNNING -> listOf(percent?.let { "$it%" } ?: "Starting…")
        DownloadStatus.DONE -> listOfNotNull(
            record.sizeBytes?.let { QualityOption.formatSize(it) },
            DateUtils.formatDateTime(
                context, record.createdAt,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
            ),
        )
        DownloadStatus.FAILED -> listOf(record.error ?: "Failed")
        DownloadStatus.CANCELLED -> listOf("Cancelled")
    }
    return parts.joinToString(" · ")
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_download),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Your downloads will show here",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Share a video to Video Downloader, or paste a link on Home.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
