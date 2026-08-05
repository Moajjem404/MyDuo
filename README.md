<p align="center">
  <img src="https://raw.githubusercontent.com/Moajjem404/MyDuo/main/img/banner.png" alt="MyDuo Banner" width="100%">
</p>

<h1 align="center">MyDuo</h1>

<p align="center">
  <b>A Private & Secure Real-Time Activity Sharing App for Couples</b><br>
  <i>Open Source • Serverless • Privacy First</i>
</p>

<p align="center">
  <a href="https://github.com/Moajjem404/MyDuo/releases">
    <img src="https://img.shields.io/badge/version-0.4-ff4081?style=for-the-badge&logo=android" alt="Version">
  </a>
  <a href="https://github.com/Moajjem404/MyDuo">
    <img src="https://img.shields.io/badge/open_source-GitHub-181717?style=for-the-badge&logo=github" alt="GitHub">
  </a>
  <a href="#privacy">
    <img src="https://img.shields.io/badge/privacy-100%25_serverless-2e002b?style=for-the-badge&logo=shield" alt="Privacy">
  </a>
</p>

---

## Download

Get the latest APK directly from GitHub Releases.

<p align="center">
  <a href="https://github.com/Moajjem404/MyDuo/releases/download/0.4/app-release.apk">
    <img src="https://img.shields.io/badge/download_apk-v0.4-ff4081?style=for-the-badge&logo=android&logoColor=white" height="50" alt="Download APK">
  </a>
</p>

> [!IMPORTANT]
> Uninstall any previous version before installing the latest APK.

---

## Features

- 🔒 **Private & Secure** – No central servers, no cloud databases.
- 🤖 **Telegram‑Powered** – Uses your personal Telegram bot for all communication.
- ⚡ **Real‑Time Sync** – Activity updates appear instantly for both partners.
- 🔗 **Connection Code** – Pair devices instantly by sharing a simple code.
- 📲 **One‑Tap Quick Ping** – Send a quick “I’m thinking of you” with one tap.
- 🌐 **Offline Resilience** – Activities sync automatically when back online.
- 🧹 **Minimal Storage** – Keeps only the latest 10 activities per device.
- 🎨 **Modern UI** – Clean, Material Design 3 interface.
- 🪶 **Lightweight** – Optimised for performance and battery.
- 📖 **100% Open Source** – Study, modify, and contribute.

---

## What’s New in v0.4

- ✨ Connection Code Sharing – pair in seconds.
- 🔔 Quick Ping – instant one‑tap notification.
- 🎨 Refreshed user interface.
- ⚡ Performance improvements.
- 🐛 Numerous bug fixes.
- 🛡️ Smoother, more stable experience.

---

## How It Works

All messages travel through **your own Telegram bot** – no external servers, no third parties.

---

## Architecture

```
┌──────────────┐
│  Your Phone  │
└──────┬───────┘
       │
       │ HTTPS
       │
┌──────▼───────┐
│ Telegram Bot │
└──────┬───────┘
       │
       │ HTTPS
       │
┌──────▼────────┐
│ Partner Phone │
└───────────────┘
```

No dedicated backend server is required.

---


Your data never leaves your Telegram account. You remain in full control.

---

## Quick Setup

### 1. Create a Telegram Bot
- Open [@BotFather](https://t.me/BotFather) on Telegram.
- Follow the instructions to create a new bot.
- Copy the **Bot Token** you receive.

### 2. Get Your Chat ID
- Open [@MissRose_bot](https://t.me/MissRose_bot) on Telegram.
- Send the command `/id`.
- Copy the **Chat ID** shown.

### 3. Configure MyDuo
- Launch MyDuo on your device.
- Enter your **Bot Token** and **Chat ID** in the setup screen.

### 4. Connect Your Partner
- Share your **Connection Code** (generated in the app) with your partner.
- Your partner pastes the code in their app.
- Pairing is instant – you’re ready to share activities.

---

## Privacy

MyDuo is built with privacy at its core:

- ❌ No cloud storage
- ❌ No central database
- ❌ No account registration
- ❌ No tracking or analytics
- ❌ No advertisements

Everything stays strictly between you and your partner, routed through your own Telegram bot.


## Support

Found a bug or have a suggestion? I’d love to hear from you.

[![Telegram](https://img.shields.io/badge/Telegram-@Moajjem404-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/Moajjem404)  
[![GitHub Issues](https://img.shields.io/badge/GitHub-Issues-181717?style=for-the-badge&logo=github)](https://github.com/Moajjem404/MyDuo/issues)

---

## Contributing

Contributions are warmly welcomed! Feel free to:

- Fork the repository
- Improve the codebase
- Fix bugs
- Suggest new features
- Open a pull request

Let’s build something great together.

---

## License

This project is open source. You are free to study, modify, and distribute it. No restrictive license – built for the community.

---

<p align="center">
  Made with ❤️ by <b>Moajjem</b><br>
  <a href="https://github.com/Moajjem404">GitHub</a>
</p>
