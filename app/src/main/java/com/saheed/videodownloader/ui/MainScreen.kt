package com.saheed.videodownloader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saheed.videodownloader.App
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Task 1 smoke test: a short public video ("Me at the zoo").
private const val TEST_URL = "https://www.youtube.com/watch?v=jNQXAC9IVRw"

@Composable
fun MainScreen() {
    // `remember { mutableStateOf(..) }` is a value Compose watches: changing it redraws the screen.
    // `by` lets us read/write it like a normal variable.
    var title by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(App.initError) }

    // LaunchedEffect runs a coroutine (Kotlin's lightweight background task) once when the
    // screen appears. withContext(Dispatchers.IO) moves the slow network call off the UI thread.
    LaunchedEffect(Unit) {
        if (error != null) return@LaunchedEffect
        try {
            title = withContext(Dispatchers.IO) { YoutubeDL.getInstance().getInfo(TEST_URL).title }
        } catch (e: Exception) {
            error = e.message ?: e.toString()
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Engine check", style = MaterialTheme.typography.titleLarge)
            when {
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                title == null -> CircularProgressIndicator(Modifier.padding(top = 16.dp))
                else -> Text("Video title: $title", Modifier.padding(top = 16.dp))
            }
        }
    }
}
