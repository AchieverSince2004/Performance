<div align="center">

# ✒️ InkCraft Studio

### A macOS-Inspired, ML-Powered Stylus Note-Taking Studio Built for Android Tablets

[![Release](https://img.shields.io/github/v/release/AchieverSince2004/Performance?style=for-the-badge&color=007AFF&label=Latest%20APK)](https://github.com/AchieverSince2004/Performance/releases/latest)
[![Platform](https://img.shields.io/badge/Android-Tablet%20%2B%20Stylus-34C759?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AchieverSince2004/Performance/releases/latest)
[![ML Kit](https://img.shields.io/badge/On--Device%20ML-Digital%20Ink%20%2B%20TTS-5856D6?style=for-the-badge)](https://developers.google.com/ml-kit/vision/digital-ink-recognition)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

**[⬇ Download Signed APK for Your Tablet](https://github.com/AchieverSince2004/Performance/releases/latest)**

</div>

---

## ✨ Why InkCraft Studio?

Most Android note-taking apps feel like blown-up phone apps with laggy webview ink and clumsy touch handling. **InkCraft Studio** was engineered from scratch for **tablet + active stylus workflows** (Samsung S Pen, USI, Lenovo Precision Pen, Xiaomi Smart Pen) — pairing **Apple/macOS-inspired studio aesthetics** with **hardware-accelerated 120/240Hz pressure ink** and **on-device machine learning**.

---

## 🎨 macOS-Grade Studio Aesthetics

- **Finder-Style Library Sidebar**: Frosted neutral sidebar (`#F0F0F5`), macOS traffic-light window accents, pill-shaped selection states, folder organization, favorites, and instant search.
- **Unified Studio Titlebar**: Crisp `1dp` hairline dividers, translucent white chrome (`#FFFFFF`), compact Undo/Redo pill, **Palm Safe** indicator, and a **1-Tap Page Lock** pill.
- **Floating Frosted Tool Dock**: Dockable to the **Top, Left, or Right** edge for effortless left-handed and right-handed ergonomics.
- **Custom 3D-Styled Notebook Covers**: 8 curated cover themes (*Classic Navy, Oxblood Leather, Botanical Sage, Amber Horizon, Architect Blueprint, Linen Cream, Carbon Studio, Iris Mist*).

---

## 🖋️ Pro Stylus Ink Engine & Rock-Solid Palm Rejection

- **Zero Accidental Page Jumps**: Tracks `TOOL_TYPE_STYLUS` / `TOOL_TYPE_ERASER` pointer indices directly and enforces a **550ms hover-and-lift cooldown** plus palm `touchMajor` filtering. Even if your palm rests on the screen while your pen writes — or shifts as you lift your hand — the page stays rock steady.
- **1-Tap Page Lock**: Lock the canvas viewport completely when taking dense lecture notes.
- **120/240Hz Historical Motion Sampling**: Processes every `MotionEvent.historySize` sample for silky-smooth curves with zero jagged corners.
- **True Pressure & Tilt Dynamics**:
  - **Fountain Pen** — Calligraphic pressure + stylus tilt nib variation
  - **Ballpoint Pen** — Crisp everyday ink with subtle pressure response
  - **Fineliner** — Uniform technical line for diagrams
  - **Brush Pen** — Expressive wide-dynamic-range pressure curve
  - **Highlighter** — Semi-transparent chisel-tip overlay
  - **Laser Pointer** — Ephemeral glowing presentation trail
- **Hardware Eraser Support**: Automatically switches to eraser on **stylus flip-eraser** (`TOOL_TYPE_ERASER`) or **pen side-button** (`BUTTON_STYLUS_PRIMARY`).
- **Hold-to-Snap Shape Recognition**: Draw a rough line, circle, ellipse, rectangle, or triangle and pause briefly to snap into a crisp geometric vector.
- **Noteshelf-Style 2.2x Zoom Box**: Magnified close-up writing strip with line-advance controls for super-neat handwriting.
- **Straightedge Ruler & Lasso Tool**: Snap strokes to an interactive ruler, or circle any strokes to move, recolor, or delete them.

---

## 🧠 On-Device AI Studio (Offline ML + Voice)

Tap **AI Studio** in the editor titlebar to run an **on-device neural pipeline** directly on your tablet:

1. **Handwriting → Digital Text (Google ML Kit Digital Ink Recognition)**: Converts all handwritten strokes on the current page into clean, editable digital text.
2. **Voice Read-Aloud (Text-To-Speech)**: Listen to your handwritten notes spoken aloud at `0.85x`, `1.0x`, or `1.25x` speed — great for proofreading essays or reviewing study notes hands-free.
3. **Smart Insights & Action-Item Detector**: Automatically computes word count, extracts key bullet points, and detects `TODO` / deadline action items.
4. **1-Tap Actions**: Copy converted text to clipboard or insert it directly onto the canvas as a **Sticky Note**.

---

## 📄 Templates, PDFs & Productivity

- **10 Paper Templates**: *Blank, Ruled Wide, Ruled Narrow, Cornell Notes, Dot Grid, Square Grid, Isometric Grid, Music Staff, Daily Planner, Weekly Schedule*.
- **5 Paper Colors**: *Crisp White, Warm Cream, Soft Sepia, Dark Slate, Midnight Ink*.
- **Multi-Page PDF Import & Annotation**: Open any PDF via Android's file picker, render crisp pages with `PdfRenderer`, and write/highlight directly on top.
- **Multi-Page PDF Export**: Export entire notebooks (with templates, images, and pressure strokes) to a shareable PDF via `PdfDocument`.
- **Audio Memos & Pomodoro Timer**: Record lecture audio alongside your notes and track 25-minute focus sessions right from the titlebar.

---

## 🏗️ Architecture

```
com.inkcraft.app
├── ai/InkAiEngine.kt          # On-device ML Kit Digital Ink + TTS + Smart Insights
├── audio/AudioMemoManager.kt  # Lecture audio recording & playback
├── data/NotebookRepository.kt # Atomic JSON workspace persistence & media storage
├── export/PdfTools.kt         # Multi-page PDF import (PdfRenderer) & export (PdfDocument)
├── ink/
│   ├── InkCanvasView.kt       # Hardware-accelerated stylus View + palm rejection
│   ├── InkMath.kt             # Pressure curves, shape recognition, lasso, stroke split
│   ├── PaperTemplates.kt      # 10 procedural vector paper templates
│   └── StrokeRenderer.kt      # Shared Canvas stroke renderer
├── model/NoteModels.kt        # Serializable domain models
└── ui/
    ├── ShelfScreen.kt         # macOS Finder-style library, folders & covers
    ├── EditorScreen.kt        # Unified titlebar, floating dock, Zoom Box & AI Studio
    └── Theme.kt               # macOS studio color system
```

---

## 📲 Installation

1. Go to **[Releases](https://github.com/AchieverSince2004/Performance/releases/latest)** on your Android tablet.
2. Download `InkCraft-v2.0.0.apk`.
3. Tap the downloaded APK and follow the Android installer prompt.
