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

## 4. If something fails

- Engine/download errors: try **Update downloader engine** first, then retry.
- Crash or "Download failed" with no clear reason: Android Studio → **Logcat**, filter `package:com.saheed.videodownloader`. Copy the red lines for Claude.
