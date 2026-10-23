package com.saheed.videodownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.saheed.videodownloader.download.DownloadWorker
import com.saheed.videodownloader.share.FormatPicker
import com.saheed.videodownloader.share.UrlExtractor

@Composable
fun MainScreen() {
    val context = LocalContext.current

    // `remember { mutableStateOf(..) }` is a value Compose watches: changing it redraws the screen.
    // `by` lets us read/write it like a normal variable.
    var text by remember { mutableStateOf("") }
    var pickerUrl by remember { mutableStateOf<String?>(null) }
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
                enabled = text.isNotBlank(),
                onClick = {
                    pickerUrl = UrlExtractor.extract(text)
                    status = if (pickerUrl == null) "No link found" else null
                }
            ) { Text("Find qualities") }

            pickerUrl?.let { url ->
                FormatPicker(url) { title, option ->
                    DownloadWorker.enqueue(context, url, title, option)
                    pickerUrl = null
                    text = ""
                    status = "Downloading ${option.label} — see the notification."
                }
            }

            status?.let { Text(it) }
        }
    }
}
