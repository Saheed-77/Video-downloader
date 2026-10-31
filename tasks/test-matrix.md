# Android test matrix (Task 9)

Fill this in on the Samsung S24. Mark each cell ✅ / ❌ and write any error message shown.

## 1. Install

1. Phone: Settings → Developer options → **USB debugging** on. Plug it into the PC and accept the "Allow USB debugging?" prompt.
2. Android Studio: open the `vd app` folder. Wait for "Gradle sync" to finish, pick the S24 in the device list at the top, and press **Run ▶**.
   - Or from a terminal in the project folder: `gradlew.bat installDebug`
3. On first open, allow **notifications** when asked.
4. The main screen should show *Downloader engine: yt-dlp version …* and, after a few seconds, a "Last updated" date. That's the first automatic engine update.

## 2. Share flow

Share from each app → **Video Downloader** → pick the quality → wait for the notification.

| # | Source | Quality picked | Qualities listed (correct? only real ones?) | Notification progress → "Download complete" | File in Downloads, plays with sound | Shows in Gallery | Notes |
|---|--------|---------------|---------------------------------------------|---------------------------------------------|-------------------------------------|------------------|-------|
| 1 | YouTube app: normal video | 720p | | | | | |
| 2 | YouTube app: same video | MP3 audio | | | | (n/a) | |
| 3 | YouTube app: Short (vertical) | Best quality | | | | | |
| 4 | YouTube app: 4K video | 1080p | | | | | |
| 5 | Instagram app: public reel | Best quality | | | | | |
| 6 | Facebook app: public video | Best quality | | | | | |
| 7 | Other site you care about: | | | | | | |

## 3. Behaviour checks

| # | Check | How | Result |
|---|-------|-----|--------|
| A | Download keeps going in the background | Start a long video (10+ min), go back to YouTube / lock the phone, wait | |
| B | Cancel | Tap **Cancel** on the progress notification mid-download → notification disappears, no file in Downloads | |
| C | No internet | Airplane mode → share a link → the picker should say "No internet connection" | |
| D | Private / removed video | Share a private or deleted video link → readable message, no crash | |
| E | Not a link | Share plain text (e.g. from Notes) → "No link found" | |
| F | Paste box | Open the app icon → paste a link → Find qualities → pick → notification | |
| G | Update button | Tap **Update downloader engine** → "Already up to date" (or "Updated to …") | |
| H | Tap "Download complete" | Opens the video/MP3 in a player | |
| I | Same video twice | Second file is saved as "title (1).mp4", not overwritten | |

## 3b. Phase 5 checks (new look and features)

Install over **wireless debugging** (PC hotspot) or Android Studio **Run ▶**; USB debugging is unstable on this PC.

