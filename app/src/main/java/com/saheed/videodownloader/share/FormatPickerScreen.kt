package com.saheed.videodownloader.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil3.compose.AsyncImage
import com.saheed.videodownloader.App
import com.saheed.videodownloader.R
import com.saheed.videodownloader.download.ErrorMapper
import com.saheed.videodownloader.formats.ClipRange
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.FormatParser
import com.saheed.videodownloader.formats.Playlist
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The things this screen can show. A `sealed interface` is a closed set of types,
 * so `when` below must handle each one (like a Java sealed interface + switch).
 */
private sealed interface PickerState {
    data object Loading : PickerState
    data class Loaded(val meta: VideoMeta, val options: List<QualityOption>) : PickerState
    data class PlaylistLoaded(val playlist: Playlist) : PickerState
    data class Failed(val message: String) : PickerState
}

private val thumbnailShape = RoundedCornerShape(12.dp)
private const val PREFS = "picker"
private const val KEY_SUBTITLES = "subtitles"

/**
 * Looks up [url] with yt-dlp, shows the video's thumbnail and details, and lists the qualities
 * it really has. Tapping a row calls [onPick] with the video's details, the chosen quality and the extras.
 * [initial]: the chips and clip times to start with (used by "Change quality").
 * A whole-playlist link shows the playlist instead, and a tap calls [onPickPlaylist].
 */
@Composable
fun FormatPicker(
    url: String,
    initial: DownloadExtras? = null,
    onPickPlaylist: (playlist: Playlist, option: QualityOption, extras: DownloadExtras) -> Unit = { _, _, _ -> },
    onPick: (meta: VideoMeta, option: QualityOption, extras: DownloadExtras) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    // The Subtitles chip remembers its last setting (saved in SharedPreferences).
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var subtitles by remember { mutableStateOf(initial?.subtitles ?: prefs.getBoolean(KEY_SUBTITLES, false)) }
    // The clip is per video, so it resets for a new link.
    val initialClip = initial?.clip
    var clipOn by remember(url) { mutableStateOf(initialClip != null) }
    var clipStart by remember(url) { mutableStateOf(initialClip?.let { ClipRange.formatTime(it.startSeconds) } ?: "") }
    var clipEnd by remember(url) { mutableStateOf(initialClip?.let { ClipRange.formatTime(it.endSeconds) } ?: "") }
    var state by remember(url) { mutableStateOf<PickerState>(PickerState.Loading) }
    // Bumped by "Try again" so the lookup below runs once more.
    var attempt by remember(url) { mutableIntStateOf(0) }

    // Re-runs whenever `url` or `attempt` changes; the work runs in the background (Dispatchers.IO).
    LaunchedEffect(url, attempt) {
        App.initError?.let {
            state = PickerState.Failed(it)
            return@LaunchedEffect
        }
        state = PickerState.Loading
        state = if (Playlist.isPlaylistUrl(url)) try {
            val playlist = withContext(Dispatchers.IO) {
                EngineUpdater.updateIfDue(context)
                // -J --flat-playlist: just the list of videos as JSON, without looking each one up.
                val request = YoutubeDLRequest(url).addOption("-J").addOption("--flat-playlist")
                val json = EngineUpdater.useEngine { YoutubeDL.getInstance().execute(request).out }
                Playlist.parse(json)
            }
            if (playlist.entries.isEmpty()) PickerState.Failed("This playlist has no videos that can be downloaded.")
            else PickerState.PlaylistLoaded(playlist)
        } catch (e: Exception) {
            PickerState.Failed(ErrorMapper.toMessage(e.message))
        } else try {
            val info = withContext(Dispatchers.IO) {
                // Weekly yt-dlp update check before every lookup (covers links shared to the app).
                // Runs first, so the lookup below already uses the new engine.
                EngineUpdater.updateIfDue(context)
                // --no-playlist: a link opened from inside a playlist describes just that one video.
                val request = YoutubeDLRequest(url).addOption("--no-playlist")
                EngineUpdater.useEngine { YoutubeDL.getInstance().getInfo(request) }
            }
            PickerState.Loaded(VideoMeta.fromVideoInfo(info), FormatParser.fromVideoInfo(info))
        } catch (e: Exception) {
            PickerState.Failed(ErrorMapper.toMessage(e.message))
        }
    }

    when (val s = state) {
        PickerState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // A grey "skeleton" in the thumbnail's place, so the sheet doesn't jump when it loads.
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(thumbnailShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            Text("Finding qualities…", style = MaterialTheme.typography.titleMedium)
        }

        is PickerState.Failed -> ErrorCard(s.message, onRetry = { attempt++ })

        is PickerState.PlaylistLoaded -> {
            val (audio, video) = Playlist.QUALITY_OPTIONS.partition { it.isAudio }
            LazyColumn {
                item { PlaylistHeader(s.playlist) }
                item {
                    // No Clip for playlists: one start/end time can't fit every video.
                    Row(Modifier.padding(top = 12.dp)) {
                        OptionChip("Subtitles", R.drawable.ic_subtitles, subtitles) {
                            subtitles = it
                            prefs.edit { putBoolean(KEY_SUBTITLES, it) }
                        }
                    }
                    if (subtitles) Hint("English subtitles are added to each video that has them.")
                }
                item { SectionLabel("Video (up to)") }
                items(video) { option ->
                    QualityRow(option, enabled = true) { onPickPlaylist(s.playlist, option, DownloadExtras(subtitles)) }
                }
                item { SectionLabel("Audio") }
                items(audio) { option ->
                    QualityRow(option, enabled = true) { onPickPlaylist(s.playlist, option, DownloadExtras()) }
                }
            }
        }

        is PickerState.Loaded -> {
            val (audio, video) = s.options.partition { it.isAudio }
            val clipCheck = if (clipOn) ClipRange.check(clipStart, clipEnd, s.meta.durationSeconds) else null
            val clip = (clipCheck as? ClipRange.Result.Valid)?.range
            // Quality rows can't be tapped while the clip times need fixing.
            val canPick = clipCheck !is ClipRange.Result.Invalid
            // LazyColumn = a scrolling list that only builds the rows on screen (like RecyclerView).
            // The header is its first item, so everything scrolls together.
            LazyColumn {
                item { VideoHeader(s.meta) }
                item {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Subtitles can't be cut to a clip (they'd start from 0:00 of the full video),
                        // so the chip is greyed out while Clip is on.
                        OptionChip("Subtitles", R.drawable.ic_subtitles, subtitles && !clipOn, enabled = !clipOn) {
                            subtitles = it
                            prefs.edit { putBoolean(KEY_SUBTITLES, it) }
                        }
                        OptionChip("Clip", R.drawable.ic_cut, clipOn) { clipOn = it }
                    }
                    if (clipOn) Hint("Subtitles aren't available for clips.")
                    else if (subtitles) Hint("English subtitles are added to the video when it has them.")
                    if (clipOn) {
                        ClipFields(
                            start = clipStart,
                            end = clipEnd,
                            endPlaceholder = s.meta.durationText ?: "1:30",
                            error = (clipCheck as? ClipRange.Result.Invalid)?.message,
                            onStart = { clipStart = it },
                            onEnd = { clipEnd = it },
                        )
                    }
                }
                item { SectionLabel("Video") }
                items(video) { option ->
                    QualityRow(option, canPick) { onPick(s.meta, option, DownloadExtras(subtitles && clip == null, clip)) }
                }
                if (audio.isNotEmpty()) {
                    item { SectionLabel("Audio") }
                    items(audio) { option ->
                        QualityRow(option, canPick) { onPick(s.meta, option, DownloadExtras(clip = clip)) }
                    }
                }
            }
        }
    }
}

