# ROADMAP: Android Photo Auto-Organizer

**Project:** Photo-Classificator  
**Version:** v1.0 MVP  
**Last Updated:** 2025-03-06  

---

## Overview

This roadmap delivers the Android Photo Auto-Organizer through **4 phases**, ensuring data safety and core functionality before polish. Each phase builds on the previous, with explicit dependencies and testable success criteria.

**Total v1 Requirements:** 35  
**Phases:** 4  
**Estimated Timeline:** 8 weeks (Foundation: 3w, Pipeline: 3w, Polish: 2w, Release: 1w)

---

## Phase Summary

| Phase | Name | Goal | Requirements | Success Criteria |
|-------|------|------|--------------|------------------|
| 1 | Foundation & Data Safety | User can configure categories and grant permissions; app safely handles files with zero data loss | 11 | 4 |
| 2 | Detection & Classification Pipeline | App automatically detects, classifies, and moves photos without user interaction for high-confidence matches | 14 | 5 |
| 3 | Notifications & Polish | User receives daily summaries for uncertain photos; edge cases handled gracefully | 7 | 4 |
| 4 | Release Preparation | App is tested, documented, and ready for Play Store submission | 3 | 3 |

---

## Phase 1: Foundation & Data Safety

**Duration:** 3 weeks  
**Goal:** Users can configure categories, grant permissions, and trust the app with their photos. Zero data loss is guaranteed through transaction logging and safe file operations.

### Requirements (11)

| ID | Requirement | Purpose |
|----|-------------|---------|
| CAT-01 | Users define categories with target folders | Core configuration |
| CAT-02 | Maximum 10 categories | Prevents accuracy degradation |
| CAT-05 | Configuration persists via DataStore | Settings survival |
| FILE-01 | SAF obtains permissions for target folders | Scoped storage compliance |
| FILE-02 | Copy-then-verify-then-delete pattern | Data loss prevention |
| FILE-03 | Transaction log tracks operations | Crash recovery |
| FILE-05 | 7-day trash folder | User safety net |
| DATA-01 | Room database stores photo metadata | State management |
| DATA-02 | Processed photos tracked | Duplicate prevention |
| DATA-03 | Processing queue for offline photos | Reliability |
| DATA-04 | Storage full detection | Operation safety |
| ERR-01 | Failed operations retry with backoff | Resilience |
| ERR-03 | Permission denials trigger re-onboarding | UX recovery |
| ERR-04 | Transaction log enables crash recovery | Data safety |

*Note: 11 unique requirements (CAT-05 and ERR-04 overlap with DATA-01 and FILE-03)*

### Success Criteria (4)

1. **User can define and persist categories**: Given a fresh install, when the user creates 3 categories with target folders, then those categories persist after app restart
2. **Permissions are granted through onboarding**: Given first launch, when the user completes onboarding, then SAF permissions are obtained for all configured target folders
3. **File operations are crash-safe**: Given a photo being moved, when the app crashes mid-operation, then the transaction log enables complete recovery on next launch with no data loss
4. **Storage constraints are handled gracefully**: Given a full storage condition, when the app attempts to move a photo, then the operation is skipped with a user-visible warning

### Phase 1 Dependencies
- None (foundational phase)

### Tech Stack
**Flutter + Platform Channels Architecture:**
- **Flutter**: Dart code for UI, state management (Riverpod), business logic
- **Platform Channels**: Flutter-to-Android communication for SAF operations
- **Native Android**: Kotlin for SAF DocumentFile, WorkManager, Foreground Service
- **Database**: sqflite (SQLite) for Flutter
- **Preferences**: shared_preferences (DataStore equivalent)
- **ML**: google_mlkit_image_labeling (Flutter package)
- **Background**: workmanager (Flutter package wrapping WorkManager)

### Plans
**7 plans in 3 execution waves:**

