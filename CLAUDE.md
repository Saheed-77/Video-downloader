# CLAUDE.md — vd app (share-to-download video downloader)

Context for Claude Code. Read this first, then `tasks/plan.md` (full plan) and `tasks/todo.md` (progress).

## What this project is

A personal-use video downloader. From YouTube, Instagram or Facebook, the user taps **Share**,
picks a quality (or MP3), and the file downloads to the phone. All downloading runs **on the
phone** with **yt-dlp** + **ffmpeg** — no server.

- **Who it's for:** Saheed's dad and nephew (iPhones, not technical) and Saheed himself (Samsung S24, Android).
- **Core goal for the iPhones:** Share → More → Download Video → pick quality → video lands in the **Photos** app. No copy-pasting links into websites, no digging through the Files app.
- **Personal use only.** Not published to Play Store, App Store or any website.

## Two parts

| Part | What it is | Status |
|------|------------|--------|
| **Android app** (Phases 1–3, built first) | Native Kotlin + Jetpack Compose app using `youtubedl-android`. Appears in the share sheet, lists the qualities the video actually has (like YouTube's menu) + MP3, downloads in the background, saves to Downloads. | Not started — next is **Task 1** |
| **iPhone** (Phase 4) | **Not an app.** An iOS Shortcut (shown in the share sheet) that runs yt-dlp inside the free **a-Shell** App Store app, then saves the video to Photos. No Mac needed. | Not started — Task 10 is a feasibility check |

## How to work with Saheed

- **Keep implementations simple.** Prefer the plainest working approach over clever or "complete" ones.
- **Flag suggestions for his consent before applying them.** Anything beyond the agreed plan (extra features, refactors, new libraries, changed defaults) gets proposed first, not done.
- **Name the industry term** when introducing a pattern or component (e.g. "intent filter", "scoped storage", "foreground worker") with a one-line plain explanation — he's a recent CS graduate new to industry jargon.
- **Background:** strong in Java, learning Spring Boot; **new to Kotlin and Android**. Briefly explain Kotlin/Compose idioms the first time they appear.
- **One task at a time** from `tasks/todo.md`, in order. Meet the task's acceptance criteria, run its verification, tick it off in `tasks/todo.md`, commit, then stop at each **Checkpoint** for his review.
- Before starting any *new* project or major upgrade, ask whether a new git repo should be initialised (this repo already is).

## Decisions already made (don't reopen without asking)

Full reasoning in `tasks/plan.md` → "Architecture Decisions" (numbered 1–15).

- Android: **Kotlin + Jetpack Compose**, native. Not Flutter / React Native.
- Engine: **`youtubedl-android` 0.18.x** — Maven Central `io.github.junkfood02.youtubedl-android:library` and `:ffmpeg`.
  - Init in the `Application` class: `YoutubeDL.getInstance().init(context)` and `FFmpeg.getInstance().init(context)` (try/catch `YoutubeDLException`).
  - Manifest: `android:extractNativeLibs="true"`. Build `abiFilters` = **`arm64-v8a` only** (all target phones ≤4 years old).
- Share sheet: `ShareActivity` with intent filter `ACTION_SEND` + `text/plain`; extract the first `http(s)://` URL with a regex.
- Background downloads: **WorkManager** foreground worker with a progress notification + Cancel.
- Saving: download into app cache → copy to public **Downloads/** via **MediaStore** (scoped storage). MP3s also go to Downloads.
- yt-dlp updates: **automatic once a week** (check a saved date when the app opens / a link is shared) + **manual "Update downloader engine" button** as fallback. Source: `updateYoutubeDL(context, STABLE)` (yt-dlp's official GitHub stable releases).
- Min SDK 24. Install as a debug APK on the phone (sideloading). No signing setup, no store.
- Hosting: **GitHub only** (code + APKs in GitHub Releases). No Vercel/Netlify — not needed and unsuitable.
- iPhone: Shortcut + a-Shell. **No PWA** (can't run yt-dlp, blocked by CORS, can't appear in the iOS share sheet). Videos → Photos, MP3s → Files. Fixed quality menu (Best/1080p/720p/480p/360p/MP3/Update downloader).
- iPhone fallback (only if Task 10 fails): small home-PC server (could be Spring Boot) running yt-dlp; Shortcut sends the link, saves the result to Photos.

## Useful yt-dlp format strings

- Video at a chosen height, merged to MP4: `-f "bestvideo[height<=720]+bestaudio/best[height<=720]" --merge-output-format mp4`
- Best overall MP4: `-f "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best" --merge-output-format mp4`
- MP3: `-x --audio-format mp3 --audio-quality 0`

## Out of scope for now (future, after downloads work)

- In-app download history screen
- Settings option to choose the save folder
- Login/cookies for Instagram/Facebook posts that require sign-in

## Environment

- Saheed develops on **Windows**. Android work needs **Android Studio** and the **Samsung S24 connected over USB** (USB debugging on) to install and test — the emulator alone isn't enough (Task 1 must pass on a real phone).
- iPhone work (Tasks 10–11) has to be done **on the dad's/nephew's iPhone** in Apple's Shortcuts app; it can't be built from Windows.

## Files

- `tasks/plan.md` — full plan: UI flows (Android + iPhone), decisions, every task with acceptance criteria + verification, risks.
- `tasks/todo.md` — checklist; tick items as they finish.
