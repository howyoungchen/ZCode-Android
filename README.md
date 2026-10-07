<div align="center">

# Zemote

**Control desktop ZCode from your phone**

[![Release](release-shield)][release-url]
[![Downloads](downloads-shield)][downloads-url]
![License](license-shield)
![Platform](platform-shield)

**[Download APK](https://github.com/howyoungchen/ZCode-Android/releases/latest)** ·
**[Live preview](https://howyoungchen.github.io/ZCode-Android/)** ·
**[Changelog](CHANGELOG.md)**

English · [简体中文](README_CN.md)

Native Android client · Kotlin + Jetpack Compose (Material 3) · independent implementation
of the official remote-control protocol

</div>

Zemote reverse-engineers the communication protocol of the official ZCode web
remote-control page and brings the whole experience natively to Android: streaming
chat, task panel, permission approvals and attachment uploads — no browser required.
It is a pure client that talks only to the official Relay and collects no data.

<p align="center">
  <img src="screenshots/step1_home.png" width="30%" alt="Remote-control dashboard with tasks grouped by workspace" />
  <img src="screenshots/step3_add_device.png" width="30%" alt="Add a device via QR scan or pasted link" />
  <img src="screenshots/home_dark.png" width="30%" alt="Dark mode" />
</p>

## About this fork

This repository is a fork of
[Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android) — many
thanks to the original author
[Damian2012](https://github.com/Damianjiang) for the protocol reverse engineering
and the overall architecture.

The mission of this fork is to **polish the UI aesthetics closer to Z.ai**: following
the official design language — a monochrome black-and-white base with sky-blue
accents — the dashboard, conversation document flow and composer are aligned
block-by-block with the official mobile remote page, not merely "similar colors".
Protocol reliability issues (cold-start session loading, streaming stalls,
reconnection, …) are also being fixed continuously — see the
[Changelog](CHANGELOG.md).

**From here on, this project is developed and maintained independently in this
repository** by [howyoungchen](https://github.com/howyoungchen).

## Features

**Connection & control**

- Pair by scanning the desktop QR code or pasting the remote-control URL; save
  multiple devices and switch with one tap
- Remote-control dashboard: task list grouped by workspace, status pills,
  one-tap new task
- Task panel: running background tasks and pending interactions, with one-tap cancel
- Permission approval: approve file access and command execution right from your phone

**Conversation**

- Streaming output: thinking, replies and tool calls render as they are generated,
  with Markdown and inline images
- One-line summary per tool call (terminal / read / search / MCP / subagent), tap to
  expand raw output
- Message queue: messages sent while the AI is busy can be sent immediately, edited,
  deleted or drag-reordered
- Switch model and thinking level, live context usage; images and files uploaded in
  chunks
- Read-only subagent sessions that restore the parent session on back

**Reliability & privacy**

- Automatic reconnect with bridge rebuild and subscription recovery — no app restart
  needed after a network switch
- Foreground keep-alive service reduces background kills
- Credentials encrypted with Android Keystore (AES/GCM) and stored only on the phone
- Zero telemetry, no requests beyond the official Relay; 中文 / English UI, light and
  dark themes

## Quick start

1. Open **Remote control** in desktop ZCode and generate a pairing QR code
2. Open Zemote on your phone, scan the code or paste the link
3. Pick a workspace, enter the task session and start chatting

## Download

- Get the latest APK from [GitHub Releases](https://github.com/howyoungchen/ZCode-Android/releases)
- Requirements: Android 9+ (API 28), arm64-v8a only
- Want a look first? Try the [live preview page](https://howyoungchen.github.io/ZCode-Android/)

## Build from source

Requires JDK 17 and Android SDK 35.

```bash
git clone https://github.com/howyoungchen/ZCode-Android.git
cd ZCode-Android
./gradlew assembleRelease   # gradlew.bat on Windows
```

The APK is written to `app/build/outputs/apk/release/`. Release builds enable R8 and
resource shrinking and are signed with the debug key, so they install directly; use
`assembleDebug` for development.

## Protocol

The stack is reverse-engineered layer by layer from the official web client,
behavior-identical but implemented from scratch:

| Layer | Notes |
|---|---|
| Relay | wss connection, 10s heartbeat, exponential-backoff reconnect |
| Pairing | HMAC-SHA256 pairing proof |
| IPC | 7-bit varint codec (String / Int / JSON / Bytes / Array) |
| RpcFrame | 512KB fragments, CRC32 verification, acks, retransmit on fault |
| Channel RPC | request/response + event subscriptions |
| Conversation V4 | snapshot & delta subscriptions, stream reassembly, message queue, attachment upload, permission handling |

The full message, field and timing spec lives in the
[protocol doc](docs/protocol.md) (Chinese).

## Project structure

A pure client, single Gradle module `:app`:

```
app/src/main/java/app/zemote/
├── protocol/    # Protocol stack: Relay, pairing, IPC codec, rpc-frame, Conversation V4
├── state/       # State layer: device persistence, credential encryption, session management
├── ui/          # Compose UI: theme, shared components, screens
├── service/     # Foreground keep-alive service
└── crash/       # Crash capture and reporting
```

See the [architecture doc](docs/architecture.md) for layering and key flow sequences.

## Documentation

All docs are currently written in Chinese:

| Doc | Contents |
|---|---|
| [Architecture](docs/architecture.md) | Modules, protocol layering, key flow sequences |
| [Protocol spec](docs/protocol.md) | Layer-by-layer spec: messages, fields, timing constants |
| [Data model](docs/data-model.md) | Local storage (DataStore / Keystore) and runtime data |
| [Runbook](docs/runbook.md) | Build, release, logging, troubleshooting |
| [ADRs](docs/adr/README.md) | Key technical decisions and their trade-offs |
| [Changelog](CHANGELOG.md) | Changes in every release |

## Community

- QQ Group [1090759263](https://qm.qq.com/q/1090759263): discussions and feedback (Chinese)
- [GitHub Issues](https://github.com/howyoungchen/ZCode-Android/issues): bug reports and feature requests

## ⚠️ Disclaimer

- Zemote is **not an official client** and has no affiliation with ZCode / Z.ai. The
  protocol comes from packet capture and reverse engineering of the official web page;
  an official update can break it at any time.
- Use it only with your own devices. Follow the terms of service and local laws; you
  assume all risk.
- The `sid` / `hash` in a pairing URL are device credentials — **never share them**.
  If leaked, regenerate the QR code on the desktop to invalidate them.
- This project collects no data; credentials are encrypted with Keystore and stored
  only on your phone.

## Acknowledgments & license

- The original project
  [Damianjiang/ZCode-Android](https://github.com/Damianjiang/ZCode-Android)
  (Damian2012) — protocol reverse engineering and overall architecture; the
  conversation protocol implementation cross-references the original Flutter version
  of that project (same wire behavior).
- During its development, AI (LLMs) assisted with parts of the original codebase; the
  overall architecture and core protocol were completed by the original author, and
  AI output was human-reviewed before inclusion.

Released under the MIT license. The ZCode name and related trademarks belong to their
respective owners; this project has no affiliation with them.

[release-shield]: https://img.shields.io/github/v/release/howyoungchen/ZCode-Android?style=flat-square&color=0ea5e9
[release-url]: https://github.com/howyoungchen/ZCode-Android/releases/latest
[downloads-shield]: https://img.shields.io/github/downloads/howyoungchen/ZCode-Android/total?style=flat-square&color=0ea5e9
[downloads-url]: https://github.com/howyoungchen/ZCode-Android/releases
[license-shield]: https://img.shields.io/badge/license-MIT-111827?style=flat-square
[platform-shield]: https://img.shields.io/badge/Android-9%2B-111827?style=flat-square&logo=android&logoColor=white
