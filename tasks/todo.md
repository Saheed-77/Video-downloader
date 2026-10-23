# Todo — Share-to-Download Video App (personal use)

See `tasks/plan.md` for details, acceptance criteria and verification for each task.

## Phase 1: Foundation (Android)
- [ ] Task 1: Project skeleton + yt-dlp/ffmpeg initialise on device (S) — deps: none
- [ ] Task 2: Paste a URL → download best MP4 into app storage (S) — deps: 1
- [ ] **Checkpoint:** real phone install, hardcoded URL downloads and plays, review with Saheed

## Phase 2: Core features (Android)
- [ ] Task 3: App appears in the share sheet and reads the link (S) — deps: 2
- [ ] Task 4: Quality picker — all available resolutions (like YouTube) + MP3 entry (M) — deps: 3
- [ ] Task 5: Background download + progress notification, saved to Downloads (M) — deps: 4
- [ ] Task 6: MP3 audio download, saved to Downloads (S) — deps: 5
- [ ] **Checkpoint:** share from YouTube → 720p MP4 and MP3 both land in Downloads and play; download survives leaving the app; review

## Phase 3: Robustness (Android)
- [ ] Task 7: yt-dlp auto-update weekly + manual update button (S) — deps: 6
- [ ] Task 8: Friendly errors + cancel download (S) — deps: 6
- [ ] Task 9: Real-site test matrix + install on your phone (S) — deps: 7, 8
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

## Next
- [ ] Task 1 — needs Android Studio on Windows and the S24 connected over USB (USB debugging on)
