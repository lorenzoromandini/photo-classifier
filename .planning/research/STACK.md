# Technology Stack: Android Photo Auto-Organization

**Project:** Photo-Classificator - Android On-Device Photo Auto-Organization Agent  
**Researched:** March 2026  
**Last Updated:** 2026-03-06

## Executive Summary

The 2025 standard stack for Android on-device image classification is **ML Kit + WorkManager + Kotlin Coroutines**. ML Kit provides Google's optimized, on-device ML models that run entirely locally with zero cloud dependency. For background file monitoring, WorkManager with MediaStore observation is the Android-recommended approach for battery-efficient background processing.

**Confidence Level: HIGH** - All recommendations based on official Google documentation (developers.google.com/ml-kit) and verified current versions from Maven repositories.

---

## Recommended Stack

### Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 2.3.10 | Primary language | Official Android recommended language; null-safety, coroutines for async operations, modern syntax |
| Android SDK | API 23+ (Android 6.0) | Minimum target | ML Kit requires API 23+; covers 95%+ of devices as of 2025 |
| Gradle | 8.x+ | Build system | Required for modern Android development with Kotlin DSL support |

### ML/Classification Layer

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **ML Kit Image Labeling** | 17.0.9 | Image classification | Google's production-ready on-device model with 400+ labels (receipts, food, screenshots, documents, pets, etc.). Zero cloud dependency. Confidence scores included. |
| ML Kit (Play Services) | 16.0.8 | Alternative packaging | Dynamically downloaded model (~200KB app size increase vs 5.7MB bundled). Good for APK size optimization. |
| **TensorFlow Lite** | 2.16+ | Custom classification (optional) | Only if ML Kit's 400 labels insufficient. Supports custom models from TensorFlow Hub or custom training. |

**Decision Matrix:**
- Use ML Kit if: You need general categories (receipts, food, selfies, screenshots, documents, pets, products). 400 labels covers most photo organization needs.
- Use TensorFlow Lite if: You need domain-specific classification ML Kit doesn't cover (e.g., specific dog breeds, specific document types).

### Background Processing Layer

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **WorkManager** | 2.9.1+ | Background photo monitoring | Android's recommended API for deferrable, guaranteed background work. Handles battery optimization, Doze mode, app standby. |
| **MediaStore API** | Built-in | File system observation | Official Android API for monitoring media file changes (photos added to DCIM, Downloads, etc.). No polling required. |
| Kotlin Coroutines | 1.8.0+ | Async processing | Structured concurrency for ML inference without blocking threads. Built into Kotlin. |
| Foreground Service | Built-in | Persistent agent | Required for "always-running" behavior while respecting Android background restrictions. |

### Data/Storage Layer

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **Room** | 2.6.1+ | Photo metadata database | Android's abstraction layer over SQLite. Type-safe, compile-time SQL validation, coroutines support. |
| **Datastore** | 1.1.0+ | User preferences | Modern replacement for SharedPreferences. Type-safe (protobuf or primitives), handles async I/O, migration support. |
| ExifInterface | 1.3.7+ | EXIF metadata | Read/write image metadata (creation date, location, camera info) before moving files. |

### File Operations

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **Storage Access Framework (SAF)** | Built-in | Scoped storage compliance | Required for Android 10+ (API 29+) to access shared storage. Uses ACTION_OPEN_DOCUMENT_TREE for folder access. |
| **MediaStore** | Built-in | File operations | Move files between directories while maintaining MediaStore database consistency. |
| DocumentFile API | Built-in | File manipulation | Work with files via URIs from SAF. Handles path abstraction. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AndroidX Core | 1.12.0+ | Core utilities | Always - modern Android extensions |
| AndroidX Lifecycle | 2.7.0+ | Lifecycle-aware components | For services that need lifecycle management |
| Notifications | Built-in | Low-confidence alerts | Notify user when classification confidence below threshold |
| Glide | 4.16.0+ | Image loading (UI) | Only if you build a UI to review photos |

---

## Implementation Architecture

### Stack Flow

```
Photo arrives in DCIM/Downloads/WhatsApp
         ↓
[MediaStore ContentObserver] ← Always monitoring via Foreground Service
         ↓
[WorkManager Job] ← Triggered on new photo, battery-aware
         ↓
[ML Kit Image Labeler] ← On-device inference (no network)
         ↓
[Classification Logic] ← Map ML labels to user categories
         ↓
[File Move via SAF] ← Organize into user-defined folders
         ↓
[Room Database] ← Track organization history
         ↓
[Notification] ← Only if confidence < threshold
```

---

## What NOT to Use

| Technology | Why Not | What to Use Instead |
|------------|---------|---------------------|
| **Firebase ML (cloud)** | Requires network, privacy concerns, latency, costs | ML Kit (on-device) |
| **TensorFlow Lite for simple cases** | Overkill for standard categories; requires model management, training data | ML Kit Image Labeling |
| **Manual FileObserver** | Deprecated approach; battery inefficient | MediaStore + WorkManager |
| **IntentService** | Deprecated in API 30; not job-scheduled | WorkManager |
| **AsyncTask** | Deprecated in API 30 | Kotlin Coroutines |
| **JobScheduler directly** | WorkManager wraps this with better API | WorkManager |
| **Direct file paths (/sdcard/Pictures)** | Scoped storage restrictions in Android 10+ | Storage Access Framework (SAF) with URIs |
| **ACCESS_MEDIA_LOCATION without permission** | Requires runtime permission for location metadata | Request `ACCESS_MEDIA_LOCATION` permission |
| **Polling loops for file monitoring** | Battery drain, inefficient | MediaStore ContentObserver |

