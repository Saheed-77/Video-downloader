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

## Phase 5: UI redesign + new features (Android) — see plan.md "Phase 5"
- [x] U1: Bottom tabs, Home (Paste + copied-link card), Settings, shared picker sheet — Downloads tab is added in U3; Coil 3.5.0 (3.6 needs compileSdk 37)
- [x] U2: Picker redesign — thumbnail, title, channel, duration, quality badges, Try again
- [ ] **Checkpoint:** review the new look on the S24
- [x] U3: Download records + Downloads tab (progress, Cancel, Open, Share, Retry, Remove) — checked on the S24
- [x] U4: Notification actions — Share (finished), Retry (failed) — checked on the S24
- [ ] **Checkpoint:** review Downloads
- [x] U5: Subtitles — English only (Saheed chose, avoids YouTube "too many requests"); real subtitles, not auto-captions — checked on the S24 (English mov_text track confirmed)
- [x] U6: Clip a section — checked on the S24 (1:00–1:30 at 720p gave a 30.0 s H.264 MP4 with subtitles)
- [x] U6 fix: no subtitles on clips (Saheed chose; yt-dlp doesn't cut subtitles and ffmpeg's shift options gave wrong times) — Subtitles chip greys out while Clip is on
- [x] U6b: Change quality (running and cancelled/failed cards) — checked on the S24 (running 1080p → 144p, cancelled 360p → 144p)
- [x] U7: Playlist download — checked on the S24 (6-video TEDx playlist, one after another; cancel stops the waiting ones)
- [x] Cancel all / Retry all buttons on the Downloads tab (Saheed asked, 28 Oct) — checked on the S24; Retry all restarts them one at a time
- [x] "Download another quality" in every card's ⋮ menu (Saheed asked, 28 Oct): new card, original kept — checked on the S24. Separate downloads already run in parallel (checked: two 1080p at once); playlists and Retry all stay one at a time
- [x] Playlists and Retry all download 2 at a time (Saheed asked, 28 Oct): two WorkManager chains ("lanes"); ✕ on one playlist video stops the whole group — lanes checked on the S24 (2 running, then the next pair); group cancel not yet checked on the phone. After a day of testing YouTube answered "429 Too Many Requests / confirm you're not a bot" for 5 of 6 videos (IP rate limit)
- [ ] **Checkpoint:** review extras
- [ ] U8: Wrap-up — test-matrix rows (section 3b) and CLAUDE.md done; on the S24 P11, P13–P16, P18 pass; P1–P10, P12, P17 still to run

## Future (after downloads work)
- [ ] In-app download history screen (now Phase 5, U3)
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
- **Toolchain:** AGP 9.2.1 (built-in Kotlin; newest that Android Studio 2026.1.1 supports), Gradle 9.6.0, Kotlin 2.4.21, compile/target SDK 36, min SDK 24.
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
