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
- [x] Pauses for playlist and Retry all downloads (Saheed asked, 28 Oct): random 5–10 s before each video + 1 s between yt-dlp requests (`--sleep-interval 5 --max-sleep-interval 10 --sleep-requests 1`); single downloads unchanged — checked on the S24 (yt-dlp logged "Sleeping 5–9 s" before each video)
- [x] Playlist sections in the Downloads tab (Saheed asked, 28 Oct): playlist name + summary, Stop / Resume / Cancel / Retry for the whole playlist, ✕ and Retry for one video only. The app's own queue replaces WorkManager chains (still 2 at a time) — checked on the S24 (P16, P21–P23); Retry of a single playlist video (P24) not yet
- [x] Clearing the list (Saheed asked, 28 Oct): Clear menu (finished / failed and cancelled / all except running) and swipe a card left to remove — checked on the S24 (P25, P26)

## Next features (Saheed picked after the feature research, 31 Oct) — see plan in chat
- [x] F1: Prefer H.264/AAC video at the chosen resolution (`-S res,vcodec:h264,acodec:m4a`) — build + tests pass, phone check pending
- [x] F2: MP3s get cover art + title/artist (`--embed-thumbnail --embed-metadata`) — build + tests pass, phone check pending
- [x] F3: Speed and time left on the card and notification — build + tests pass, phone check pending
- [x] F4: "Download only on Wi-Fi" setting; downloads wait for the connection (WorkManager network constraint, retry ×5 on "No internet") — build + tests pass, phone check pending
- [x] F5: Search box on the Downloads tab (more than 5 rows) — build + tests pass, phone check pending
- [x] F6: "When sharing a link" default quality (Always ask / Best / 1080p / 720p / 480p / MP3) — build + tests pass, phone check pending
- [x] F7: Sign in to Instagram/Facebook (Settings → Accounts, in-app WebView, cookies.txt for yt-dlp) — build + tests pass; Saheed signs in himself on the phone
- [ ] Phone check of F1–F7, plus a Reddit and an X/Twitter video
- [x] V1: "Video format" setting — Auto for this phone (AV1 when the phone decodes it in hardware, else H.264), Smallest files (AV1/VP9 + Opus), Most compatible (H.264 + AAC); picker sizes follow it — build + tests pass, phone check pending

## Phase 6: Windows desktop app (Saheed chose: own app, Kotlin + Compose for Desktop, same repo)
- [x] D1: Move the pure logic into a `:shared` module used by the Android app — 60 shared + 29 app tests pass, APK builds; phone check pending
- [x] D2: `:desktop` module, a window with Home / Downloads / Settings — `gradlew :desktop:run` opens it (Compose Multiplatform 1.12.2)
- [x] D3: Engine (official yt-dlp.exe + ffmpeg), lookup shows real qualities and thumbnail — checked on this PC (yt-dlp 2026.10.26 downloaded on first start, ffmpeg from PATH; "Me at the zoo" showed thumbnail, details and Best/240p/144p/MP3) — **Checkpoint**
- [x] D4: Downloads with progress, Cancel, Retry, Open, to ~/Downloads — checked on this PC: 240p (H.264 + AAC), MP3 (cover + tags), 1080p at 14 MB/s with live speed/time left, Cancel (process stopped, no file left), Settings (format, folder, engine)
- [x] D5: Playlists with sections, Clear, search, drag-and-drop a link — checked on this PC: playlist section (Stop / Resume / Cancel one video / Cancel all), Clear failed and cancelled, search, Home keeps its lookup, Subtitles + Clip (10.0 s clip; error for a too-long end). Drag-and-drop not checked (can't be simulated)
- [x] D6: Packaged VideoDownloader.exe (zip for GitHub Releases) — `gradlew :desktop:packageZip` → 64 MB zip (128 MB folder: exe + its own Java). Checked on this PC: the packaged exe looks up (thumbnail over HTTPS), downloads, and fetches yt-dlp.exe on a fresh start. No .msi (WiX not installed) — **Checkpoint**
- [x] L1: New logos and a Theme setting (Settings → Look → Theme on Android, Settings → Theme on the PC).
  Play-down (violet/pink, the default), Midnight (navy with lime/cyan, always dark) and, on Android 12+, Wallpaper colours.
  The icon follows the theme: Android swaps launcher *activity aliases*; the desktop window/taskbar icon changes live, the .exe keeps Play-down.
  Checked on this PC (switch, restart keeps it, .exe icon). **Phone check pending:** colours, status-bar icons on Midnight, launcher icon.
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
