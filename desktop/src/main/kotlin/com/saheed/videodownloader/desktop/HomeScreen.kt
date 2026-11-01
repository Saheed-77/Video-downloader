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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import com.saheed.videodownloader.share.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

/**
 * What the Home screen shows, kept outside the screen itself so it's still there after switching
 * tabs. `mutableStateOf` in a plain object still redraws the screen when a value changes.
 */
object HomeState {
    private val scope = MainScope()

    var text by mutableStateOf("")
    var engineStatus by mutableStateOf("Getting the downloader ready…")
    var engineReady by mutableStateOf(false)
    var lookingUp by mutableStateOf(false)
    var result by mutableStateOf<LookupResult?>(null)
    var lookedUpUrl by mutableStateOf("")
    var subtitles by mutableStateOf(DesktopSettings.subtitles)
    var clipOn by mutableStateOf(false)
    var clipStart by mutableStateOf("")
    var clipEnd by mutableStateOf("")
    private var preparing = false

    /** First start: download yt-dlp (and ffmpeg if this PC has none); weekly yt-dlp update. Runs once. */
    fun prepareEngine() {
        if (preparing) return
        preparing = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { Engine.prepare { engineStatus = it } }
                engineReady = true
            } catch (e: Exception) {
                engineStatus = "Couldn't get the downloader ready: ${e.message}. Check the internet and restart."
            }
        }
    }

    /** Looks up the link in the box (also used when a link is dropped onto the window). */
    fun find() {
        val url = UrlExtractor.extract(text) ?: run {
            result = LookupResult.Failed("No link found in that text")
            return
        }
        lookedUpUrl = url
        lookingUp = true
        result = null
        clipOn = false
        clipStart = ""
        clipEnd = ""
        scope.launch {
            result = withContext(Dispatchers.IO) { Lookup.lookup(url) }
            lookingUp = false
        }
    }
}

/**
 * Home: paste or drop a link, look it up, and pick a quality from what the video really has.
 * [onPickVideo] / [onPickPlaylist] start the download.
 */
@Composable
fun HomeScreen(
    onPickVideo: (url: String, meta: VideoMeta, option: QualityOption, extras: DownloadExtras) -> Unit,
    onPickPlaylist: (playlist: Playlist, option: QualityOption, extras: DownloadExtras) -> Unit,
) {
    val s = HomeState
    s.prepareEngine()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
            Text(
                "YouTube, Instagram, Facebook, Reddit, X and 1,700+ other sites. Paste a link, or drop one onto this window.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = s.text,
                onValueChange = { s.text = it },
                placeholder = { Text("Paste a video link") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { readClipboard()?.let { s.text = it } }) { Text("Paste") }
            Button(enabled = s.engineReady && s.text.isNotBlank() && !s.lookingUp, onClick = s::find) { Text("Find qualities") }
        }
        if (s.engineStatus.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!s.engineReady && !s.engineStatus.startsWith("Couldn't")) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(s.engineStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when (val r = s.result) {
            null -> if (s.lookingUp) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Text("Finding qualities…", style = MaterialTheme.typography.titleMedium)
            }
            is LookupResult.Failed -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(r.message, color = MaterialTheme.colorScheme.onErrorContainer)
                    if (s.lookedUpUrl.isNotEmpty()) TextButton(onClick = s::find) { Text("Try again") }
                }
            }
            is LookupResult.Video -> {
                val clipCheck = if (s.clipOn) ClipRange.check(s.clipStart, s.clipEnd, r.meta.durationSeconds) else null
                val clip = (clipCheck as? ClipRange.Result.Valid)?.range
                QualityPanel(
                    thumbnail = r.meta.thumbnailUrl,
                    pill = r.meta.durationText,
                    title = r.meta.title,
                    subtitle = r.meta.uploader,
                    options = r.options,
                    // Rows can't be picked while the clip times need fixing.
                    canPick = clipCheck !is ClipRange.Result.Invalid,
                    extrasControls = { VideoExtras(r.meta, clipCheck) },
                    onPick = { option ->
                        val extras = if (option.isAudio) DownloadExtras(clip = clip)
                        else DownloadExtras(subtitles = s.subtitles && clip == null, clip = clip)
                        onPickVideo(s.lookedUpUrl, r.meta, option, extras)
                    },
                )
            }
            is LookupResult.PlaylistFound -> QualityPanel(
                thumbnail = r.playlist.entries.first().thumbnailUrl,
                pill = "${r.playlist.entries.size} videos",
                title = r.playlist.title,
                subtitle = if (r.playlist.isCut) "Only the first ${r.playlist.entries.size} of ${r.playlist.totalCount} videos"
                else "Playlist · the videos download two at a time",
                options = Playlist.QUALITY_OPTIONS,
                canPick = true,
                // No Clip for playlists: one start/end time can't fit every video.
                extrasControls = { SubtitlesChip() },
                onPick = { option ->
                    onPickPlaylist(r.playlist, option, if (option.isAudio) DownloadExtras() else DownloadExtras(subtitles = s.subtitles))
                },
            )
        }
    }
}

/** Subtitles and Clip choices for one video (same rules as on Android). */
@Composable
private fun VideoExtras(meta: VideoMeta, clipCheck: ClipRange.Result?) {
    val s = HomeState
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Subtitles can't be cut to a clip, so the chip is greyed out while Clip is on.
            SubtitlesChip(enabled = !s.clipOn)
            FilterChip(selected = s.clipOn, onClick = { s.clipOn = !s.clipOn }, label = { Text("Clip") })
        }
        if (s.clipOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s.clipStart, onValueChange = { s.clipStart = it },
                    label = { Text("Start") }, placeholder = { Text("0:00") }, singleLine = true,
                    isError = clipCheck is ClipRange.Result.Invalid, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = s.clipEnd, onValueChange = { s.clipEnd = it },
                    label = { Text("End") }, placeholder = { Text(meta.durationText ?: "1:30") }, singleLine = true,
                    isError = clipCheck is ClipRange.Result.Invalid, modifier = Modifier.weight(1f),
                )
            }
            val error = (clipCheck as? ClipRange.Result.Invalid)?.message
            Text(
                error ?: "Subtitles aren't available for clips. Exact cuts take a little longer.",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (s.subtitles) {
            Text(
                "English subtitles are added to the video when it has them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubtitlesChip(enabled: Boolean = true) {
    val s = HomeState
    FilterChip(
        selected = s.subtitles && enabled,
        enabled = enabled,
        onClick = { s.subtitles = !s.subtitles; DesktopSettings.subtitles = s.subtitles },
        label = { Text("Subtitles") },
    )
}

/** Thumbnail, details and choices on the left; the quality rows on the right. */
@Composable
private fun QualityPanel(
    thumbnail: String?,
    pill: String?,
    title: String,
    subtitle: String?,
    options: List<QualityOption>,
    canPick: Boolean,
    extrasControls: @Composable () -> Unit,
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
            extrasControls()
        }
        LazyColumn(Modifier.weight(1f)) {
            items(options) { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (canPick) 1f else 0.38f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canPick) { onPick(option) }
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
