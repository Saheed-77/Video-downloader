# Todo — Share-to-Download Video App (personal use)

See `tasks/plan.md` for details, acceptance criteria and verification for each task.

## Phase 1: Foundation (Android)
- [x] Task 1: Project skeleton + yt-dlp/ffmpeg initialise on device (S) — deps: none — code done, phone check pending
- [x] Task 2: Paste a URL → download best MP4 into app storage (S) — deps: 1 — code done, phone check pending
- [ ] **Checkpoint:** real phone install, hardcoded URL downloads and plays, review with Saheed

## Phase 2: Core features (Android)
- [x] Task 3: App appears in the share sheet and reads the link (S) — deps: 2 — code done, phone check pending
- [x] Task 4: Quality picker — all available resolutions (like YouTube) + MP3 entry (M) — deps: 3 — code done, phone check pending (approved addition: "Best quality" row at the top)
- [x] Task 5: Background download + progress notification, saved to Downloads (M) — deps: 4 — code done, phone check pending
- [x] Task 6: MP3 audio download, saved to Downloads (S) — deps: 5 — code done, phone check pending
- [ ] **Checkpoint:** share from YouTube → 720p MP4 and MP3 both land in Downloads and play; download survives leaving the app; review

## Phase 3: Robustness (Android)
- [x] Task 7: yt-dlp auto-update weekly + manual update button (S) — deps: 6 — code done, phone check pending
- [x] Task 8: Friendly errors + cancel download (S) — deps: 6 — code done, phone check pending
- [ ] Task 9: Real-site test matrix + install on your phone (S) — deps: 7, 8 — build + lint clean, `tasks/test-matrix.md` ready; **run it on the S24**
- [ ] **Checkpoint:** Android complete, review

## Phase 4: iOS via Shortcut + a-Shell (for dad + nephew; no Mac; needs their iPhone)
- [ ] Task 10: a-Shell spike — one video + one MP3 download; check ffmpeg; confirm video can be saved to Photos (S)
- [ ] Task 11: Share-sheet Shortcut with quality menu → video to Photos, MP3 to Files; weekly yt-dlp update + "Update downloader" item (S) — deps: 10
- [ ] **Checkpoint:** dad shares from YouTube → More → Download Video → picks quality → video in Photos, without help
- [ ] (Only if Task 10 fails) fallback: home-PC server + Shortcut

## Future (after downloads work)
- [ ] In-app download history screen
- [ ] Settings option to choose the save folder

## Decisions confirmed by Saheed
- [x] arm64-v8a only build (phones ≤4 years old, S24)
- [x] yt-dlp updates: weekly auto + manual fallback, both platforms
- [x] Order: Android first, then iPhone
- [x] Hosting: GitHub only (code + APK releases), no Vercel/Netlify
- [x] App name "Video Downloader", package `com.saheed.videodownloader` (name: `res/values/strings.xml`; logo: Android Studio → New → Image Asset)
- [x] "Best quality" row always at the top of the quality list
- [x] Overnight build: all Android tasks in one go, one commit per task on main, not pushed

## Morning notes (overnight build, 2026-10-24)
Built and checked on the PC only: `assembleDebug`, 34 unit tests and `lintDebug` (0 errors) all pass.
Nothing has run on a phone yet, so every "phone check pending" above still needs the S24.
- **Toolchain:** AGP 9.4.1 (built-in Kotlin), Gradle 9.6.0, Kotlin 2.4.21, compile/target SDK 36, min SDK 24.
  The debug APK is about 68 MB (arm64 only). The library bundles Python 3.12, so updating to the latest yt-dlp is fine.
- **First launch** updates yt-dlp automatically (no saved date yet), so the first link may take a few extra seconds.
- **Small additions while building:**
  - `--no-playlist`, so a YouTube link from inside a playlist downloads just that video.
  - Resolutions are labelled by the shorter side, so a vertical Short reads 1080p, not 1920p.
  - A lock stops a yt-dlp update and a download from running at the same moment.
  - Error messages for "YouTube bot check", "video unavailable" and "quality not available".
- **Not verified yet / worth watching on the phone:**
  - The share sheet's bottom-sheet look.
  - Notification permission prompt on first share.
  - Cancel mid-merge (ffmpeg may finish a moment after Cancel; the leftover file is deleted anyway).
  - Instagram/Facebook may need login (out of scope).
  - On Android 12+ the system can refuse to start "foreground" work right after the share sheet closes.
    The worker catches that and keeps downloading with a normal notification.
    If long downloads get stopped when the phone is idle, tell Claude: the fix is to make the work "expedited".
- **Lint warnings left on purpose:**
  - Newer library versions available.
  - targetSdk 36 not 37 (SDK 37 isn't installed).
  - No x86_64 for ChromeOS (decision 12).
  - allowBackup deprecation.

## Next
- [ ] Run `tasks/test-matrix.md` on the S24, then review the three Android checkpoints above
- [ ] After that: push to GitHub + attach the APK to a GitHub Release (decision 15), then Phase 4 (iPhone)
