package com.saheed.videodownloader.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Asks once for the runtime permissions downloads need:
 * - Android 13+: showing notifications (progress / complete).
 * - Android 7–9 only: writing to the Downloads folder (newer versions use MediaStore, no permission).
 * Must be called from the activity's onCreate.
 */
fun ComponentActivity.requestDownloadPermissions() {
    val needed = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

    if (needed.isNotEmpty()) {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
            .launch(needed.toTypedArray())
    }
}
