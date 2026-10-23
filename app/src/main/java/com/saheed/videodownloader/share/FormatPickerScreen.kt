package com.saheed.videodownloader.share

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.saheed.videodownloader.download.ErrorMapper
import com.saheed.videodownloader.formats.FormatParser
import com.saheed.videodownloader.formats.QualityOption
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
    data class Loaded(val title: String, val options: List<QualityOption>) : PickerState
    data class Failed(val message: String) : PickerState
}

/**
 * Looks up [url] with yt-dlp and lists the qualities it really has. Tapping a row calls
 * [onPick] with the video title and the chosen quality.
 */
@Composable
fun FormatPicker(url: String, onPick: (title: String, option: QualityOption) -> Unit) {
    var state by remember(url) { mutableStateOf<PickerState>(PickerState.Loading) }

    // Re-runs whenever `url` changes; the work runs in the background (Dispatchers.IO).
    LaunchedEffect(url) {
        App.initError?.let {
            state = PickerState.Failed(it)
            return@LaunchedEffect
        }
        state = try {
            val info = withContext(Dispatchers.IO) {
                // --no-playlist: a link opened from inside a playlist describes just that one video.
                val request = YoutubeDLRequest(url).addOption("--no-playlist")
                EngineUpdater.useEngine { YoutubeDL.getInstance().getInfo(request) }
            }
            PickerState.Loaded(info.title ?: "video", FormatParser.fromVideoInfo(info))
        } catch (e: Exception) {
            PickerState.Failed(ErrorMapper.toMessage(e.message))
        }
    }

    when (val s = state) {
        PickerState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp))
            Text("Finding qualities…", Modifier.padding(start = 16.dp))
        }

        is PickerState.Failed -> Text(s.message, color = MaterialTheme.colorScheme.error)

        is PickerState.Loaded -> Column {
            Text(
                s.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            // LazyColumn = a scrolling list that only builds the rows on screen (like RecyclerView).
            LazyColumn(Modifier.padding(top = 8.dp)) {
                items(s.options) { option ->
                    Text(
                        option.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(s.title, option) }
                            .padding(vertical = 14.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