/** 16:9 rounded thumbnail (film icon until it loads) with an optional pill in the corner. */
@Composable
private fun Thumbnail(url: String?, pill: String?) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(thumbnailShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        // Film icon underneath; the thumbnail covers it once Coil has downloaded it.
        Icon(
            painterResource(R.drawable.ic_movie),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        pill?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** First video's thumbnail with a "24 videos" pill, the playlist title, and how it downloads. */
@Composable
private fun PlaylistHeader(playlist: Playlist) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Thumbnail(playlist.entries.first().thumbnailUrl, pill = "${playlist.entries.size} videos")
        Spacer(Modifier.size(4.dp))
        Text(playlist.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (playlist.isCut) "Only the first ${playlist.entries.size} of ${playlist.totalCount} videos. They download one after another."
            else "Playlist. The videos download one after another.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VideoHeader(meta: VideoMeta) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Thumbnail(meta.thumbnailUrl, pill = meta.durationText)
        Spacer(Modifier.size(4.dp))
        Text(
            meta.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        meta.uploader?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** An on/off *filter chip* (the small rounded toggle button) with an icon. */
@Composable
private fun OptionChip(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = { onChange(!selected) },
        label = { Text(label) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, Modifier.size(18.dp)) },
    )
}

@Composable
private fun Hint(text: String, isError: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** Start and End boxes for "Clip". Empty Start = the beginning, empty End = the end of the video. */
@Composable
private fun ClipFields(
    start: String,
    end: String,
    endPlaceholder: String,
    error: String?,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
) {
    // Decimal keyboard: digits plus "." (the parser accepts 1.30 as well as 1:30).
    val keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = start, onValueChange = onStart,
            label = { Text("Start") }, placeholder = { Text("0:00") },
            isError = error != null, singleLine = true, keyboardOptions = keyboard,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = end, onValueChange = onEnd,
            label = { Text("End") }, placeholder = { Text(endPlaceholder) },
            isError = error != null, singleLine = true, keyboardOptions = keyboard,
            modifier = Modifier.weight(1f),
        )
    }
    if (error != null) Hint(error, isError = true)
    else Hint("Exact cuts take a little longer: the phone re-encodes the clip.")
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun QualityRow(option: QualityOption, enabled: Boolean, onClick: () -> Unit) {
    // ListItem = Material's standard list row: leading badge, text, trailing size.
    ListItem(
        headlineContent = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { QualityBadge(option) },
        trailingContent = option.sizeBytes?.let { size ->
            { Text(QualityOption.formatSize(size), style = MaterialTheme.typography.bodyMedium) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f) // Material's standard "disabled" look
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun QualityBadge(option: QualityOption) {
    // "Best" stands out in the main colour; the rest use the softer container colour.
    val best = option.badge == "Best"
    val container = if (best) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val content = if (best) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val badge = option.badge
        if (badge == null) {
            Icon(painterResource(R.drawable.ic_music), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        } else {
            Text(badge, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        TextButton(onClick = onRetry, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try again")
        }
    }
}
