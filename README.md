# Photo Classifier

[![Phase](https://img.shields.io/badge/phase-01%20Complete-success)](https://github.com/lorenzoromandini/photo-classifier)
[![Android](https://img.shields.io/badge/platform-Android-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-blue)](https://kotlinlang.org)

An Android app that automatically organizes your photos using on-device machine learning. Photos are analyzed and sorted into folders as they arrive — no manual organization required.

## Features

### Phase 01: Foundation & Data Safety ✅ COMPLETE

- **Auto-discovery** — Discovers existing folders in your Pictures/ directory
- **Folder Learning** — Analyzes 50 sample photos per folder to learn content patterns
- **Safe File Operations** — Copy-then-verify-then-delete pattern with transaction logging
- **Crash Recovery** — Transaction log enables complete recovery if app crashes mid-operation
- **SAF Integration** — Storage Access Framework for Android 10+ scoped storage compliance
- **7-Day Trash** — Deleted photos retained for 7 days before permanent removal
- **Confidence Settings** — Three levels: Low (0.6), Medium (0.75), High (0.9)

### Phase 02: Detection & Classification Pipeline 🚧 PLANNED

- Foreground service with persistent notification
- MediaStore ContentObserver for real-time photo detection
- ML Kit Image Labeling for on-device classification
- Automatic organization of high-confidence matches
- Low-confidence photo queue for review

## Architecture

Built with modern Android architecture patterns:

- **UI Layer** — Jetpack Compose with Material 3
- **Domain Layer** — Use cases and business logic
- **Data Layer** — Offline-first repositories with Room and DataStore

### Tech Stack

- **UI**: Jetpack Compose, Material 3, Navigation Compose
- **DI**: Hilt
- **Database**: Room with SQLite
- **Preferences**: Proto DataStore
- **Background**: WorkManager
- **ML**: ML Kit Image Labeling (on-device, no cloud)
- **Async**: Kotlin Coroutines + Flow

## Project Structure

```
app/src/main/java/com/example/photoorganizer/
├── data/
│   ├── local/
│   │   ├── database/       # Room entities and DAOs
│   │   ├── datastore/      # Proto DataStore preferences
│   │   ├── saf/            # Storage Access Framework
│   │   ├── safe/           # Crash-safe file operations
│   │   └── ml/             # ML Kit wrapper
│   ├── repository/         # Offline-first repositories
│   └── worker/             # Background WorkManager workers
├── di/                     # Hilt dependency injection modules
├── domain/
│   └── model/              # Domain models
├── ui/
│   ├── components/         # Reusable UI components
│   ├── onboarding/         # Onboarding flow
│   ├── main/               # Main screen
│   └── settings/           # Settings screen
└── PhotoOrganizerApplication.kt
```

## Requirements

- **Android**: 10+ (API 29+)
- **Permissions**: Storage Access Framework (scoped storage compliant)
- **Offline**: All ML runs on-device, no internet required

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 11+

### Build

```bash
# Clone the repository
git clone git@github.com:lorenzoromandini/photo-classifier.git
cd photo-classifier

# Build the project
./gradlew build

# Run on device/emulator
./gradlew installDebug
```

## Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 01 | ✅ Complete | Foundation & Data Safety |
| 02 | 🚧 Planned | Detection & Classification Pipeline |
| 03 | 📋 Planned | Notifications & Polish |
| 04 | 📋 Planned | Release Preparation |

See [ROADMAP.md](.planning/ROADMAP.md) for detailed phase breakdown.

## Philosophy

- **Zero Data Loss** — Transaction logging and safe file operations
- **Privacy First** — All processing on-device, no cloud
- **Invisible UX** — Works silently in the background, only notify on uncertainty
- **Conservative by Default** — High confidence threshold to avoid misplacing photos

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Built with** ❤️ **and** 🤖 **using the GSD methodology**