| Plan | Objective | Wave | Files |
|------|-----------|------|-------|
| [ ] 01-01 | Create platform channels for SAF operations. Implement sqflite database with entities and SharedPreferences for settings. | 1 | 10 files |
| [ ] 01-02 | Implement SAF integration via platform channels for folder discovery. Build FolderRepository with offline-first pattern using Riverpod. | 1 | 6 files |
| [ ] 01-03 | Create SafeFileService with copy-then-verify-then-delete pattern via platform channels. Implement transaction logging and crash recovery. | 1 | 8 files |
| [ ] 01-04 | Build single-screen onboarding with SAF permission picker using file_picker. Riverpod state management. | 2 | 6 files |
| [ ] 01-05 | Implement folder learning with ML Kit Image Labeling (50 photos per folder). Background WorkManager via workmanager package. | 2 | 7 files |
| [ ] 01-06 | Create trash system with 7-day retention and daily cleanup worker via platform channels. | 2 | 6 files |
| [ ] 01-07 | Build main screen and settings with Riverpod. Implement startup crash recovery and confidence threshold selection. | 3 | 8 files |

### Deliverables
- Category management UI
- Onboarding flow with SAF permissions
- Room database schema (PhotoRecord, Category, TransactionLog)
- SafeFileOperations utility with transaction logging
- Trash folder implementation

---

## Phase 2: Detection & Classification Pipeline

**Duration:** 3 weeks  
**Goal:** App automatically detects new photos, classifies them using on-device ML, and moves them to appropriate folders without user interaction for high-confidence matches.

### Requirements (14)

| ID | Requirement | Purpose |
|----|-------------|---------|
| MON-01 | Foreground service maintains persistent notification | Always-on monitoring |
| MON-02 | MediaStore ContentObserver watches for new photos | Event-driven detection |
| MON-03 | Service restarts after device reboot | Reliability |
| MON-04 | Service handles Doze mode gracefully | Battery efficiency |
| MON-05 | WorkManager schedules ML jobs with constraints | Battery-aware processing |
| ML-01 | ML Kit Image Labeling analyzes photos | On-device classification |
| ML-02 | Classification maps ML labels to categories | Category matching |
| ML-03 | Confidence threshold (0.7) determines auto-sort | Quality gate |
| ML-04 | Low-confidence photos queue for user decision | Uncertainty handling |
| ML-05 | Photos downscaled to 224x224 for performance | Optimization |
| CAT-03 | Categories map to ML labels with thresholds | Classification rules |
| CAT-04 | Hierarchical categories supported | Organization depth |
| IMG-01 | JPEG support | Format coverage |
| IMG-02 | PNG support | Format coverage |
| IMG-03 | WebP support | Format coverage |
| IMG-04 | HEIC/HEIF support (Android 9+) | Format coverage |
| IMG-05 | Unsupported formats skipped gracefully | Robustness |
| ERR-02 | Corrupt images skipped gracefully | Error resilience |

*Note: 14 unique requirements*

### Success Criteria (5)

1. **Photos are detected automatically**: Given the app is running in background, when a new photo is saved to DCIM/Camera or Downloads/WhatsApp, then it appears in the processing queue within 5 seconds
2. **High-confidence photos are auto-sorted**: Given a photo with classification confidence ≥0.7, when processed, then it is moved to the target folder without user notification
3. **Low-confidence photos are queued for review**: Given a photo with classification confidence <0.7, when processed, then it remains in source location and is marked for user review
4. **Service survives device reboot**: Given the device restarts, when the boot completes, then the monitoring service restarts automatically within 30 seconds
5. **Multiple image formats are supported**: Given JPEG, PNG, WebP, and HEIC photos, when processed, then all are successfully analyzed and classified

### Phase 2 Dependencies
- Phase 1 complete (requires: categories, permissions, safe file operations, transaction logging)

### Plans
**6 plans in 3 execution waves:**

