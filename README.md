<p align="center">
  <img src="app/assets/icons/logo.png" width="120" height="120" alt="JayDesk Logo" style="border-radius: 28px;">
</p>

<h1 align="center">JayDesk Enhanced</h1>

<p align="center">
  <b>Turn any Android phone into a high-performance Linux desktop workstation.</b><br>
  Not an emulator. Not VNC. Native kernel execution with hardware-accelerated X11 rendering.
</p>

<p align="center">
  <a href="https://github.com/techjarves/JayDesk/releases/latest"><img src="https://img.shields.io/github/v/release/techjarves/JayDesk?style=for-the-badge&color=6366F1" alt="Latest Release"></a>
  <a href="https://github.com/techjarves/JayDesk/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-10B981?style=for-the-badge" alt="License"></a>
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20ARM64-38BDF8?style=for-the-badge" alt="Platform">
</p>

---

## Overview

**JayDesk Enhanced** brings full Linux desktop capability directly to your Android device with advanced customization, dynamic light/dark themeing, and modern edge-to-edge UI support. Connect your phone to any monitor via USB-C or wireless bridge, and it transforms instantly into a desktop computer running real Linux applications—from **VS Code** and **LibreOffice** to **Wireshark**, **Metasploit**, and offline **Local AI** models.

Unplug your phone, and your entire workstation stays with you.

---

## Key Features

- **Direct Kernel & Hardware Acceleration**: Uses Turnip & Zink Vulkan drivers for Snapdragon / Adreno GPUs with fallbacks for Mesa software rendering.
- **Adaptive Light & Dark Theme System**: Seamlessly switch between dark and light modes live across all screens, cards, and modal sheets.
- **Pre-Installation Onboarding Theme Switcher**: Toggle your preferred aesthetic right from the initial setup screens before installation.
- **Edge-to-Edge System Bar Integration**: Clean, transparent Android status bar overlays with adaptive icon brightness on Android 11 through Android 15.
- **One-Tap Desktop Essentials**: Automated installation of **XFCE4**, **LXQt**, **MATE**, or **KDE Plasma** desktop environments.
- **AI Coding Agents Built In**: **Hermes Agent**, **OpenClaude**, and **Antigravity CLI** are installed inside the Proot Linux container with desktop-menu launchers, pre-configured for Google Gemini.
- **Dual Architecture**: Supports **Rooted Chroot** (Ubuntu 24.04 LTS) and **Non-Rooted Native Termux/TUR** userspaces.
- **Embedded X11 Server**: Renders through an embedded Termux:X11 display server directly on `DISPLAY=:0` without VNC lag.

---

## What You Can Run

| Category | Supported Tools |
|---|---|
| **Development** | Full VS Code (Python, Node.js, C++, Extensions), Git, Claude Code, Vim, Neovim |
| **AI Coding Agents** | Hermes Agent (Nous Research), OpenClaude, Antigravity CLI (`agy`) — Gemini-powered, menu launchers included |
| **Productivity** | LibreOffice Suite (Writer, Calc, Impress), Firefox, Chromium |
| **Security & Auditing** | Wireshark, Metasploit Framework, Nmap |
| **Media & AI** | Blender (3D modeling), Local Offline LLMs (Ollama / Llama.cpp), GIMP |

---

## AI Agents (inside the Linux desktop)

The setup script installs three AI coding agents **inside the Proot Ubuntu container** and adds them to your desktop menu (Development category), so they launch right from the Linux desktop:

| Agent | Command | Default model |
|---|---|---|
| **Hermes Agent** (Nous Research) | `hermes` | Gemini 3.5 Flash Lite (`~/.hermes/config.yaml`) |
| **OpenClaude** | `openclaude` | Gemini 3.5 Flash Lite (`--model gemini-3.5-flash-lite`) |
| **Antigravity CLI** (Google) | `agy` | Gemini API mode — pick the model inside the app |

**API key:** During setup you're asked for a Google Gemini API key (input is hidden). It's stored only in the Proot user's `~/.hermes/.env` (permissions `600`) and loaded into your shell from there. Skipped it? Add it later:

```bash
bash ~/start-proot.sh        # enter the container
su - <your-desktop-username> # switch to the user who owns the agents
nano ~/.hermes/.env          # GOOGLE_API_KEY=... and GEMINI_API_KEY=...
```

**Notes:**
- OpenClaude needs Node.js ≥ 22, so the setup installs Node 22 (NodeSource) inside Proot first — Ubuntu 22.04's stock Node is too old.
- Installers: Hermes via `hermes-agent.nousresearch.com/install.sh`, OpenClaude via `npm install -g @gitlawb/openclaude`, Antigravity via `antigravity.google/cli/install.sh` (ARM64 build, runs under glibc in Proot).
- Antigravity's model list is controlled by Google in the `agy` app; the setup points it at your Gemini API key (`modelProvider: gemini` in `~/.gemini/antigravity-cli/settings.json`).

## Quick Start

### Option A: Standalone Android App (Recommended)

1. Download the latest compiled **[Release APK](https://github.com/techjarves/JayDesk/releases/latest)**.
2. Install the APK on your Android phone (ARM64, Android 8.0+).
3. Select your theme (Light/Dark) on the onboarding screen.
4. Pick your desktop environment (XFCE4, LXQt, MATE, or KDE Plasma) and tap **Install Essentials**.

### Option B: Manual Termux Setup Script

If you prefer installing inside an existing Termux terminal environment:

```bash
curl -sL https://raw.githubusercontent.com/orailnoor/JayDesk/main/termux-linux-setup.sh -o setup.sh
bash setup.sh
```

Launch the desktop:
```bash
bash ~/start-x11.sh
```

---

## Display & Monitor Output

- **Direct USB-C Display**: Connect a USB-C to HDMI adapter directly to phones supporting DisplayPort Alt mode.
- **Raspberry Pi Bridge**: Use a Raspberry Pi Zero 2W connected via USB tethering to mirror the desktop to any HDMI monitor.

---

## Credits & Acknowledgments

JayDesk is built on the incredible work of the open-source Linux and Android community:

- **Original Creator & Architect**: **[orailnoor](https://youtube.com/@orailnoor)** ([GitHub: @orailnoor](https://github.com/orailnoor/JayDesk))
  *Designed the original JayDesk Linux setup scripts, Termux integration, embedded X11 architecture, and standalone app core.*

- **Customizations & Enhancements**: **[techjarves](https://github.com/techjarves/JayDesk)**
  *Implemented the dynamic Light & Dark theme engine, pre-installation onboarding theme selector, status bar edge-to-edge transparent integration, high-contrast terminal bottom sheet, UI contrast overhauls, and distribution releases.*

- **Upstream Open Source Projects**:
  - [Termux](https://github.com/termux/termux-app) & [Termux:X11](https://github.com/termux/termux-x11)
  - [Termux User Repository (TUR)](https://github.com/termux-user-repository/tur)
  - [Ubuntu / Canonical](https://ubuntu.com)

---

## License & Legal Notice

JayDesk is independent open-source software licensed under the **[GNU General Public License v3.0](LICENSE)**. 

> [!NOTE]
> JayDesk is an independent project and is not affiliated with or endorsed by Termux, Termux:X11, TUR, Canonical, or Ubuntu. All trademarks belong to their respective owners.
