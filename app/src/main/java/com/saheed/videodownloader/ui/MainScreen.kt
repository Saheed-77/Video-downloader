package com.saheed.videodownloader.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.saheed.videodownloader.R
import com.saheed.videodownloader.download.DownloadStore
import com.saheed.videodownloader.download.DownloadWorker

/** The bottom tabs. An `enum class` is like a Java enum; `entries` lists its values in order. */
private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    HOME("Home", R.drawable.ic_home),
    DOWNLOADS("Downloads", R.drawable.ic_download),
    SETTINGS("Settings", R.drawable.ic_settings),
}

/** The app's main screen: a bottom *navigation bar* switching between the tabs. */
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(Tab.HOME.ordinal) }
    val records by DownloadStore.records.collectAsState()
    val activeCount = records.count { it.isActive }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.ordinal,
                        onClick = { tab = t.ordinal },
                        icon = {
                            // A small count bubble on Downloads while something is downloading.
                            BadgedBox(badge = {
                                if (t == Tab.DOWNLOADS && activeCount > 0) Badge { Text("$activeCount") }
                            }) { Icon(painterResource(t.icon), contentDescription = null) }
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (Tab.entries[tab]) {
                Tab.HOME -> HomeTab { url, meta, option, extras ->
                    DownloadWorker.enqueue(context, url, meta.title, meta.thumbnailUrl, option, extras)
                    tab = Tab.DOWNLOADS.ordinal
                }
                Tab.DOWNLOADS -> DownloadsTab()
                Tab.SETTINGS -> SettingsTab()
            }
        }
    }
}