| Plan | Objective | Wave | Files | Autonomous |
|------|-----------|------|-------|------------|
| [ ] 02-01 | Set up persistent foreground service with flutter_foreground_task. Implement MediaStore ContentObserver platform channel for automatic photo detection. Handle Doze mode and boot auto-restart. | 1 | 6 files | yes |
| [ ] 02-02 | Create photo detection queue management. Implement PhotoDetection model, PhotoQueueRepository for deduplication, and PhotoDetectionService coordinator. | 1 | 4 files | yes |
| [ ] 02-03 | Implement ML classification pipeline with image preprocessing. Add flutter_image_compress for 224x224 downscaling. Support JPEG, PNG, WebP, HEIC formats with corrupt image handling. | 2 | 4 files | yes |
| [ ] 02-04 | Build classification engine mapping ML labels to categories. Implement confidence thresholds (0.7 auto-sort), hierarchical categories, and label mappings. | 2 | 4 files | yes |
| [ ] 02-05 | Implement WorkManager-based background classification and auto-sort execution. Execute file moves via SafeFileService with transaction logging. | 3 | 4 files | yes |
| [ ] 02-06 | Build UI for monitoring service control and photo queue review. Add screens for service status, queue management, and classification results. | 3 | 5 files | no |

### Deliverables
- Foreground service with ContentObserver
- WorkManager workers for ML inference
- ML Kit Image Labeling integration
- Classification engine with confidence thresholds
- MediaStore integration for file moves
- Support for JPEG, PNG, WebP, HEIC formats

---

## Phase 3: Notifications & Polish

**Duration:** 2 weeks  
**Goal:** Users receive daily summaries for uncertain photos, duplicates are prevented, and the app handles edge cases gracefully.

### Requirements (7)

| ID | Requirement | Purpose |
|----|-------------|---------|
| FILE-06 | Duplicate detection via SHA-256 hash | Prevents reprocessing |
| NOTE-01 | Daily summary notification for low-confidence photos | Batch UX |
| NOTE-02 | Notification shows thumbnail and suggestions | Decision context |
| NOTE-03 | User can approve/reject/reclassify from notification | Actionable UX |
| NOTE-04 | Silent operation by default; notify only on uncertainty | UX principle |
| DATA-05 | Configuration backup/restore via Android backup | Data portability |
| ERR-05 | Manual sync button triggers reprocessing | User control |

### Success Criteria (4)

1. **Daily summaries are delivered**: Given 5 low-confidence photos accumulated, when 24 hours pass, then a single daily notification appears with thumbnails and suggested categories
2. **Users can resolve from notification**: Given a daily summary notification, when the user taps approve/reject/reclassify, then the action completes without opening the app
3. **Duplicates are detected and skipped**: Given a photo already processed, when the same file appears again (same hash), then it is skipped with a log entry
4. **Manual sync recovers missed photos**: Given photos missed during offline period, when the user triggers manual sync, then all unprocessed photos are queued for classification

### Phase 3 Dependencies
- Phase 2 complete (requires: working detection, classification, and low-confidence queue)

### Deliverables
- Notification helper with daily batching
- Notification actions (approve/reject/reclassify)
- SHA-256 duplicate detection
- Manual sync UI
- Android backup configuration

---

## Phase 4: Release Preparation

**Duration:** 1 week  
**Goal:** App is tested, documented, and ready for Play Store submission.

### Requirements (3)

| ID | Requirement | Purpose |
|----|-------------|---------|
| *(Testing)* | Instrumented tests for file operations | Quality assurance |
| *(Testing)* | Device testing matrix (Pixel, Samsung, Xiaomi) | Compatibility |
| *(Docs)* | Play Store listing and screenshots | Distribution |

*Note: These are implicit release requirements not explicitly listed in v1 requirements*

### Success Criteria (3)

1. **All critical paths have automated tests**: Given the test suite, when run, then file operations, classification, and recovery scenarios pass with >90% coverage
2. **App runs on target device matrix**: Given Pixel, Samsung, and Xiaomi devices, when tested, then core functionality works on Android 10-15
3. **Play Store submission is ready**: Given the release build, when submitted, then it passes pre-launch report and policy review

### Phase 4 Dependencies
- Phase 3 complete (requires: full feature set implemented)

### Deliverables
- Instrumented test suite
- Device testing report
- Play Store listing (description, screenshots, feature graphic)
- Privacy policy
- Signed release APK

---

## Requirement Traceability

### Complete Mapping

