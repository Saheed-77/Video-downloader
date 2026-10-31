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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.saheed.videodownloader.download.DownloadWorker
import com.saheed.videodownloader.download.FileActions
import com.saheed.videodownloader.download.Progress
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
    // Non-null while the quality sheet is open for a row ("Change quality" or "Download another quality").
    var picking by remember { mutableStateOf<QualityPick?>(null) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Downloads", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                ClearMenu(records)
            }
            // Only shown when there's something to cancel or retry.
            val anyActive = records.any { it.isActive || it.status == DownloadStatus.PAUSED }
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
        // key: lets Compose keep each block's state (an open menu, a hidden section) with the right one.
        items(toBlocks(records), key = { it.key }) { block ->
            // One way to show a video card, used for single videos and inside playlist sections.
            val card: @Composable (DownloadRecord) -> Unit = { record ->
                SwipeToRemove(record) {
                    DownloadCard(
                        record,
                        progress[record.id],
                        onChangeQuality = { picking = QualityPick(record, replaceOld = true) },
                        onAnotherQuality = { picking = QualityPick(record, replaceOld = false) },
                    )
                }
            }
            when (block) {
                is DownloadBlock.Single -> card(block.record)
                is DownloadBlock.Playlist -> PlaylistSection(block, card)
            }
        }
    }

    picking?.let { (record, replaceOld) ->
        // Same sheet as Home and Share, starting with this download's subtitles and clip choices.
        PickerSheet(record.url, onDismiss = { picking = null }, initial = record.toExtras()) { _, meta, option, extras ->
            picking = null
            if (replaceOld) {
                DownloadStore.changeQuality(context, record.id, meta, option, extras)
            } else {
                // A separate download next to the old one, which keeps going (or stays saved).
                DownloadWorker.enqueue(context, record.url, meta.title, meta.thumbnailUrl ?: record.thumbnailUrl, option, extras)
            }
        }
    }
}

/** Which row the quality sheet is for, and whether the pick replaces it or is added next to it. */
private data class QualityPick(val record: DownloadRecord, val replaceOld: Boolean)

@Composable
private fun DownloadCard(
    record: DownloadRecord,
    progress: Progress?,
    onChangeQuality: () -> Unit,
    onAnotherQuality: () -> Unit,
) {
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
                    statusText(record, progress, context),
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
                    // Keeps downloading until a new quality is actually picked.
                    MoreMenu("Change quality" to onChangeQuality, "Download another quality" to onAnotherQuality)
                }

                DownloadStatus.DONE -> {
                    if (canOpen) {
                        IconButton(onClick = { FileActions.share(context, uri!!, mime!!) }) {
                            Icon(painterResource(R.drawable.ic_share), contentDescription = "Share")
                        }
                    }
                    MoreMenu(
                        "Download another quality" to onAnotherQuality,
                        "Remove from list" to { DownloadStore.remove(record.id) },
                    )
                }

                // Stopped with its playlist's Stop button: the section's Resume starts it again.
                DownloadStatus.PAUSED -> MoreMenu("Remove from list" to { DownloadStore.remove(record.id) })

                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    TextButton(onClick = { DownloadStore.retry(context, record.id) }) { Text("Retry") }
                    MoreMenu(
                        "Change quality" to onChangeQuality,
                        "Download another quality" to onAnotherQuality,
                        "Remove from list" to { DownloadStore.remove(record.id) },
                    )
                }
            }
        }
        if (record.isActive) {
            // No percent yet (waiting or starting): a moving bar instead of a stuck one.
            if (progress == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * A playlist's section: its name, a summary, buttons for the whole playlist, and its videos.
 * Tapping the name hides or shows the videos.
 */
@Composable
private fun PlaylistSection(playlist: DownloadBlock.Playlist, card: @Composable (DownloadRecord) -> Unit) {
    val context = LocalContext.current
    val videos = playlist.videos
    // rememberSaveable keyed by the playlist, so each section remembers if it's hidden.
    var expanded by rememberSaveable(playlist.group) { mutableStateOf(true) }
    val anyActive = videos.any { it.isActive }
    val anyPaused = videos.any { it.status == DownloadStatus.PAUSED }
    val anyStopped = videos.any { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { expanded = !expanded }
                        .padding(4.dp)
                ) {
                    Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        playlistSummary(videos) + if (expanded) "" else " · tap to show",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!anyActive) {
                    MoreMenu("Remove playlist from list" to { DownloadStore.removeGroup(playlist.group) })
                }
            }
            // Only the buttons that would do something right now.
            if (anyActive || anyPaused || anyStopped) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (anyActive) TextButton(onClick = { DownloadStore.stopGroup(context, playlist.group) }) { Text("Stop") }
                    if (anyPaused) TextButton(onClick = { DownloadStore.resumeGroup(context, playlist.group) }) { Text("Resume") }
                    if (anyActive || anyPaused) {
                        TextButton(onClick = { DownloadStore.cancelGroup(context, playlist.group) }) { Text("Cancel") }
                    }
                    if (anyStopped) TextButton(onClick = { DownloadStore.retryGroup(context, playlist.group) }) { Text("Retry") }
                }
            }
            // key(): ties each card's state (like a half-done swipe) to its own video, so removing
            // one doesn't hand its swiped-away state to the card below.
            if (expanded) videos.forEach { video -> key(video.id) { card(video) } }
        }
    }
}

