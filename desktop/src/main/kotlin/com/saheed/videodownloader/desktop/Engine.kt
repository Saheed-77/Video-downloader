package com.saheed.videodownloader.desktop

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * The downloader engine on the PC: yt-dlp.exe plus ffmpeg, run as normal Windows programs.
 * yt-dlp comes from its official GitHub releases; ffmpeg from this PC's PATH if it has one,
 * otherwise from yt-dlp's recommended FFmpeg-Builds.
 */
object Engine {

    private const val YT_DLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
    private const val FFMPEG_ZIP_URL =
        "https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
    private val UPDATE_EVERY_MS = TimeUnit.DAYS.toMillis(7)

    val ytDlp = File(DesktopSettings.binDir, "yt-dlp.exe")

    /** The folder with ffmpeg.exe: from PATH, or the one this app downloaded. */
    val ffmpegDir: File?
        get() = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .map(::File).firstOrNull { File(it, "ffmpeg.exe").isFile }
            ?: DesktopSettings.binDir.takeIf { File(it, "ffmpeg.exe").isFile }

    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    /**
     * Makes sure yt-dlp and ffmpeg are there (downloading them on first start) and updates yt-dlp
     * once a week. [onStatus] gets short messages for the window. Blocking: call off the UI thread.
     */
    fun prepare(onStatus: (String) -> Unit) {
        if (!ytDlp.isFile) {
            onStatus("Downloading yt-dlp…")
            download(YT_DLP_URL, ytDlp)
            DesktopSettings.lastEngineUpdate = System.currentTimeMillis()
        }
        if (ffmpegDir == null) {
            onStatus("Downloading ffmpeg (about 140 MB, first start only)…")
            downloadFfmpeg()
        }
        val last = DesktopSettings.lastEngineUpdate
        if (last == null || System.currentTimeMillis() - last > UPDATE_EVERY_MS) {
            onStatus("Checking for a yt-dlp update…")
            runCatching { update() }
        }
        onStatus("")
    }

    /** "Update engine": runs `yt-dlp -U` and returns a message for the user. */
    fun update(): String {
        val result = run(listOf("-U"))
        DesktopSettings.lastEngineUpdate = System.currentTimeMillis()
        return when {
            "up to date" in result.output -> "Already up to date"
            "Updated yt-dlp to" in result.output -> "Updated to ${version()}"
            else -> result.output.lines().lastOrNull { it.isNotBlank() } ?: "Update finished"
        }
    }

    fun version(): String? = runCatching { run(listOf("--version")).output.trim().lines().first() }.getOrNull()

    /** What a yt-dlp run printed, and whether it succeeded. */
    data class Result(val exitCode: Int, val output: String, val errors: String)

    /**
     * Runs yt-dlp with [args] and waits for it. Each printed line also goes to [onLine] (progress).
     * [onStart] receives the running process, so it can be stopped (Cancel).
     */
    fun run(args: List<String>, onStart: (Process) -> Unit = {}, onLine: (String) -> Unit = {}): Result {
        val command = buildList {
            add(ytDlp.absolutePath)
            ffmpegDir?.let { add("--ffmpeg-location"); add(it.absolutePath) }
            addAll(args)
        }
        val process = ProcessBuilder(command).start()
        onStart(process)
        // Read errors on their own thread, so a full error pipe can't make yt-dlp wait forever.
        val errors = StringBuilder()
        val errorReader = Thread { process.errorStream.bufferedReader(Charsets.UTF_8).forEachLine { errors.appendLine(it) } }
            .apply { start() }
        val output = StringBuilder()
        process.inputStream.bufferedReader(Charsets.UTF_8).forEachLine { line ->
            output.appendLine(line)
            onLine(line)
        }
        val code = process.waitFor()
        errorReader.join()
        return Result(code, output.toString(), errors.toString())
    }

    private fun download(url: String, target: File) {
        val tmp = File(target.parentFile, target.name + ".part")
        val response = http.send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofFile(tmp.toPath()))
        check(response.statusCode() == 200) { "Download failed (${response.statusCode()}): $url" }
        java.nio.file.Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    /** Downloads the ffmpeg zip and keeps just ffmpeg.exe and ffprobe.exe from its bin folder. */
    private fun downloadFfmpeg() {
        val zip = File(DesktopSettings.binDir, "ffmpeg.zip")
        download(FFMPEG_ZIP_URL, zip)
        ZipInputStream(zip.inputStream()).use { input ->
            generateSequence { input.nextEntry }.forEach { entry ->
                val name = entry.name.substringAfterLast('/')
                if (name == "ffmpeg.exe" || name == "ffprobe.exe") {
                    File(DesktopSettings.binDir, name).outputStream().use { input.copyTo(it) }
                }
            }
        }
        zip.delete()
    }
}