| Requirement | Phase | Status |
|-------------|-------|--------|
| MON-01 | Phase 2 | Pending |
| MON-02 | Phase 2 | Pending |
| MON-03 | Phase 2 | Pending |
| MON-04 | Phase 2 | Pending |
| MON-05 | Phase 2 | Pending |
| ML-01 | Phase 2 | Pending |
| ML-02 | Phase 2 | Pending |
| ML-03 | Phase 2 | Pending |
| ML-04 | Phase 2 | Pending |
| ML-05 | Phase 2 | Pending |
| FILE-01 | Phase 1 | Pending |
| FILE-02 | Phase 1 | Pending |
| FILE-03 | Phase 1 | Pending |
| FILE-04 | Phase 2 | Pending |
| FILE-05 | Phase 1 | Pending |
| FILE-06 | Phase 3 | Pending |
| CAT-01 | Phase 1 | Pending |
| CAT-02 | Phase 1 | Pending |
| CAT-03 | Phase 2 | Pending |
| CAT-04 | Phase 2 | Pending |
| CAT-05 | Phase 1 | Pending |
| NOTE-01 | Phase 3 | Pending |
| NOTE-02 | Phase 3 | Pending |
| NOTE-03 | Phase 3 | Pending |
| NOTE-04 | Phase 3 | Pending |
| IMG-01 | Phase 2 | Pending |
| IMG-02 | Phase 2 | Pending |
| IMG-03 | Phase 2 | Pending |
| IMG-04 | Phase 2 | Pending |
| IMG-05 | Phase 2 | Pending |
| DATA-01 | Phase 1 | Pending |
| DATA-02 | Phase 1 | Pending |
| DATA-03 | Phase 1 | Pending |
| DATA-04 | Phase 1 | Pending |
| DATA-05 | Phase 3 | Pending |
| ERR-01 | Phase 1 | Pending |
| ERR-02 | Phase 2 | Pending |
| ERR-03 | Phase 1 | Pending |
| ERR-04 | Phase 1 | Pending |
| ERR-05 | Phase 3 | Pending |

### Coverage Summary

| Phase | Requirement Count | Percentage |
|-------|-------------------|------------|
| Phase 1: Foundation & Data Safety | 11 | 31% |
| Phase 2: Detection & Classification Pipeline | 14 | 40% |
| Phase 3: Notifications & Polish | 7 | 20% |
| Phase 4: Release Preparation | 3 | 9% |
| **Total v1** | **35** | **100%** |

---

## Phase Dependencies Graph

```
┌─────────────────────────────────────────────────────────┐
│                    Phase 1: Foundation                    │
│              (Data Safety, Config, Permissions)           │
└───────────────────────────┬─────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              Phase 2: Detection & Classification        │
│              (Service, ML, File Moves)                  │
└───────────────────────────┬─────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              Phase 3: Notifications & Polish            │
│              (Daily Summaries, Duplicates, Sync)        │
└───────────────────────────┬─────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│              Phase 4: Release Preparation                 │
│              (Testing, Docs, Play Store)                │
└─────────────────────────────────────────────────────────┘
```

---

## Success Criteria Summary

| Phase | Criteria | Observable User Behavior |
|-------|----------|-------------------------|
| 1 | 4 | Categories persist, permissions granted, crash recovery works, storage handled |
| 2 | 5 | Photos detected <5s, auto-sorted ≥0.7 confidence, low-confidence queued, survives reboot, formats supported |
| 3 | 4 | Daily notifications actionable, duplicates skipped, manual sync works |
| 4 | 3 | Tests pass, device matrix verified, Play Store ready |
| **Total** | **16** | **100% outcome-focused** |

---

## Risk Mitigation by Phase

| Risk | Phase | Mitigation |
|------|-------|------------|
| Data loss during file operations | Phase 1 | Transaction log + copy-verify-delete + trash folder |
| Permission denial | Phase 1 | Re-onboarding flow + clear SAF UX |
| Service killed by Doze | Phase 2 | Foreground service + WorkManager + battery optimization request |
| ML battery drain | Phase 2 | Downscaling + NNAPI + batch processing |
| Notification fatigue | Phase 3 | Daily summaries only, not per-photo |
| Play Store rejection | Phase 4 | Scoped storage compliance + policy justification |

---

*Document generated: 2025-03-06*  
*Next review: Phase completion checkpoints*