| # | Check | How | Result |
|---|-------|-----|--------|
| P1 | Copied link card | Copy a YouTube link, open the app → "Copied link found" → **Use this link** opens the quality list | |
| P2 | Paste / clear | Home: the clipboard icon pastes; the ✕ clears | |
| P3 | Picker preview | Share a video: thumbnail, title, channel, duration, badges (Best/4K/2K/HD/SD) and sizes | |
| P4 | Try again | Airplane mode → share → red card → Wi-Fi on → **Try again** lists qualities | |
| P5 | Downloads tab live | Start a download → Downloads tab shows the % and a moving bar; tab badge shows 1 | |
| P6 | Cancel / Retry | ✕ on a running card → "Cancelled" → **Retry** starts it again as a new card | |
| P7 | Open / Share / Remove | Tap a finished card plays it; share icon → WhatsApp; ⋮ → Remove from list | |
| P8 | List survives restart | Close the app fully, reopen → the list is still there | |
| P9 | Notification buttons | "Download complete" → **Share**; "Download failed" (airplane mode mid-download) → **Retry** | |
| P10 | Subtitles | TED talk, Subtitles on, 720p → player shows English subtitles (CC menu) | |
| P11 | Clip | Clip on, 1:00 → 1:30 → a 30-second file; Subtitles chip greyed out while Clip is on | ✅ 28 Oct: Subtitles chip greyed with "Subtitles aren't available for clips" |
| P12 | Clip errors | Start after End, or past the video length → red message, quality rows greyed out | |
| P13 | Change quality (running) | Start 1080p → Downloads ⋮ → **Change quality** → 480p: old card gone, new 480p card runs | ✅ 28 Oct: 1080p → 144p while running; 1080p card gone, 144p finished (10 MB) |
| P14 | Change quality (stopped) | Cancel one → ⋮ → **Change quality**: Clip times and Subtitles come back filled in | ✅ 28 Oct: cancelled 360p → 144p; Subtitles setting kept, card replaced |
| P15 | Playlist | Share a YouTube **playlist** link (youtube.com/playlist?list=…) with 3–5 videos → "N videos" header → pick 720p → videos download two at a time, each with its own card | ✅ 28 Oct: TEDx "Celebrating planet Earth", 6 videos at 360p, one after another (first: 22 MB) |
| P16 | Playlist: cancel one video | ✕ on one running playlist video → only that one is Cancelled, the next waiting one starts | ✅ 28 Oct: "How agri-robotics" cancelled, "The immortal naturalist" started, the rest kept waiting |
| P17 | Video inside a playlist | Share a video opened from inside a playlist (watch?v=…&list=…) → just that one video, as before | |
| P18 | Cancel all / Retry all | Downloads tab buttons: Retry all restarts every failed/cancelled one, two at a time; Cancel all stops every running/waiting one | ✅ 28 Oct: 5 cancelled → Retry all (1 running, 4 waiting) → Cancel all (all Cancelled) |
| P19 | Download another quality | ⋮ on any card → **Download another quality** → pick: a new card starts, the original stays | ✅ 28 Oct: finished 360p → new 720p card, 360p kept |
| P20 | Parallel downloads | Start two separate downloads → both run at once, each with its own notification | ✅ 28 Oct: two 1080p TEDx talks at the same time (27% / 29% …); Cancel all stopped both |
| P21 | Playlist section | Share a playlist → a section with its name and "6 videos · 2 downloading · 4 waiting"; tap the name to hide/show | ✅ 28 Oct: "Celebrating planet Earth" section; older playlists show as "Playlist" |
| P22 | Stop / Resume | Section **Stop** → all "Stopped", nothing downloads; **Resume** → 2 start again | ✅ 28 Oct: 5 stopped + 1 cancelled, no progress notifications; Resume → 2 downloading, 3 waiting |
| P23 | Cancel / Retry playlist | Section **Cancel** → all Cancelled; section **Retry** → failed/cancelled wait again in place, 2 at a time, with pauses | ✅ 28 Oct: 5 failed videos retried in place, all done, yt-dlp "Sleeping 5–9 s"; Cancel → 6 cancelled |
| P24 | Retry one playlist video | **Retry** on one failed/cancelled video in a section → it restarts in the same place | |
| P25 | Clear menu | ⋮ next to "Downloads" → Clear finished / failed and cancelled / all except running; files stay in Downloads | ✅ 28 Oct: "Clear failed and cancelled" removed the cancelled playlist (26 → 18 rows), finished ones stayed |
| P26 | Swipe to remove | Swipe a finished card left → row removed; running cards can't be swiped | ✅ 28 Oct after a fix (the next card had inherited the swiped state inside a section) |

## 3c. Next features (F1–F7)

| # | Check | How | Result |
|---|-------|-----|--------|
| F1 | H.264 video | Download a YouTube video at 720p → file is H.264 + AAC (plays on old phones/WhatsApp) | |
| F2 | MP3 cover + tags | Download MP3 → Samsung Music shows the thumbnail as cover, title and artist | |
| F3 | Speed / time left | Card and notification show "46% · 3.4 MB/s · 0:12 left" | |
| F4 | Wi-Fi only / wait | Settings → Wi-Fi only on, Wi-Fi off → "Waiting for Wi-Fi…", starts when Wi-Fi is back. Airplane mode mid-download → "Waiting for internet…", then continues | |
| F5 | Search | Downloads tab → type part of a title → list and sections filter | |
| F6 | Default quality | Settings → When sharing a link → 720p; share from YouTube → sheet closes by itself, 720p download starts | |
| F7 | Instagram/Facebook login | Settings → Accounts → Sign in (you type your details) → Done → "Signed in"; share a login-only reel → downloads; Sign out → "Not signed in" | |
| S1 | Reddit | Share a Reddit video post → downloads with sound | |
| S2 | X/Twitter | Share an X video → downloads | |

## 4. If something fails

- Engine/download errors: try **Update downloader engine** first, then retry.
- Crash or "Download failed" with no clear reason: Android Studio → **Logcat**, filter `package:com.saheed.videodownloader`. Copy the red lines for Claude.
