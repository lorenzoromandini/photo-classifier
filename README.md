# Photo Classifier

[![Phase](https://img.shields.io/badge/phase-01%20Complete-success)](https://github.com/lorenzoromandini/photo-classifier)
[![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-brightgreen)](https://flutter.dev)
[![Framework](https://img.shields.io/badge/framework-Flutter-blue)](https://flutter.dev)

A **cross-platform mobile app** that automatically organizes your photos using on-device machine learning. Photos are analyzed and sorted into folders as they arrive — no manual organization required. Built with Flutter for Android and iOS.

## Features

### Phase 01: Foundation & Data Safety ✅ COMPLETE

- **Auto-discovery** — Discovers existing folders in your Pictures/ directory
- **Folder Learning** — Analyzes 50 sample photos per folder to learn content patterns
- **Safe File Operations** — Copy-then-verify-then-delete pattern with transaction logging
- **Crash Recovery** — Transaction log enables complete recovery if app crashes mid-operation
- **File System Integration** — Native file picker for folder selection
- **7-Day Trash** — Deleted photos retained for 7 days before permanent removal
- **Confidence Settings** — Three levels: Low (0.6), Medium (0.75), High (0.9)

### Phase 02: Detection & Classification Pipeline 🚧 PLANNED

- Background service with persistent notification
- Real-time photo detection on new photos
- ML Kit Image Labeling for on-device classification
- Automatic organization of high-confidence matches
- Low-confidence photo queue for review

## Architecture

Built with Flutter and clean architecture principles:

```
lib/
├── data/
│   ├── database/           # sqflite for local storage
│   ├── services/           # ML Kit, file operations, preferences
│   └── repositories/       # Data access layer
├── domain/
│   └── models/             # Business entities (Folder, Photo, Transaction)
├── presentation/
│   ├── screens/            # UI screens
│   ├── widgets/            # Reusable components
│   └── providers/          # Riverpod state management
└── main.dart
```

### Tech Stack

- **Framework**: Flutter 3.x
- **UI**: Material 3 design system
- **State Management**: Riverpod
- **Database**: sqflite (SQLite)
- **Preferences**: SharedPreferences
- **File Operations**: file_picker, permission_handler, path_provider
- **ML**: google_mlkit_image_labeling (on-device, no cloud)
- **Background**: workmanager

## Requirements

### Android
- **SDK**: 21+ (Android 5.0+)
- **Permissions**: Storage, Camera (optional)

### iOS
- **Version**: 15.5+
- **Permissions**: Photo Library

## Getting Started

### Prerequisites

- Flutter SDK 3.0+
- Dart SDK 3.0+
- Android Studio / Xcode

### Install Dependencies

```bash
flutter pub get
```

### Run

```bash
# Android
flutter run

# iOS (requires macOS + Xcode)
flutter run -d ios
```

### Build

```bash
# Android APK
flutter build apk

# Android App Bundle
flutter build appbundle

# iOS
flutter build ios
```

## Project Structure

```
lib/
├── data/
│   ├── database/
│   │   └── database_service.dart      # SQLite database setup
│   └── services/
│       ├── preference_service.dart    # SharedPreferences wrapper
│       ├── folder_discovery_service.dart  # File system scanning
│       └── ml_labeling_service.dart   # ML Kit Image Labeling
├── domain/
│   └── models/
│       ├── folder_model.dart
│       ├── photo_model.dart
│       └── transaction_model.dart
├── presentation/
│   ├── screens/
│   │   ├── onboarding_screen.dart     # First-run setup
│   │   └── main_screen.dart           # Folder list + settings
│   └── providers/
│       ├── onboarding_provider.dart
│       ├── folder_provider.dart
│       └── preference_provider.dart
└── main.dart
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
- **Cross-Platform** — Single codebase for Android and iOS

## License

MIT License — See [LICENSE](LICENSE) for details.

---

**Built with** ❤️ **and** 🤖 **using the GSD methodology**