/**
 * Lets a finished, failed, cancelled or stopped card be swiped left to remove it from the list.
 * A *SwipeToDismissBox* is a row you can drag sideways; the red "Remove" shows underneath.
 * Running and waiting cards can't be swiped, so nothing is removed mid-download.
 */
@Composable
private fun SwipeToRemove(record: DownloadRecord, content: @Composable () -> Unit) {
    if (record.isActive) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) DownloadStore.remove(record.id)
            value == SwipeToDismissBoxValue.EndToStart
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Remove", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { content() }
}

/**
 * The ⋮ next to "Downloads": clears many rows at once. Each item shows only when it would remove
 * something. The files themselves stay in the phone's Downloads folder.
 */
@Composable
private fun ClearMenu(records: List<DownloadRecord>) {
    val finished = setOf(DownloadStatus.DONE)
    val stopped = setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED)
    val notRunning = finished + stopped + DownloadStatus.PAUSED
    val items = listOfNotNull(
        ("Clear finished" to { DownloadStore.clear(finished) }).takeIf { records.any { it.status in finished } },
        ("Clear failed and cancelled" to { DownloadStore.clear(stopped) }).takeIf { records.any { it.status in stopped } },
        ("Clear all except running" to { DownloadStore.clear(notRunning) }).takeIf { records.any { it.status in notRunning } },
    )
    // `*` spreads the list into MoreMenu's vararg, like passing an array to Java's `String...`.
    if (items.isNotEmpty()) MoreMenu(*items.toTypedArray())
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

private fun statusText(record: DownloadRecord, progress: Progress?, context: android.content.Context): String {
    // "720p · 1:30–2:45 · 46% · 3.4 MB/s · 0:12 left" for a running clip.
    val parts = listOfNotNull(record.label, record.clip?.displayText) + when (record.status) {
        DownloadStatus.QUEUED -> listOf("Waiting…")
        DownloadStatus.RUNNING -> listOf(progress?.text ?: "Starting…")
        DownloadStatus.DONE -> listOfNotNull(
            record.sizeBytes?.let { QualityOption.formatSize(it) },
            DateUtils.formatDateTime(
                context, record.createdAt,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
            ),
        )
        DownloadStatus.FAILED -> listOf(record.error ?: "Failed")
        DownloadStatus.CANCELLED -> listOf("Cancelled")
        DownloadStatus.PAUSED -> listOf("Stopped")
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
