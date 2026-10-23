package com.saheed.videodownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.saheed.videodownloader.download.Downloader
import com.saheed.videodownloader.share.FormatPicker
import com.saheed.videodownloader.share.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MainScreen() {
    val context = LocalContext.current
    // rememberCoroutineScope gives a scope tied to this screen, for starting work from a button tap.
    val scope = rememberCoroutineScope()

    // `remember { mutableStateOf(..) }` is a value Compose watches: changing it redraws the screen.
    // `by` lets us read/write it like a normal variable.
    var text by remember { mutableStateOf("") }
    var pickerUrl by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    var status by remember { mutableStateOf(App.initError?.let { "Engine error: $it" }) }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Paste a video link", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; pickerUrl = null },
                label = { Text("Link") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = text.isNotBlank() && progress == null,
                onClick = {
                    pickerUrl = UrlExtractor.extract(text)
                    status = if (pickerUrl == null) "No link found" else null
                }
            ) { Text("Find qualities") }

            pickerUrl?.let { url ->
                FormatPicker(url) { _, option ->
                    pickerUrl = null
                    progress = 0f
                    status = "Downloading ${option.label}…"
                    scope.launch {
                        status = try {
                            // Dispatchers.IO = a thread pool for slow I/O, so the UI stays responsive.
                            val file = withContext(Dispatchers.IO) {
                                // MP3 is handled from Task 6; until then it falls back to best MP4.
                                val format = option.format ?: Downloader.BEST_MP4
                                Downloader.download(url, format, File(context.cacheDir, "downloads")) { p ->
                                    progress = p
                                }
                            }
                            "Saved: ${file.absolutePath} (${file.length() / 1_000_000} MB)"
                        } catch (e: Exception) {
                            "Failed: ${e.message}"
                        }
                        progress = null
                    }
                }
            }

            progress?.let {
                LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth())
                Text("${it.toInt()}%")
            }
            status?.let { Text(it) }
        }
    }
}
