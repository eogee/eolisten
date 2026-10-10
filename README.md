# EoListen · Yueji

[English](README.md) | [简体中文](README.zh-CN.md)

> Preserve every detail in every second of sound — audio transcription, speaker separation, AI meeting minutes, hotword library, voice input

EoListen is a cloud speech-to-text service with one account across three apps:

- **Web** ([eogee.com/listen](https://eogee.com/listen/)): upload audio/video for transcription, meeting workspace (speaker naming + AI minutes & action items), video subtitles, hotword library
- **Android**: one-tap meeting recording with a native foreground service — records with the screen off, auto-pauses during phone calls; ships with a built-in voice-input keyboard (9-key or full layout, hold to talk)
- **Windows**: the transcription workspace as a desktop app with a **built-in voice input** — hold Left Ctrl + Left Alt, speak, and the text lands in any app

One account across all three: shared quota (15 min/day free, 60 min/day premium), hotwords and tasks sync in real time.

## Download & Update

Grab the latest Windows installer and Android APK from the **[Releases](releases)** page.

### What's new in v0.3.5

- New: Phone as microphone — pair your phone as the pickup for a PC without a mic over LAN (QR or manual pairing); recognition runs on the PC and audio never leaves your local network
- New: Never lose speech — failed recognitions are stashed and retried automatically on both desktop and Android, delivered within 10 minutes; the Android voice panel is now tap-to-talk (tap to start, tap again to stop)
- Improved: Smarter punctuation for choppy speech — periods land only at semantic boundaries, short pauses no longer shatter sentences
- Desktop: optional tray toggle to strip filler words from recognition

Full notes for every version live on the [Releases](releases) page. Apps check for updates on launch: the web app is always current, while Android and Windows prompt you when a new build ships.

## Privacy

- Source audio is deleted right after transcription; only the text result is kept
- Hotword material is used only to extract candidates and discarded immediately
- AI-summary reference material is used only during summarization and deleted afterwards
- Web recordings stay in your own browser

## Contact

- **Website**: <https://eogee.com> · **Product & downloads**: <https://eogee.com/listen/app/>
- **QQ**: 3886370035 · **WeChat**: eogee2022
- **Feedback**: the "Feedback" entry in the app footer (screenshots supported) reaches the developer at eogee@qq.com
