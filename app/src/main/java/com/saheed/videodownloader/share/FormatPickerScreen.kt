package com.saheed.videodownloader.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.saheed.videodownloader.App
import com.saheed.videodownloader.R
import com.saheed.videodownloader.download.ErrorMapper
import com.saheed.videodownloader.formats.FormatParser
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import com.saheed.videodownloader.update.EngineUpdater
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The three things this screen can show. A `sealed interface` is a closed set of types,
 * so `when` below must handle each one (like a Java sealed interface + switch).
 */
private sealed interface PickerState {
    data object Loading : PickerState
    data class Loaded(val meta: VideoMeta, val options: List<QualityOption>) : PickerState
    data class Failed(val message: String) : PickerState
}

private val thumbnailShape = RoundedCornerShape(12.dp)

/**
 * Looks up [url] with yt-dlp, shows the video's thumbnail and details, and lists the qualities
 * it really has. Tapping a row calls [onPick] with the video title and the chosen quality.
 */
@Composable
fun FormatPicker(url: String, onPick: (title: String, option: QualityOption) -> Unit) {
    val context = LocalContext.current.applicationContext
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
        state = try {
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

        is PickerState.Loaded -> {
            val (audio, video) = s.options.partition { it.isAudio }
            // LazyColumn = a scrolling list that only builds the rows on screen (like RecyclerView).
            // The header is its first item, so everything scrolls together.
            LazyColumn {
                item { VideoHeader(s.meta) }
                item { SectionLabel("Video") }
                items(video) { option -> QualityRow(option) { onPick(s.meta.title, option) } }
                if (audio.isNotEmpty()) {
                    item { SectionLabel("Audio") }
                    items(audio) { option -> QualityRow(option) { onPick(s.meta.title, option) } }
                }
            }
        }
    }
}

@Composable
private fun VideoHeader(meta: VideoMeta) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            if (meta.thumbnailUrl != null) {
                AsyncImage(
                    model = meta.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            meta.durationText?.let {
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
private fun QualityRow(option: QualityOption, onClick: () -> Unit) {
    // ListItem = Material's standard list row: leading badge, text, trailing size.
    ListItem(
        headlineContent = { Text(option.label, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = { QualityBadge(option) },
        trailingContent = option.sizeBytes?.let { size ->
            { Text(QualityOption.formatSize(size), style = MaterialTheme.typography.bodyMedium) }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
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
