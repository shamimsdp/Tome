# Tome — Book-Like PDF Reader & Annotation Studio

[![Android](https://img.shields.io/badge/Platform-Android_14+-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_BOM-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material_3-blue.svg?style=flat)](https://m3.material.io/)
[![Room Database](https://img.shields.io/badge/Storage-Room_2.7-orange.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![CI/CD](https://img.shields.io/badge/Build-GitHub_Actions-2088FF.svg?style=flat&logo=githubactions)](https://github.com)

**Tome** is a modern, high-performance Android PDF reader crafted to bring back the tangible pleasure of reading a physical book on a digital screen. It features page-turning ergonomics, multi-color text highlights, personal contemplation notes, a dog-ear bookmarking system, cloud drive integration, and offline caching.

---

## ✨ Key Features

### 📖 1. "Read Like a Book" Experience
* **Realistic Page Aesthetics**: Rendered with paper margins, soft spine gutter shadows, and delicate edge curl gradients.
* **Touch-Zone Navigation**: Tap the left edge (20%) to turn back, tap the right edge (20%) to flip forward, or tap the center (60%) to toggle reading controls.
* **Interactive Page Scrubber**: Fast slider with instant page preview, page counter (`Page X of Y`), and reading progress percentage.
* **Movable Reading Ruler**: Draggable horizontal focus guide overlay that helps you track lines during deep reading sessions.
* **5 Reading Themes**:
  * **Clean Day**: Crisp white paper with rich contrast typography.
  * **Warm Book (Classic Sepia)**: Amber/cream paper tone that simulates real book pages and reduces eye fatigue.
  * **Parchment Sage**: Gentle, eye-soothing soft green tint.
  * **Night Charcoal**: Muted dark theme for comfortable evening reading.
  * **OLED Pure Dark**: True black background for maximum contrast and battery conservation.

### 🖍️ 2. Text Highlighting & Personal Contemplation Notes
* **6-Color Highlighter Palette**: Amber Gold (`#FFEB3B`), Mint Emerald (`#4CAF50`), Sky Blue (`#03A9F4`), Rose Pink (`#E91E63`), Royal Violet (`#9C27B0`), and Sunset Orange (`#FF9800`).
* **Category Tagging**: Organize highlights by `Key Takeaway`, `Important`, `Question`, `Quote`, `Vocabulary`, or `Action Item`.
* **Personal Notes**: Attach custom thoughts, quotes, or summaries to any highlighted passage.
* **Full Annotation Editing**: Edit note text, switch colors, update category tags, or delete annotations with a single tap.
* **Markdown Export & Sharing**: Export all your highlights and notes into clean Markdown formatted text and share via the Android share sheet.

### 🔖 3. Intuitive Bookmarking System
* **Dog-Ear Ribbon Effect**: Bookmarked pages display a folded corner in the top-right corner.
* **One-Tap Bookmark**: Tap the ribbon icon in the top app bar to instantly bookmark or remove a bookmark.
* **Bookmarks Drawer**: View all saved bookmarks with snippets and jump directly to any marked page.
* **Automatic Progress Recovery**: Remembers your exact reading position, timestamp, and percentage for every document.

### 🔍 4. Fast Full-Text Search
* **Instant Keyword Filtering**: Search across the entire document text in real time.
* **Page Highlights**: Occurrences are highlighted with orange markers on the book page.
* **Search Navigation**: Step through occurrences with Next and Previous match buttons (`Match 2 of 8`) and inspect contextual sentence snippets.

### ☁️ 5. Local & Cloud Storage Providers
* **Local PDF Import**: Open any PDF file from device storage via Android's Storage Access Framework (`OpenDocument`).
* **Built-in Classic Library**: Pre-loaded with complete editions of:
  * *The Art of Reading & Focus* by Mortimer J. Adler & Charles Van Doren
  * *Pride and Prejudice* by Jane Austen
  * *Modern Software Architecture & Kotlin Guide*
* **Cloud Drive Connector**: Browse simulated Google Drive and Cloud Storage files, inspect file sizes and page counts, and cache documents locally with one tap.

### ⚡ 6. Offline Mode & Cross-Device Sync
* **Offline Mode Master Switch**: Toggle offline mode for uninterrupted reading on flights or transit.
* **Single Source of Truth**: All documents, bookmarks, highlights, and notes are persisted locally using **Room (SQLite)** with KSP.
* **Sync Dashboard**: Status badge (`Synced just now`), unsynced item counters, manual `Sync Now` button, and activity audit logs.

### 🤖 7. Gemini AI Literary Companion & Google Search Grounding
* **Multi-Turn Scrollable Chat**: Maintain a rich conversational dialogue about any book, chapter, or philosophical idea.
* **Google Search Grounding**: Integrates live Google Search data via the `googleSearch` tool on `gemini-3.5-flash` to verify real-time historical facts, contemporary literary critiques, and author biographies with clickable citation sources.
* **Specialized System Roles**:
  * **Literary Scholar & Critic**: Deep, eloquent analysis of symbolism, prose style, and philosophical themes.
  * **Thoughtful Reading Companion**: Friendly assistance explaining difficult vocabulary and plot nuances.
  * **Grounded Research Assistant**: Facts, historical timelines, and real-world cross-references.
  * **Socratic Discussion Mentor**: Probing questions that encourage critical and active contemplation.
* **Smart Model Selection**:
  * `gemini-3.5-flash`: General, balanced literary exploration with Google Search Grounding.
  * `gemini-3.1-pro-preview`: Advanced reasoning, complex critique, and deep philosophical queries.
  * `gemini-3.1-flash-lite-preview`: Rapid definitions, quick summaries, and fast answers.
* **Context-Aware Book Discussions**: Ask questions with the currently open book and page text attached with a single tap.

---

## 🏗️ Architecture & Technology Stack

```
com.example
├── MainActivity.kt                # Single activity with edge-to-edge Compose navigation
├── data
│   ├── model                      # Room entities (Document, Bookmark, Annotation, SyncLog)
│   ├── local                      # Room DAOs and AppDatabase
│   └── repository                 # PdfRepository and CloudSyncRepository
├── engine
│   ├── PdfEngine.kt               # Hardware-accelerated PdfRenderer with LRU bitmap cache
│   └── SamplePdfGenerator.kt      # Native PdfDocument sample book generator
├── ui
│   ├── theme                      # Material 3 colors, typography, and TomeTheme
│   ├── components                 # BookPageView, ReadingControls, AnnotationDialog, SearchOverlay
│   ├── screens                    # LibraryScreen, ReaderScreen, CloudScreen, AllNotesScreen, SyncSettingsScreen
│   └── viewmodel                  # PdfViewModel (state management with StateFlow)
```

* **UI Layer**: 100% Jetpack Compose using Material 3 design tokens and responsive WindowInsets.
* **Rendering Engine**: Native `android.graphics.pdf.PdfRenderer` utilizing hardware acceleration to rasterize vector PDF pages into crisp Bitmaps with an adaptive `LruCache`.
* **State Management**: Android `ViewModel` + `MutableStateFlow` with unidirectional data flow (UDF).
* **Local Persistence**: **Room 2.7.0** using Kotlin Symbol Processing (**KSP**).
* **Zero-Permission Media Access**: Android Storage Access Framework (SAF) compliant with Google Play privacy policies.

---

## 🚀 Building & Running Locally

### Prerequisites
* **Android Studio Ladybug (or newer)**
* **JDK 17 or JDK 21**
* **Android SDK 36** (Min SDK 24)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/your-username/tome-pdf-reader.git
cd tome-pdf-reader

# Run local unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 CI/CD & GitHub Workflow Deployment

This repository includes an automated GitHub Actions workflow in `.github/workflows/build-and-release.yml`.

### Automated Features:
1. **Pull Request & Push Verification**: Automatically executes `./gradlew testDebugUnitTest` and verifies the build on every push to `main`/`master`.
2. **Artifact Generation**: Builds the APK and uploads it as a downloadable GitHub Action workflow artifact (`Tome-APK`) retained for 30 days.
3. **Automatic GitHub Releases**: Whenever a git tag starting with `v*` is pushed (e.g., `git tag v1.0.0 && git push origin v1.0.0`), the workflow automatically:
   - Builds the production APK.
   - Creates a new GitHub Release with release notes.
   - Attaches the packaged `Tome-v1.0.0.apk` file to the release for direct user download.
4. **Manual Dispatch**: You can also trigger the workflow manually from the **Actions** tab in GitHub, choosing between `debug` and `release` build modes and toggling release publishing.

### Release Signing Secrets (Optional)
To sign release builds in GitHub Actions, configure these repository secrets under **Settings > Secrets and variables > Actions**:
* `KEYSTORE_PATH` (or upload keystore file via repository secret)
* `STORE_PASSWORD`
* `KEY_ALIAS`
* `KEY_PASSWORD`

If signing secrets are omitted, the workflow builds a valid debug/unsigned APK ready for testing.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
