package com.saheed.videodownloader.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import java.io.File

/**
 * Copies a finished download into the phone's public Downloads folder.
 *
 * Android 10+ uses *scoped storage*: apps can't write anywhere they like, but they can add files
 * to shared folders through *MediaStore* (the system's index of photos/videos/music/downloads)
 * without any storage permission. The file then shows up in Files and Samsung Gallery.
 */
object MediaStoreSaver {

    fun mimeTypeOf(file: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "application/octet-stream"

    /**
     * Returns a content Uri for opening the saved file, or null on Android 7–9
     * (there the notification opens the Downloads app instead).
     */
    fun save(context: Context, file: File): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveWithMediaStore(context, file)
        else saveLegacy(context, file)

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(context: Context, file: File): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeTypeOf(file))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // IS_PENDING hides the file from other apps until the copy is finished.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Couldn't create the file in Downloads")
        try {
            resolver.openOutputStream(uri).use { out ->
                requireNotNull(out) { "Couldn't write to Downloads" }
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    /** Android 7–9: plain file copy (needs WRITE_EXTERNAL_STORAGE), then tell the media scanner. */
    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, file: File): Uri? {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        var target = File(dir, file.name)
        var n = 1
        while (target.exists()) target = File(dir, "${file.nameWithoutExtension} (${n++}).${file.extension}")
        file.copyTo(target)
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(mimeTypeOf(file)), null)
        return null
    }
}
