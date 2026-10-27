package com.saheed.videodownloader.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.saheed.videodownloader.R
import com.saheed.videodownloader.download.DownloadWorker
import kotlinx.coroutines.launch

/** The bottom tabs. An `enum class` is like a Java enum; `entries` lists its values in order. */
private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home),
    SETTINGS("Settings", R.drawable.ic_settings),
}

/** The app's main screen: a bottom *navigation bar* switching between the tabs. */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // A *snackbar* is the short message that pops up at the bottom of the screen.
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, t ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(painterResource(t.icon), contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (Tab.entries[tab]) {
                Tab.HOME -> HomeTab { url, title, option ->
                    DownloadWorker.enqueue(context, url, title, option)
                    scope.launch { snackbar.showSnackbar("Downloading ${option.label}. See the notification.") }
                }
                Tab.SETTINGS -> SettingsTab()
            }
        }
    }
}
