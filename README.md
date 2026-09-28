# EoListen · Yueji

[English](README.md) | [简体中文](README.zh-CN.md)

> Preserve every detail in every second of sound — audio transcription, speaker separation, AI meeting minutes, hotword library, voice input

EoListen is a cloud speech-to-text service with one account across three apps:

- **Web** ([eogee.com/listen](https://eogee.com/listen/)): upload audio/video for transcription, meeting workspace (speaker naming + AI minutes & action items), video subtitles, hotword library
- **Android**: one-tap meeting recording with a native foreground service — records with the screen off, auto-pauses during phone calls
- **Windows**: the transcription workspace as a desktop app with a **built-in voice input** — hold Left Ctrl + Left Alt, speak, and the text lands in any app

One account across all three: shared quota (15 min/day free, 60 min/day premium), hotwords and tasks sync in real time.

## Download & Update

Grab the latest Windows installer and Android APK from the **[Releases](releases)** page.

### What's new in v0.2.1

- Login and settings unified into the web app — sign in once in the main window and the voice input inherits your session automatically
- Tray settings now opens the web settings page, with a new "Desktop" card (launch at login, voice input toggle)
- Android re-released under the unified version number (same features as 0.1.2, including auto-pause during calls)

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
