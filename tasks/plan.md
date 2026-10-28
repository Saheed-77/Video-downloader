# Implementation Plan: Share-to-Download Video App

> Working name: **video-downloader** (placeholder, rename anytime)

## Overview

A phone app that appears in the **share sheet** (the "Share to…" menu) of YouTube, Instagram,
Facebook and similar apps. When the user taps it, the app reads the shared link, fetches the list
of available qualities, and lets the user pick one (e.g. 1080p / 720p / 480p video, or MP3 audio).
The file downloads in the background and lands in the phone's **Downloads** folder.

All downloading runs **on the phone**, using **yt-dlp** (the open-source engine behind most
"paste a URL" downloader websites) plus **ffmpeg** (for merging video+audio and converting to MP3).
No server, no hosting cost.

**Personal use only** — not published to any store or website.

**Who it's for:** Saheed's dad and nephew, who use **iPhones** and aren't very technical. Today they
copy a link, paste it into a downloader website, and the file ends up buried in the Files app
instead of the Photos gallery. The goal: **Share → pick quality → video appears in Photos.** No
copy-paste, no hunting for files. The Android app is for Saheed's own Samsung S24.

**Build order:** Android app first (Phases 1–3). iOS (Phase 4) is **not an app** — it's an
iOS *Shortcut* running yt-dlp inside the free a-Shell app, so no Mac is needed (decision 9).
Android goes first because it's the guaranteed path (Saheed's decision); iPhone follows.

## What the user sees (UI flow)

### Android (Saheed's S24)

1. Watching a video in YouTube (or Instagram / Facebook) → tap **Share** → tap our app's icon in the share list.
2. A small screen slides up over YouTube showing the video title and "Finding qualities…" (a few seconds).
3. A list appears, like YouTube's own quality menu, showing only what that video has:
   `1080p · 85 MB`, `720p · 45 MB`, `480p · 22 MB`, `360p · 12 MB`, `MP3 audio · 4 MB`.
4. Tap one → the screen closes and you're straight back in YouTube.
5. A notification shows **"Downloading <title> — 42%"** with a **Cancel** button.
6. It changes to **"Download complete"** — tap it to play. The file is in **Downloads** and shows up in Samsung Gallery.
7. If something goes wrong, the notification says why in plain words ("This video is private", "Login required", "Try updating the downloader engine").

