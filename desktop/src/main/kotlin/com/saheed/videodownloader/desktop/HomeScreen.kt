package com.saheed.videodownloader.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import com.saheed.videodownloader.share.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * Home: paste a link, look it up, and pick a quality from what the video really has.
 * [onPickVideo] / [onPickPlaylist] start the download.
 */
@Composable
fun HomeScreen(
    onPickVideo: (url: String, meta: VideoMeta, option: QualityOption) -> Unit,
    onPickPlaylist: (playlist: Playlist, option: QualityOption) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var engineStatus by remember { mutableStateOf("Getting the downloader ready…") }
    var engineReady by remember { mutableStateOf(false) }
    var lookingUp by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<LookupResult?>(null) }
    var lookedUpUrl by remember { mutableStateOf("") }

    // First start: download yt-dlp (and ffmpeg if this PC has none); weekly yt-dlp update.
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) { Engine.prepare { engineStatus = it } }
            engineReady = true
        } catch (e: Exception) {
            engineStatus = "Couldn't get the downloader ready: ${e.message}. Check the internet and restart."
        }
    }

    fun find() {
        val url = UrlExtractor.extract(text) ?: run {
            result = LookupResult.Failed("No link found in that text")
            return
        }
        lookedUpUrl = url
        lookingUp = true
        result = null
        scope.launch {
            result = withContext(Dispatchers.IO) { Lookup.lookup(url) }
            lookingUp = false
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
            Text(
                "YouTube, Instagram, Facebook, Reddit, X and 1,700+ other sites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Paste a video link") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { readClipboard()?.let { text = it } }) { Text("Paste") }
            Button(enabled = engineReady && text.isNotBlank() && !lookingUp, onClick = ::find) { Text("Find qualities") }
        }
        if (engineStatus.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!engineReady && !engineStatus.startsWith("Couldn't")) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(engineStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val r = result) {
            null -> if (lookingUp) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Finding qualities…", style = MaterialTheme.typography.titleMedium)
            }
            is LookupResult.Failed -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(r.message, color = MaterialTheme.colorScheme.onErrorContainer)
                    if (lookedUpUrl.isNotEmpty()) TextButton(onClick = ::find) { Text("Try again") }
                }
            }
            is LookupResult.Video -> QualityPanel(
                thumbnail = r.meta.thumbnailUrl,
                pill = r.meta.durationText,
                title = r.meta.title,
                subtitle = r.meta.uploader,
                options = r.options,
                onPick = { onPickVideo(lookedUpUrl, r.meta, it) },
            )
            is LookupResult.PlaylistFound -> QualityPanel(
                thumbnail = r.playlist.entries.first().thumbnailUrl,
                pill = "${r.playlist.entries.size} videos",
                title = r.playlist.title,
                subtitle = if (r.playlist.isCut) "Only the first ${r.playlist.entries.size} of ${r.playlist.totalCount} videos"
                else "Playlist · the videos download two at a time",
                options = Playlist.QUALITY_OPTIONS,
                onPick = { onPickPlaylist(r.playlist, it) },
            )
        }
    }
}

/** Thumbnail and details on the left, the quality rows on the right. */
@Composable
private fun QualityPanel(
    thumbnail: String?,
    pill: String?,
    title: String,
    subtitle: String?,
    options: List<QualityOption>,
    onPick: (QualityOption) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.width(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(width = 360.dp, height = 202.dp),
            ) {
                if (thumbnail != null) {
                    AsyncImage(model = thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                pill?.let {
                    Text(
                        it,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(options) { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(option) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Badge(option.badge ?: "MP3", highlight = option.badge == "Best")
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    option.sizeBytes?.let { Text(QualityOption.formatSize(it)) }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, highlight: Boolean) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelLarge) }
}

/** The text on the Windows clipboard, or null. */
private fun readClipboard(): String? = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
}.getOrNull()
