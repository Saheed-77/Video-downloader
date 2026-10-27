package com.saheed.videodownloader.share

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta

/**
 * The bottom sheet that lists a video's qualities. Used by both the Share menu and the Home tab,
 * so the two look and behave the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    url: String?,
    onDismiss: () -> Unit,
    onPick: (url: String, meta: VideoMeta, option: QualityOption, extras: DownloadExtras) -> Unit,
) {
    // skipPartiallyExpanded: open the sheet fully straight away instead of half-height first.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            if (url == null) {
                Text("No link found", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Share a video from YouTube, Instagram or Facebook.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                FormatPicker(url) { meta, option, extras -> onPick(url, meta, option, extras) }
            }
        }
    }
}