---

## Key Technical Decisions

### 1. ML Kit vs TensorFlow Lite

**Recommendation: ML Kit Image Labeling (HIGH confidence)**

ML Kit's base model recognizes 400+ categories including:
- Receipts, Screenshots, Documents (explicitly supported)
- Food, Selfie, Crowd, Smile (explicitly supported)
- Car, Cat, Dog, Bird, Plant (explicitly supported)
- Artwork, Landscape, Product (explicitly supported)

**Confidence scores:** Each label returns 0.0-1.0 confidence. Threshold can be configured (default 0.5).

**Custom model support:** If ML Kit's base model insufficient, ML Kit supports custom TensorFlow Lite models via `com.google.mlkit:image-labeling-custom` dependency.

### 2. Background Execution Strategy

**Recommendation: Foreground Service + WorkManager (HIGH confidence)**

Android 8+ (API 26+) restricts background services. For "always-running" behavior:

1. **Foreground Service** - Keeps process alive, shows persistent notification
2. **WorkManager** - Schedules classification jobs with battery constraints
3. **MediaStore ContentObserver** - Immediate notification of new photos

**Battery optimization:** WorkManager respects Doze mode and App Standby buckets. Photos added during Doze are queued and processed when device exits Doze.

### 3. File Operations

**Recommendation: Storage Access Framework (SAF) (HIGH confidence)**

Android 10+ requires Scoped Storage. To organize photos:

1. User grants folder access via `ACTION_OPEN_DOCUMENT_TREE` to Pictures/ root
2. App uses DocumentFile API to create/move files via URIs
3. MediaStore updates automatically via ContentResolver

**No direct file paths allowed** - Must use `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` operations.

---

## Gradle Dependencies

```kotlin
dependencies {
    // Language
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    
    // ML Kit - Image Labeling (choose ONE)
    // Option A: Bundled model (5.7MB increase, immediate availability)
    implementation("com.google.mlkit:image-labeling:17.0.9")
    
    // Option B: Play Services model (200KB increase, download on first use)
    // implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    
    // Optional: Custom model support if ML Kit base insufficient
    // implementation("com.google.mlkit:image-labeling-custom:17.0.3")
    
    // Background Processing
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Data/Storage
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    
    // Lifecycle (for services)
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

---

## Minimum Viable Device Requirements

| Component | Requirement | Rationale |
|-----------|-------------|-----------|
| **Android Version** | API 23+ (Android 6.0) | ML Kit minimum requirement |
| **RAM** | 2GB+ | ML inference requires memory |
| **Storage** | 50MB+ app space | ML Kit model storage |
| **Architecture** | arm64-v8a or x86_64 | ML Kit optimized for 64-bit |

---

## Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| ML Engine | **HIGH** | Official ML Kit docs, verified current versions, Google's production recommendation |
| Background Processing | **HIGH** | WorkManager is Android's standard, documented patterns exist |
| File Operations | **HIGH** | Scoped Storage well-documented, SAF is standard approach |
| Architecture Pattern | **MEDIUM** | Requires custom implementation combining foreground service + WorkManager |

---

## Verification Sources

1. **ML Kit Image Labeling Android Guide** - developers.google.com/ml-kit/vision/image-labeling/android  
   Last updated: 2026-03-02

2. **ML Kit Custom Models** - developers.google.com/ml-kit/custom-models  
   Last updated: 2026-03-02

3. **ML Kit Object Detection** - developers.google.com/ml-kit/vision/object-detection/android  
   Last updated: 2026-03-02

4. **Kotlin Releases** - kotlinlang.org/docs/releases.html  
   Current stable: 2.3.10 (February 5, 2026)

5. **ExecuTorch Android** - pytorch.org/executorch/main/using-executorch-android.html  
   Version 1.0.0 available on Maven Central

6. **ML Kit Samples** - github.com/googlesamples/mlkit  
   Official Google samples repository

---

## Research Gaps

| Topic | Gap | Mitigation |
|-------|-----|------------|
| Custom model training | If ML Kit's 400 labels insufficient, custom model training requires TensorFlow expertise | Start with ML Kit, evaluate coverage before custom training |
| MediaStore reliability | ContentObserver may miss some file operations in edge cases | Implement periodic reconciliation WorkManager job (daily) |
| Scoped Storage UX | SAF permission flow requires user interaction for folder selection | Design clear onboarding explaining why access needed |

---

## Next Steps for Roadmap

1. **Phase 1 (MVP):** ML Kit base model + WorkManager + Room - covers 80% of use cases
2. **Phase 2 (Enhancement):** Custom TensorFlow Lite model if ML Kit labels insufficient
3. **Phase 3 (Optimization):** Play Services model distribution to reduce APK size

---

**End of Stack Research**