Opening the app icon directly shows a simple screen: a box to paste a link (for when sharing
isn't available), the downloader engine version + last-updated date, and the **Update downloader
engine** button.

### iPhone (dad + nephew) — to be confirmed in Task 10

1. In YouTube / Instagram / Facebook, tap **Share**. The app opens its *own* share panel, which
   never lists Shortcuts. Tap the option that opens the **iPhone's system share sheet** — usually
   "More", "Share to…" or "Share via…" (iOS share icon: square with an up arrow; name and position
   vary by app and app version).
2. In the iPhone share sheet, **Download Video** is in the action list *below* the row of app icons
   (it's a Shortcut, not an app). During setup, use **Edit Actions… → Favourites** to pin it to the
   top of that list. Routine for dad: **Share → More → Download Video → pick quality.**
3. A menu pops up: **Best · 1080p · 720p · 480p · 360p · MP3 · Update downloader**.
4. Tap a quality. a-Shell may open briefly while it downloads, then iOS returns to YouTube.
5. A banner says **"Saved to Photos"** — the video is in the Photos app under Recents. MP3s go to the Files app.

Task 10 confirms step 4 (whether a-Shell has to open visibly) and step 5 (saving to Photos).
Show dad the "More" step once in each of the three apps, since its position differs.

## Architecture Decisions

| # | Decision | Why |
|---|----------|-----|
| 1 | **Android: native app in Kotlin + Jetpack Compose** | You said language doesn't matter, so this picks what fits best: the downloader library (`youtubedl-android`) is written in Kotlin, the best open-source reference app (Seal) is Kotlin + Compose, and Kotlin is Google's official Android language. Download speed is the same in any language — yt-dlp does the heavy lifting. Kotlin reads close to Java, so the jump is small. |
| 2 | **Not Flutter / React Native** | Cross-platform frameworks only share the *UI*. The hard parts here (share sheet, running yt-dlp, saving files) are native on each platform anyway, so a cross-platform layer adds complexity without saving work. |
| 3 | **Downloader engine: `youtubedl-android` 0.18.x** (library + ffmpeg modules, Maven Central `io.github.junkfood02.youtubedl-android`) | Bundles yt-dlp + Python + ffmpeg for Android. Has `getInfo(url)` (list formats), `execute(request, callback)` (download with progress %), and `updateYoutubeDL()` (update yt-dlp without an app release). |
| 4 | **Share sheet via an *intent filter*** for `ACTION_SEND` + `text/plain` on a small `ShareActivity` | This is the standard Android mechanism ("share target") that makes an app show up in other apps' Share menus. Apps share links as plain text. |
| 5 | **Background downloads with *WorkManager* (as a foreground worker with a progress notification)** | Downloads keep going if the user leaves the app; Android won't kill them. WorkManager is Google's recommended tool for this. |
| 6 | **Download into app's private cache → then copy into public `Downloads/` via *MediaStore*** | Android 10+ enforces *scoped storage* (apps can't freely write anywhere). MediaStore is the official way to put a file into Downloads so it shows in the Files/Gallery app. No storage permission needed on Android 10+. |
| 7 | **Min SDK 24 (Android 7.0), target the latest SDK** | Covers ~all active phones; matches the library's support. |
| 8 | **Install the APK directly on your own phone (*sideloading*); no store, no publishing** | Personal use. Google Play wouldn't accept a YouTube downloader anyway. |
| 9 | **iOS: an iOS *Shortcut* + the free a-Shell app, not a custom app** | You have no Mac, and building any native iOS app needs Xcode (Mac only). a-Shell is a free App Store terminal app that ships with Python, so yt-dlp installs with `pip`. A Shortcut can appear in the iOS share sheet, show a quality menu, and pass the link to a-Shell to download. Nothing to build or re-sign. |
| 10 | **No PWA for iOS** | A *PWA* (Progressive Web App — a website installed to the home screen) runs inside Safari's sandbox: it can't run yt-dlp or ffmpeg, the browser blocks fetching video streams from YouTube/Instagram directly (*CORS*), and iOS doesn't let PWAs appear in the share sheet. A PWA would only work as a front end to a server that does the downloading — more moving parts than decision 9. |
| 11 | **MP3s save to Downloads** (same as videos) | Simplest: one save location, one code path. A "choose save folder" setting is a future item. |
| 12 | **Android build for `arm64-v8a` only** (approved) | All target phones are ≤4 years old (Samsung S24); every one of them is arm64. Roughly halves the APK size. |
| 13 | **iPhone: videos save to the Photos app; MP3s save to Files** | Videos in Photos is the whole point for non-technical users. iOS Photos can't hold audio files, so MP3s go to Files. |
| 14 | **yt-dlp updates: automatic once a week + manual "Update" as fallback** (both platforms) | Saheed's decision. Keeps downloads working without anyone thinking about it. |
| 15 | **Hosting: GitHub only** — code in a GitHub repo, APKs attached to GitHub Releases. No Vercel/Netlify. | The Android app needs no server at all. Vercel/Netlify host websites and short-lived *serverless functions*, which can't do this job: Vercel's free plan caps a function's response at 4.5 MB (a 720p video is far larger) and runs it for at most 5 minutes, and video sites tend to block requests from cloud data centres. Only relevant if the iPhone fallback server is ever needed, and that would run on a home PC. |

### How the pieces connect (data flow)

```
YouTube/Instagram/FB app ──Share──▶ ShareActivity (intent filter)
                                         │ extract URL from shared text
                                         ▼
                                  FormatPicker screen
                                         │ YoutubeDL.getInfo(url) → list qualities + "MP3"
                                         │ user taps one
                                         ▼
                                  DownloadWorker (WorkManager, foreground notification)
                                         │ YoutubeDL.execute(...) with progress → notification
                                         │ ffmpeg merges video+audio / converts to MP3
                                         ▼
                                  MediaStoreSaver → Downloads/ folder → "Done" notification
```

### Dependency graph

```
Task 1: project + yt-dlp init (foundation, highest risk)
   └── Task 2: download one hardcoded format
          ├── Task 3: share target + URL extraction
          │      └── Task 4: format picker (needs URL + getInfo)
          │             └── Task 5: background download + save to Downloads
          │                    └── Task 6: MP3 option
          │                           ├── Task 7: yt-dlp self-update
          │                           ├── Task 8: errors + cancel
          │                           └── Task 9: real-site test matrix + install
          └── (Phase 4 iOS Shortcut — independent, no code shared with Android)
```

## Task List

### Phase 1: Foundation (prove the engine works on a real phone — fail fast)

- [ ] **Task 1: Project skeleton + yt-dlp/ffmpeg initialise on device** (S)
- [ ] **Task 2: Paste a URL → download best MP4 into app storage** (S)

### Checkpoint: Foundation
- [ ] App installs on a real Android phone (not just emulator)
- [ ] A hardcoded YouTube URL downloads and the file plays
- [ ] Review with Saheed before continuing

### Phase 2: Core features (the share → pick → download flow)

- [ ] **Task 3: App appears in the share sheet and reads the link** (S)
- [ ] **Task 4: Quality picker (all available resolutions + MP3 entry)** (M)
- [ ] **Task 5: Background download with progress notification, saved to Downloads** (M)
- [ ] **Task 6: MP3 audio download** (S)

### Checkpoint: Core features
- [ ] Share from the YouTube app → pick 720p → MP4 appears in Downloads and plays with sound
- [ ] Share from the YouTube app → pick MP3 → MP3 appears in Downloads and plays
- [ ] Leaving the app mid-download doesn't stop it
- [ ] Review with Saheed

### Phase 3: Robustness and release

- [ ] **Task 7: Update yt-dlp from inside the app** (S)
- [ ] **Task 8: Friendly errors + cancel download** (S)
- [ ] **Task 9: Real-site test matrix + install on your phone** (S)

### Checkpoint: Android complete
- [ ] All Android acceptance criteria met
- [ ] Review with Saheed

### Phase 4: iOS via Shortcut + a-Shell (no Mac, no app build)

- [ ] **Task 10: a-Shell spike — yt-dlp downloads one video on the iPhone** (S)
- [ ] **Task 11: Share-sheet Shortcut with quality menu → video to Photos, MP3 to Files** (S)

### Checkpoint: iOS
- [ ] Dad shares from YouTube → More → Download Video → picks quality → video in Photos, without help

### Future (after downloads work — not in current scope)
- In-app download history screen (now Phase 5, U3)
- Settings option to choose the save folder

---

## Task details

### Task 1: Project skeleton + yt-dlp/ffmpeg initialise on device

**Description:** Create a Kotlin + Compose Android project, add the `youtubedl-android`
library and ffmpeg modules, and initialise both when the app starts. Then call
`getInfo()` on one hardcoded YouTube URL and show the video title on screen. This is the riskiest
step (native libraries, APK packaging), so it goes first.

**Acceptance criteria:**
- [ ] App launches on a real phone without crashing
- [ ] `YoutubeDL.getInstance().init()` and `FFmpeg.getInstance().init()` succeed (wrapped in try/catch)
- [ ] Title of a hardcoded public YouTube video is shown on screen

**Verification:**
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Manual: install on phone, see the title

**Dependencies:** None

**Files likely touched:**
- `settings.gradle.kts`, `app/build.gradle.kts` (deps, `abiFilters`, min SDK 24)
- `app/src/main/AndroidManifest.xml` (`android:extractNativeLibs="true"`, INTERNET permission)
- (`abiFilters` set to `arm64-v8a` only — decision 12)
- `app/src/main/java/.../App.kt` (Application class — init here)
- `app/src/main/java/.../MainActivity.kt`

**Estimated scope:** Small–Medium (4 files, mostly config)

---

### Task 2: Paste a URL → download best MP4 into app storage

**Description:** Add a text field + "Download" button on the main screen. Run a
`YoutubeDLRequest` with `-f "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best"`,
`--merge-output-format mp4`, output to the app's cache folder, and show the progress %.
This proves downloading + ffmpeg merging work before any share/UI work.

**Acceptance criteria:**
- [ ] Pasting a YouTube URL and tapping Download shows progress 0 → 100%
- [ ] Resulting MP4 has both video and audio (proves ffmpeg merge works)

**Verification:**
- [ ] Manual: open the file via Android Studio's Device Explorer or play it from a debug button

**Dependencies:** Task 1

**Files likely touched:**
- `MainActivity.kt` (or `MainScreen.kt`)
- `download/Downloader.kt` (thin wrapper around `YoutubeDL.execute`)

**Estimated scope:** Small

---

### Task 3: App appears in the share sheet and reads the link

**Description:** Add a `ShareActivity` with an *intent filter* for `ACTION_SEND` /
`text/plain`. Apps often share extra text around the link (e.g. Facebook: "Check this out
https://fb.watch/…"), so pull out the first `http(s)://` URL with a regex.

**Acceptance criteria:**
- [ ] App is listed when tapping Share → in YouTube, Instagram and Facebook
- [ ] Tapping it opens our screen showing the clean extracted URL
- [ ] Non-URL text shows "No link found" instead of crashing

**Verification:**
- [ ] Unit test: `./gradlew test` — `UrlExtractorTest` covers youtu.be, youtube.com/shorts, instagram.com/reel, fb.watch, text-with-link, no-link
- [ ] Manual: share from each of the three apps

**Dependencies:** Task 2

**Files likely touched:**
- `AndroidManifest.xml` (intent filter)
- `share/ShareActivity.kt`
- `share/UrlExtractor.kt`
- `test/.../UrlExtractorTest.kt`

**Estimated scope:** Small

---

### Task 4: Quality picker (all available resolutions + MP3 entry)

**Description:** After a link is received, call `getInfo(url)` on a background thread
(show a spinner), then list the qualities the video actually has — deduplicated by height
(2160p, 1440p, 1080p, 720p, 480p, 360p…) with approximate file size where available — plus a
fixed **"MP3 (audio only)"** row at the bottom. Tapping a row hands the choice to Task 5.

**Acceptance criteria:**
- [ ] Only qualities that exist for that video are shown (a 720p-max video shows no 1080p)
- [ ] MP3 row always present
- [ ] Spinner while loading; screen doesn't freeze

**Verification:**
- [ ] Unit test: `FormatParserTest` turns a sample format list into the expected deduped rows
- [ ] Manual: share a 4K video and a low-res Short, compare lists

**Dependencies:** Task 3

**Files likely touched:**
- `formats/FormatParser.kt` (VideoInfo formats → list of `QualityOption`)
- `formats/QualityOption.kt` (data class: label, yt-dlp format string, isAudio)
- `share/FormatPickerScreen.kt`
- `test/.../FormatParserTest.kt`

**Estimated scope:** Medium

---

### Task 5: Background download with progress notification, saved to Downloads

**Description:** Move the download into a `DownloadWorker` (WorkManager, running as a
foreground worker so Android doesn't kill it). It shows a notification with progress %, downloads
to cache using the chosen format string (e.g. `bestvideo[height<=720]+bestaudio/best[height<=720]`),
then copies the file into public **Downloads/** using MediaStore and deletes the cache copy.
The share screen closes right after the user picks — the user goes back to YouTube while it downloads.

**Acceptance criteria:**
- [ ] Notification shows title + progress, then "Download complete" (tap opens the file)
- [ ] File appears in the phone's Files app under Downloads with the video's title as filename
- [ ] Download continues after leaving our app

**Verification:**
- [ ] Manual: start a long video, switch apps, confirm it finishes
- [ ] Manual: test on Android 13+ (notification permission prompt appears once)

**Dependencies:** Task 4

**Files likely touched:**
- `download/DownloadWorker.kt`
- `download/MediaStoreSaver.kt`
- `download/Notifications.kt`
- `AndroidManifest.xml` (`POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`)

**Estimated scope:** Medium

---

### Task 6: MP3 audio download

**Description:** When MP3 is picked, run yt-dlp with `-x --audio-format mp3 --audio-quality 0`
(ffmpeg converts the audio). Save to Downloads via the same saver (decision 11).

**Acceptance criteria:**
- [ ] MP3 file saved and plays in a music player
- [ ] Filename is the video title + `.mp3`

**Verification:**
- [ ] Manual: share a YouTube song → MP3 → play it

**Dependencies:** Task 5

**Files likely touched:**
- `download/DownloadWorker.kt` (branch on `isAudio`)
- `formats/QualityOption.kt`

**Estimated scope:** Small

---

### Task 7: Update yt-dlp from inside the app

**Description:** Sites like YouTube change often and break downloaders; the fix is almost always
"update yt-dlp". Two parts (Saheed's decision):
- **Weekly auto-update:** whenever the app is opened (or a link is shared to it), check the saved
  "last updated" date; if it's more than 7 days old, call `updateYoutubeDL(context, STABLE)` in the
  background and save today's date. Simpler than a scheduled background job, and nothing runs when
  the app isn't used.
- **Manual fallback:** an "Update downloader engine" button on the main screen, for when downloads
  stop working before the weekly update kicks in.

**Acceptance criteria:**
- [ ] Main screen shows current yt-dlp version and last-updated date
- [ ] Opening the app with a last-updated date >7 days old triggers an update automatically
- [ ] Button updates on demand; "Already up to date" handled

**Verification:**
- [ ] Unit test: `UpdateSchedulerTest` — 6 days old → no update, 8 days old → update
- [ ] Manual: tap the button, confirm version string changes (or reports up-to-date)

**Dependencies:** Task 6

**Files likely touched:**
- `update/EngineUpdater.kt` (weekly check + update call, date stored in SharedPreferences)
- `MainScreen.kt` (button + version text)
- `test/.../UpdateSchedulerTest.kt`

**Estimated scope:** Small

> Note: `updateYoutubeDL(STABLE)` downloads yt-dlp from yt-dlp's own official GitHub stable releases.

---

### Task 8: Friendly errors + cancel download

**Description:** Map common yt-dlp failures to plain messages: "This video is private",
"Login required (Instagram/Facebook often need this)", "Unsupported link", "No internet",
"Try updating the downloader engine". Add a **Cancel** action on the progress notification
(uses `destroyProcessById`).

**Acceptance criteria:**
- [ ] Private / invalid / no-network cases show a readable message, no crash
- [ ] Cancel stops the download and removes the partial file

**Verification:**
- [ ] Unit test: `ErrorMapperTest` maps sample yt-dlp error strings to messages
- [ ] Manual: airplane mode, a private video link, cancel mid-download

**Dependencies:** Task 6

**Files likely touched:**
- `download/ErrorMapper.kt`
- `download/DownloadWorker.kt`
- `download/Notifications.kt`
- `test/.../ErrorMapperTest.kt`

**Estimated scope:** Small

---

### Task 9: Real-site test matrix + install on your phone

**Description:** Run through the full flow on each target site, record results, and install the
build on your own phone. Since it's personal use, a debug APK is enough — no signing setup,
no store.

**Acceptance criteria:**
- [ ] Test matrix filled in: YouTube video, YouTube Short, Instagram reel, Facebook video, + any other site you care about
- [ ] App installed and working on your phone outside Android Studio

**Verification:**
- [ ] `./gradlew assembleDebug`, install APK on phone
- [ ] Manual: run the matrix

**Dependencies:** Tasks 7, 8

**Files likely touched:**
- `tasks/test-matrix.md`

**Estimated scope:** Small

---

### Task 10: a-Shell spike — yt-dlp downloads one video on the iPhone

**Description:** Install a-Shell (free, App Store) on the iPhone, run
`python3 -m pip install -U yt-dlp`, then download one YouTube video at 720p and one as MP3 by
typing the commands by hand. Also confirm whether `ffmpeg` is available in a-Shell (needed to
merge HD video+audio and make MP3s) — sources confirm yt-dlp works there; ffmpeg must be checked.

**Acceptance criteria:**
- [ ] 720p video with sound downloads into a-Shell's folder and plays from the Files app
- [ ] MP3 downloads and plays
- [ ] A test Shortcut can pick up the downloaded video and **save it to Photos** (the key requirement — not yet confirmed by any source)
- [ ] If ffmpeg is missing: fall back to pre-merged formats (`-f best`, usually ≤720p) and M4A instead of MP3, and note it
- [ ] If saving to Photos or running from the share sheet doesn't work: stop and switch to the fallback (see Risks)

**Dependencies:** None (independent of Android). Needs your dad's or nephew's iPhone — Shortcuts
can only be built on an iPhone/iPad, not on the S24.

**Files likely touched:** `tasks/ios-setup.md` (exact commands that worked)

**Estimated scope:** Small

### Task 11: Share-sheet Shortcut with quality menu → video to Photos, MP3 to Files

**Description:** Build an iOS Shortcut (in Apple's Shortcuts app) with "Show in Share Sheet"
turned on. It receives the shared link, shows a menu, runs the matching yt-dlp command in
a-Shell, then **saves videos to Photos** (Shortcuts' "Save to Photo Album" action) and MP3s to
Files, and deletes the leftover copy. Build it once on one iPhone, then share it to the other
iPhone with an iCloud link (one tap to install). One-time setup per iPhone (done by Saheed):
install a-Shell, run the pip command, add the Shortcut, pin it via Edit Actions → Favourites.

**Engine updates (decision 14):** before each download, the Shortcut reads a small text file
holding the last-updated date; if it's more than 7 days old, it runs `pip install -U yt-dlp` first
and writes today's date. The menu's **Update downloader** item forces an update on demand.

Note — difference from Android: Shortcuts can't easily read yt-dlp's format list, so the menu is
a **fixed list** (Best, 1080p, 720p, 480p, 360p, MP3). If a video doesn't have the picked quality,
yt-dlp falls back to the nearest lower one (format string `bestvideo[height<=720]+bestaudio/best[height<=720]`).
Showing only the qualities that exist is possible but much more complex in Shortcuts.

**Acceptance criteria:**
- [ ] Shortcut appears in the iPhone share sheet when reached from the YouTube, Instagram and Facebook apps (via their "More" / "Share to…" option)
- [ ] Picking a quality → video shows up in the **Photos** app
- [ ] Picking MP3 → MP3 in Files app
- [ ] Your dad can do it alone after one demo (the real test)

**Verification:**
- [ ] Manual: share from each app, check result

**Dependencies:** Task 10

**Files likely touched:** `tasks/ios-setup.md` (step-by-step Shortcut build + iCloud share link)

**Estimated scope:** Small

---

## Phase 5: UI redesign and new features (Android) — agreed 2026-10-27

Saheed picked these after the first successful downloads on the S24.
- **Look:** Material 3 dynamic colour (wallpaper colours), with cards, icons and quality badges.
- **Layout:** bottom tabs, Home / Downloads / Settings.
- **New dependency:** Coil 3 for thumbnails. Icons are a few Material vector drawables in `res/drawable`, not the large icons library.
- **Tabs:** a plain tab index, no navigation library.

| Task | What | Acceptance | Verify on the S24 |
|------|------|------------|-------------------|
| U1 | Bottom tabs; Home (link box, Paste button, "copied link found" card, tip); Settings (engine card, app version); one shared picker bottom sheet for Home and Share | Tabs switch; a copied link is offered when the app opens; Paste fills the box; Find qualities opens the sheet; engine update still works | Copy a YouTube link, open the app, tap "Use this link" |
| U2 | Picker redesign: thumbnail, title, channel, duration; Video/Audio sections; badges Best/4K/2K/HD/SD; error card with Try again | Share from YouTube shows the preview and badges; airplane mode → error card; Try again works | Share a video and a Short |
| **Checkpoint** | Saheed reviews the new look | | |
| U3 | Download records (JSON file) + Downloads tab: live %, Cancel, Open, Share, Retry, Remove from list | Records survive closing the app; every action works | Download, cancel one, retry a failed one |
| U4 | Notification actions: Share on finished, Retry on failed | Both buttons work from the notification shade | |
| **Checkpoint** | Saheed reviews Downloads | | |
| U5 | Subtitles chip (remembered), embeds subtitles into the MP4 | Player can switch subtitles on | Video with captions |
| U6 | Clip chip: start/end time, exact cuts (`--download-sections` + `--force-keyframes-at-cuts`) | 1:00–1:30 gives a ~30 s file; MP3 clip works | |
| U6 fix | Clip + Subtitles: yt-dlp doesn't cut subtitles to the clip and ffmpeg's shift options gave wrong times (tested on the PC). Saheed chose: no subtitles on clips | Subtitles chip greyed out while Clip is on, with a short note | Turn Clip on in the picker |
| U6b | Change quality: on a running card (⋮) and on cancelled/failed cards (⋮, next to Retry); opens the quality sheet with the same Subtitles/Clip choices, and the new pick replaces the old card | Running download stops and a new card with the new quality starts; failed notification cleared | Start 1080p, change to 480p |
| U7 | Playlist links: header with video count, fixed quality list, videos download two at a time (max 100) | 3–5 video playlist lands in Downloads, each shows in the tab | |
| **Checkpoint** | Saheed reviews extras | | |
| U8 | Wrap-up: tests, lint, test-matrix rows, docs | All green, installed | |

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| YouTube changes its site and downloads break | High | Task 7 in-app yt-dlp update; this is normal for every downloader site too |
| Instagram / Facebook increasingly require login even for "public" posts | High | Task 8 shows a clear message. Adding login (cookies) is possible later but is a bigger feature — not in scope unless you approve |
| Large APK (~60–100 MB) from bundled Python + ffmpeg | Low | Personal use, so size matters little; arm64-only build (decision 12) |
| a-Shell lacks ffmpeg on iOS | Med | Task 10 checks first; fallback to pre-merged ≤720p + M4A |
| a-Shell route can't save to Photos / run from the share sheet | High | **Fallback:** a small server (could be Spring Boot) running yt-dlp on a home PC; the Shortcut sends it the link, gets the video back, saves to Photos. Phones then need only the Shortcuts app — but the PC must be on and reachable. Only built if Task 10 fails. |
| yt-dlp on the iPhones goes stale (dad won't update it) | Med | Shortcut auto-updates weekly; "Update downloader" menu item as fallback (decision 14) |
| Terms of Service | Low | YouTube's ToS disallows downloading outside its own features; personal use, not published |

## Decisions recorded (from Saheed, 2026-10-23)

1. iOS wanted, no Mac → Shortcut + a-Shell (decision 9); PWA ruled out (decision 10)
2. Quality list: like YouTube — show all available qualities, pick one (Task 4)
3. MP3 → Downloads now; save-folder setting later (decision 11, Future)
4. Download history → Future, after downloads work
5. Personal use only, not published (decision 8)
6. Phones ≤4 years old (Samsung S24) → arm64-only build (decision 12)
7. Real users are dad + nephew on iPhone; videos must land in Photos (decision 13)
8. yt-dlp auto-updates weekly on both platforms, manual update as fallback (decision 14)
9. Android first (guaranteed path), then iPhone
10. GitHub for code + APK releases; no Vercel/Netlify (decision 15)

## Pending your consent
- Nothing pending. Plan ready to start at Task 1.
