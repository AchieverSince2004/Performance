<div align="center">

# ✒️ Vellum

### A macOS-Inspired, ML-Powered Stylus Note Studio for Android Tablets

[![Release](https://img.shields.io/github/v/release/AchieverSince2004/Performance?style=for-the-badge&color=007AFF&label=Latest%20APK)](https://github.com/AchieverSince2004/Performance/releases/latest)
[![Platform](https://img.shields.io/badge/Android-Tablet%20%2B%20Stylus-34C759?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AchieverSince2004/Performance/releases/latest)
[![ML Kit](https://img.shields.io/badge/On--Device%20ML-Digital%20Ink%20%2B%20TTS-5856D6?style=for-the-badge)](https://developers.google.com/ml-kit/vision/digital-ink-recognition)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)

**[⬇ Download Vellum v3.0.0 APK for Your Tablet](https://github.com/AchieverSince2004/Performance/releases/latest)**

</div>

---

## ✨ Why Vellum?

Most Android note-taking apps feel like blown-up phone apps with polygonal, laggy strokes and jumpy touch canvases. **Vellum** is engineered from scratch for **tablet + active stylus workflows** (Samsung S Pen, USI, Lenovo Precision Pen, Xiaomi Smart Pen) — pairing **macOS Studio aesthetics** with a **Catmull-Rom variable-width ribbon ink engine** and **on-device machine learning**.

---

## 🖋️ Pro Ribbon Ink Engine & Zero-Drift Palm Rejection

- **Catmull-Rom Variable-Width Ribbon Rendering**: Subdivides every stroke with cubic Catmull-Rom splines, smooths pressure transitions with a 3-tap moving average, and constructs a filled perpendicular-normal ink ribbon with round caps — eliminating jagged polygon lines.
- **8 Expressive Pen & Brush Tools**:
  - **Fountain Pen** (pressure + tilt + velocity tapering)
  - **Calligraphy Nib** (45° chisel nib dynamics)
  - **Graphite Pencil** (soft textured pencil grain)
  - **Brush Pen** (wide dynamic range)
  - **Ballpoint Pen** & **Fineliner**
  - **Highlighter** & **Laser Pointer**
- **Rock-Solid Palm Rejection + Page Lock**: Independently tracks `TOOL_TYPE_STYLUS` / `TOOL_TYPE_ERASER` pointer indices, enforces a **550ms hover/lift cooldown**, filters palm `touchMajor`, and offers a **1-Tap Page Lock** pill.
- **Constrained Smooth Viewport & Zoom HUD**: Defaults to comfortable **Fit-Width** page sizing, constrains horizontal drift, and includes a persistent bottom-right **Zoom & Page HUD** (`-`, `100%`, `+`, `Fit`, `Prev`/`Next Page`).
- **Interactive Ruler, Lasso & Text Boxes**: Drag the ruler center to move or its end handles to **rotate** with a live degree readout; circle strokes with **Lasso** to **Move, Scale, Rotate, Duplicate, Recolor, or Delete**; place **Text Boxes** and draggable **Sticky Notes**.

---

## 🧠 On-Device AI Studio (Offline ML + Voice)

1. **Handwriting → Digital Text (Google ML Kit Digital Ink Recognition)**: Converts all handwritten strokes on the page into clean, editable digital text.
2. **Voice Read-Aloud (`TextToSpeech`)**: Listen to your handwritten notes spoken aloud at `0.85x`, `1.0x`, or `1.25x` speed.
3. **Smart Insights**: Extracts word count, key bullet points, and `TODO` / deadline action items.

---

## 📄 18 Pro Paper Templates & 5 Paper Colors

*Blank, Ruled Wide, Ruled Narrow, Cornell Notes, Dot Grid, Square Graph, Engineering Grid, Isometric Grid, Hexagonal Chemistry, Storyboard, Tablet UI Wireframe, Music Staff, Guitar Tablature, Daily Planner, Weekly Plan, Monthly Calendar, Habit Tracker, and Meeting Minutes.*

---

## 📲 Installation

1. Open **[Releases](https://github.com/AchieverSince2004/Performance/releases/latest)** on your Android tablet.
2. Download `Vellum-v3.0.0.apk`.
3. Tap to install and start writing!
