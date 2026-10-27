package com.saheed.videodownloader.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.saheed.videodownloader.R
import com.saheed.videodownloader.formats.DownloadExtras
import com.saheed.videodownloader.formats.QualityOption
import com.saheed.videodownloader.formats.VideoMeta
import com.saheed.videodownloader.share.PickerSheet
import com.saheed.videodownloader.share.UrlExtractor
import kotlinx.coroutines.launch

/**
 * Home tab: paste a link (or use the one just copied), then pick a quality in the bottom sheet.
 * [onPick] starts the download.
 */
@Composable
fun HomeTab(onPick: (url: String, meta: VideoMeta, option: QualityOption, extras: DownloadExtras) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // rememberSaveable (unlike remember) also survives screen rotation.
    var text by rememberSaveable { mutableStateOf("") }
    var noLinkError by remember { mutableStateOf(false) }
    // Non-null while the quality sheet is open.
    var pickerUrl by rememberSaveable { mutableStateOf<String?>(null) }
    // A link found on the clipboard, and the last one the user closed or already used.
    var copiedLink by remember { mutableStateOf<String?>(null) }
    var dismissedLink by rememberSaveable { mutableStateOf<String?>(null) }

    // Android only lets an app read the clipboard while its window has focus, so check each
    // time the window gains it (opening the app, coming back to it).
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) {
        if (focused) copiedLink = UrlExtractor.extract(clipboard.readText(context))
    }

    fun open(url: String) {
        text = url
        noLinkError = false
        pickerUrl = url
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Video Downloader", style = MaterialTheme.typography.headlineMedium)
            Text(
                "YouTube, Instagram and Facebook",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        App.initError?.let {
            Text("Engine error: $it", color = MaterialTheme.colorScheme.error)
        }

        val copied = copiedLink
        if (copied != null && copied != dismissedLink && copied != UrlExtractor.extract(text)) {
            CopiedLinkCard(
                link = copied,
                onUse = { dismissedLink = copied; open(copied) },
                onDismiss = { dismissedLink = copied },
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; noLinkError = false },
                    placeholder = { Text("Paste a video link") },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_link), contentDescription = null) },
                    // Paste button while empty, clear button once there's text.
                    trailingIcon = {
                        if (text.isEmpty()) {
                            IconButton(onClick = {
                                scope.launch {
                                    text = clipboard.readText(context).orEmpty()
                                    noLinkError = false
                                }
                            }) { Icon(painterResource(R.drawable.ic_paste), contentDescription = "Paste") }
                        } else {
                            IconButton(onClick = { text = ""; noLinkError = false }) {
                                Icon(painterResource(R.drawable.ic_close), contentDescription = "Clear")
                            }
                        }
                    },
                    isError = noLinkError,
                    supportingText = if (noLinkError) {
                        { Text("No link found in that text") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        val url = UrlExtractor.extract(text)
                        if (url == null) noLinkError = true else open(url)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(painterResource(R.drawable.ic_download), contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Find qualities")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Quicker way: in YouTube, Instagram or Facebook tap Share, then Video Downloader.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    pickerUrl?.let { url ->
        PickerSheet(url, onDismiss = { pickerUrl = null }) { pickedUrl, meta, option, extras ->
            pickerUrl = null
            text = ""
            dismissedLink = pickedUrl
            onPick(pickedUrl, meta, option, extras)
        }
    }
}

@Composable
private fun CopiedLinkCard(link: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Copied link found", style = MaterialTheme.typography.titleSmall)
                Text(
                    link.removePrefix("https://").removePrefix("www."),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Dismiss")
            }
        }
        TextButton(onClick = onUse, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
            Text("Use this link")
        }
    }
}

/** The clipboard's text, or null. `suspend` because Compose's clipboard API is asynchronous. */
private suspend fun Clipboard.readText(context: Context): String? {
    val clip = getClipEntry()?.clipData ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
