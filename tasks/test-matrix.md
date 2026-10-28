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
| P11 | Clip | Clip on, 1:00 → 1:30 → a 30-second file; Subtitles chip greyed out while Clip is on | |
| P12 | Clip errors | Start after End, or past the video length → red message, quality rows greyed out | |
| P13 | Change quality (running) | Start 1080p → Downloads ⋮ → **Change quality** → 480p: old card gone, new 480p card runs | |
| P14 | Change quality (stopped) | Cancel one → ⋮ → **Change quality**: Clip times and Subtitles come back filled in | |
| P15 | Playlist | Share a YouTube **playlist** link (youtube.com/playlist?list=…) with 3–5 videos → "N videos" header → pick 720p → videos download one after another, each with its own card | |
| P16 | Playlist cancel | During a playlist download, ✕ on the running video → it and the waiting ones show "Cancelled" | |
| P17 | Video inside a playlist | Share a video opened from inside a playlist (watch?v=…&list=…) → just that one video, as before | |

## 4. If something fails

- Engine/download errors: try **Update downloader engine** first, then retry.
- Crash or "Download failed" with no clear reason: Android Studio → **Logcat**, filter `package:com.saheed.videodownloader`. Copy the red lines for Claude.
