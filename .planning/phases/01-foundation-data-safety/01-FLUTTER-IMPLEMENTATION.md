# Phase 1: Foundation & Data Safety — Flutter Implementation Plan

**Project:** Photo Classifier (Flutter)  
**Phase:** 1 — Foundation & Data Safety  
**Duration:** 3 weeks  
**Goal:** Users can configure folders via SAF, grant permissions, and trust the app with their photos. Zero data loss through transaction logging.

---

## Flutter Architecture Adaptation

### Native Android vs Flutter Mapping

| Android Native | Flutter Equivalent | Notes |
|---------------|-------------------|-------|
| Room Database | `sqflite` | Already implemented ✅ |
| DataStore | `shared_preferences` | Already implemented ✅ |
| SAF (Storage Access Framework) | **Platform Channel** | Requires native Kotlin code |
| WorkManager | `workmanager` plugin | Already in pubspec, needs setup |
| Foreground Service | **Platform Channel** | Requires native Kotlin service |
| MediaStore ContentObserver | **Platform Channel** | Requires native Kotlin observer |
| ML Kit | `google_mlkit_image_labeling` | Already implemented ✅ |
| Notifications | `flutter_local_notifications` | Already in pubspec ✅ |

### Critical Gap: Platform Channels

Flutter cannot directly access:
- Storage Access Framework (SAF) — required for Android 10+
- Foreground Services — required for background monitoring
- MediaStore ContentObserver — required for photo detection

**Solution:** Create a `MethodChannel` for Android-specific operations.

---

## Implementation Waves

### Wave 1: Platform Channel Foundation (Days 1-3)

**Goal:** Enable Flutter ↔ Native Android communication for SAF and background services.

#### Files to Create

**lib/core/platform/**
```
platform_channel.dart          # MethodChannel wrapper
android_storage_interface.dart # Abstract interface for storage ops
```

**android/app/src/main/kotlin/.../**
```
PhotoClassifierPlugin.kt       # Main plugin entry point
StorageAccessFramework.kt      # SAF implementation
PhotoObserverService.kt        # ContentObserver for MediaStore
BackgroundWorker.kt            # WorkManager integration
```

#### Tasks

1. **Setup MethodChannel** (`platform_channel.dart`)
   - Define channel name: `com.example.photo_classifier/storage`
   - Create methods: `pickFolder`, `readFile`, `writeFile`, `deleteFile`, `startPhotoObserver`, `stopPhotoObserver`
   - Handle platform responses/errors

2. **Implement SAF Picker** (`StorageAccessFramework.kt`)
   - `ACTION_OPEN_DOCUMENT_TREE` for folder selection
   - Persist URI permissions via `ContentResolver.takePersistableUriPermission()`
   - Return persisted URIs to Flutter

3. **Create DocumentFile wrapper** (`StorageAccessFramework.kt`)
   - Methods: `listFiles`, `copyFile`, `moveFile`, `deleteFile`
   - All operations via DocumentFile API (not java.io.File)
   - Return results to Flutter via MethodChannel

4. **Setup WorkManager** (`BackgroundWorker.kt`)
   - Create `Worker` subclass for photo processing
   - Enqueue from Flutter via MethodChannel
   - Return progress/results via callback channel

**Success Criteria:**
- [ ] Flutter can open SAF folder picker
- [ ] Flutter can read/write files via SAF URIs
- [ ] Flutter can enqueue background work

---

### Wave 2: SAF Onboarding & Folder Discovery (Days 4-7)

**Goal:** Replace the placeholder onboarding with real SAF-based folder selection.

#### Files to Modify/Create

**lib/presentation/screens/**
```
onboarding_screen.dart         # Complete rewrite with SAF
folder_selection_screen.dart   # New: SAF folder picker UI
```

**lib/presentation/providers/**
```
onboarding_provider.dart       # Rewrite with SAF integration
folder_provider.dart           # Add SAF folder discovery
```

**lib/data/services/**
```
folder_discovery_service.dart  # Rewrite to use platform channel
saf_storage_service.dart       # New: SAF operations wrapper
```

#### Tasks

1. **Create SAF Storage Service** (`saf_storage_service.dart`)
   ```dart
   class SafStorageService {
     Future<String?> pickFolder();
     Future<List<String>> getFolderUris();
     Future<void> persistUriPermission(String uri);
     Future<List<FolderModel>> discoverFoldersInTree(String treeUri);
     Future<FileStats> getFolderStats(String folderUri);
   }
   ```

2. **Rewrite OnboardingScreen** (`onboarding_screen.dart`)
   - Remove placeholder, add real SAF picker
   - Step 1: Explain permissions needed
   - Step 2: Open SAF picker for `/Pictures` directory
   - Step 3: Show discovered subfolders
   - Step 4: User selects which folders to monitor
   - Persist selection via `PreferenceService`

3. **Implement Folder Discovery** (`folder_discovery_service.dart`)
   - Call platform channel: `discoverFoldersInTree(treeUri)`
   - Parse native response into `FolderModel` list
   - Filter system folders (`.thumbnails`, `Android`, etc.)
   - Count photos in each folder

4. **Update FolderProvider** (`folder_provider.dart`)
   - Add `selectFoldersProvider` for onboarding
   - Add `discoveredFoldersProvider` for main screen
   - Integrate with `DatabaseService` for persistence

**Success Criteria:**
- [ ] User can complete onboarding with real SAF permission
- [ ] Discovered folders appear in main screen
- [ ] Folder URIs persist across app restarts
- [ ] Permission survives app restart (via `takePersistableUriPermission`)

---

### Wave 3: Safe File Operations with Transaction Log (Days 8-12)

**Goal:** Implement copy-verify-delete pattern with crash recovery.

#### Files to Create

**lib/domain/models/**
```
file_operation_model.dart      # Operation type, status, metadata
```

**lib/data/services/**
```
file_operation_service.dart    # Copy-verify-delete orchestration
transaction_log_service.dart   # Transaction log CRUD
storage_checker_service.dart   # Available storage detection
```

**lib/data/repositories/**
```
file_operation_repository.dart # Offline-first repository
```

#### Tasks

1. **Create Transaction Log Service** (`transaction_log_service.dart`)
   ```dart
   class TransactionLogService {
     Future<String> beginTransaction(String sourceUri, String destUri, String type);
     Future<void> updateStatus(String id, String status, {String? error});
     Future<void> completeTransaction(String id);
     Future<List<TransactionModel>> getPendingTransactions();
     Future<void> replayTransaction(TransactionModel tx);
   }
   ```

2. **Implement File Operation Service** (`file_operation_service.dart`)
   ```dart
   class FileOperationService {
     Future<OperationResult> copyFile(String sourceUri, String destUri);
     Future<bool> verifyFile(String sourceUri, String destUri);
     Future<bool> deleteFile(String uri);
     Future<OperationResult> moveFileWithTransaction(String sourceUri, String destUri);
   }
   ```
   - **Copy:** Copy via platform channel, track bytes
   - **Verify:** Compare file size (not hash for performance)
   - **Delete:** Only after verification succeeds
   - **Transaction:** Log before copy, update after each step

3. **Create Storage Checker** (`storage_checker_service.dart`)
   ```dart
   class StorageCheckerService {
     Future<bool> hasSpaceForFile(String destUri, int fileSize);
     static const MIN_FREE_SPACE = 100 * 1024 * 1024; // 100MB buffer
   }
   ```

4. **Implement Crash Recovery** (`transaction_log_service.dart`)
   - On app startup: query pending transactions
   - For each pending: determine last known state
   - Resume from interrupted step or rollback
   - Log recovery result

**Success Criteria:**
- [ ] File move survives app kill mid-operation
- [ ] Storage full detected before copy attempt
- [ ] Pending transactions recovered on startup
- [ ] No data loss in interruption tests

---

### Wave 4: Folder Learning System (Days 13-17)

**Goal:** Sample photos from each folder, extract ML labels, aggregate patterns.

#### Files to Create

**lib/data/services/**
```
folder_learning_service.dart   # Label extraction & aggregation
photo_sampling_service.dart    # Smart photo sampling
```

**lib/domain/models/**
```
learned_label_model.dart       # Label + confidence + frequency
```

#### Tasks

1. **Implement Photo Sampler** (`photo_sampling_service.dart`)
   ```dart
   class PhotoSamplingService {
     Future<List<String>> samplePhotosFromFolder(String folderUri, {int count = 50});
   }
   ```
   - List files in folder via platform channel
   - Filter by image extensions
   - Randomly sample 50 photos (or all if < 50)

2. **Create Folder Learning Service** (`folder_learning_service.dart`)
   ```dart
   class FolderLearningService {
     Future<Map<String, double>> learnFolderLabels(String folderUri, List<String> photoPaths);
     Future<void> updateFolderLearnedLabels(String folderUri, Map<String, double> labels);
   }
   ```
   - Use `MlLabelingService` to analyze each sampled photo
   - Aggregate labels: sum confidence, divide by count
   - Boost labels matching folder name (exact +15%, partial +8%)
   - Store in `FolderModel.learnedLabels`

3. **Implement Learning WorkManager Worker** (Kotlin: `FolderLearningWorker.kt`)
   - Enqueue when folder added
   - Run with battery constraints
   - Report progress to Flutter
   - Update database on completion

4. **Update FolderProvider** (`folder_provider.dart`)
   - Add `learningProgressProvider` stream
   - Expose `startFolderLearning(folderUri)` method
   - Update UI with learning status

**Success Criteria:**
- [ ] 50 photos sampled per folder
- [ ] ML labels aggregated with confidence scores
- [ ] Folder name boost applied correctly
- [ ] Learning status visible in UI (PENDING → IN_PROGRESS → COMPLETED)

---

### Wave 5: Trash System (Days 18-20)

**Goal:** 7-day retention for deleted photos with auto-cleanup.

#### Files to Create

**lib/data/services/**
```
trash_service.dart           # Move to trash, restore, purge
trash_cleanup_worker.dart    # Daily cleanup scheduler
```

#### Tasks

1. **Implement Trash Service** (`trash_service.dart`)
   ```dart
   class TrashService {
     Future<String> moveToTrash(String originalUri, String fileName, int fileSize);
     Future<bool> restoreFromTrash(String originalUri);
     Future<void> purgeExpiredItems();
     Future<List<TrashItem>> getTrashItems();
   }
   ```
   - Create `.trash/` folder in app documents dir
   - Move file with timestamped name
   - Insert into `trash` table with `expires_at = now + 7 days`

2. **Create Daily Cleanup Worker** (Kotlin: `TrashCleanupWorker.kt`)
   - Schedule with `PeriodicWorkManager` (24h interval)
   - Query expired trash items
   - Delete files from disk
   - Remove from database

3. **Update DatabaseService** (`database_service.dart`)
   - Add `insertTrashItem`, `deleteTrashItem`, `getExpiredTrashItems`
   - Already has table schema, just implement methods

**Success Criteria:**
- [ ] Deleted photos recoverable for 7 days
- [ ] Expired items auto-deleted daily
- [ ] `.trash/` folder hidden from gallery (`.nomedia` file)

---

### Wave 6: Main Screen & Settings Polish (Days 21-27)

**Goal:** Complete the UI with real data, settings persistence, and startup recovery.

#### Files to Modify

**lib/presentation/screens/**
```
main_screen.dart             # Add recovery, permissions, storage warnings
onboarding_screen.dart       # Complete with all waves integrated
```

**lib/presentation/providers/**
```
onboarding_provider.dart     # Full SAF + learning integration
```

#### Tasks

1. **Implement Startup Recovery** (`main.dart` or dedicated service)
   ```dart
   Future<void> performStartupRecovery() async {
     final pending = await transactionLogService.getPendingTransactions();
     for (final tx in pending) {
       await transactionLogService.replayTransaction(tx);
     }
     await trashService.purgeExpiredItems();
   }
   ```
   - Run after onboarding complete check
   - Schedule background workers for pending learning

2. **Enhance MainScreen** (`main_screen.dart`)
   - Show permission warning if SAF access lost
   - Show storage warning if < 100MB free
   - Pull-to-refresh triggers folder rescan
   - Display learning status per folder

3. **Complete Settings** (expand modal or separate screen)
   - Confidence threshold (0.6 / 0.75 / 0.9) — already implemented
   - Notification preferences
   - Folder management (add/remove/relearn)
   - About section

**Success Criteria:**
- [ ] Crash recovery runs on startup
- [ ] Main screen shows real folder data with learning status
- [ ] Settings persist and affect behavior
- [ ] Permission/storage warnings visible when needed

---

## Testing Strategy

### Unit Tests (Dart)
- `transaction_log_service_test.dart` — state transitions, recovery logic
- `folder_learning_service_test.dart` — label aggregation, name boost
- `trash_service_test.dart` — expiration logic, restore

### Integration Tests (Flutter Driver)
- `onboarding_flow_test.dart` — complete onboarding with mock platform channel
- `folder_learning_flow_test.dart` — add folder → learning → completion

### Manual Testing (Android Device)
- Kill app mid-file-move, restart, verify recovery
- Fill storage to 99%, attempt move, verify warning
- Reject SAF permission, verify re-onboarding flow

---

## Platform Channel API Specification

### Methods (Flutter → Android)

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| `pickFolder` | `none` | `String?` (tree URI) | Open SAF picker, return persisted URI |
| `getFolderUris` | `none` | `List<String>` | All persisted tree URIs |
| `listFilesInTree` | `{String treeUri, String relativePath}` | `List<FileEntry>` | List files/folders in tree |
| `copyFile` | `{String sourceUri, String destUri}` | `bool` | Copy via SAF |
| `moveFile` | `{String sourceUri, String destUri}` | `bool` | Move via SAF |
| `deleteFile` | `{String uri}` | `bool` | Delete via SAF |
| `getFileStats` | `{String uri}` | `{size: int, lastModified: int}` | File metadata |
| `startPhotoObserver` | `none` | `void` | Register ContentObserver |
| `stopPhotoObserver` | `none` | `void` | Unregister ContentObserver |
| `enqueueWorker` | `{String workerType, Map<String, dynamic> data}` | `String` (work ID) | Schedule WorkManager job |
| `cancelWorker` | `{String workId}` | `bool` | Cancel WorkManager job |

### Callbacks (Android → Flutter)

| Callback | Parameters | Description |
|----------|------------|-------------|
| `onPhotoDetected` | `{String uri, String folder}` | New photo found by ContentObserver |
| `onWorkerProgress` | `{String workId, int progress, String? message}` | WorkManager progress update |
| `onPermissionLost` | `{String uri}` | SAF permission revoked |

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Platform channel complexity | High | Start with simple SAF picker, test early on device |
| WorkManager not triggering | High | Test on real device with Doze mode, add manual trigger |
| SAF URI permission lost | Medium | Re-request on startup, handle gracefully in UI |
| ML Kit slow on low-end | Medium | Benchmark, add timeout, batch processing |
| File move fails mid-operation | High | Transaction log + copy-verify-delete pattern |

---

## Definition of Done (Phase 1)

- [ ] User can complete onboarding with real SAF permission
- [ ] Discovered folders displayed in main screen
- [ ] Folder learning runs and persists labels
- [ ] File operations use transaction logging
- [ ] Crash recovery works after app kill
- [ ] Trash system with 7-day retention
- [ ] Settings persist and affect behavior
- [ ] All platform channel methods implemented and tested
- [ ] Unit tests for core services (>60% coverage)
- [ ] Manual testing on Android 10-14 device

---

## Files Checklist

### New Files (Estimated: 25 files)
- [ ] `lib/core/platform/platform_channel.dart`
- [ ] `lib/core/platform/android_storage_interface.dart`
- [ ] `lib/data/services/saf_storage_service.dart`
- [ ] `lib/data/services/transaction_log_service.dart`
- [ ] `lib/data/services/file_operation_service.dart`
- [ ] `lib/data/services/storage_checker_service.dart`
- [ ] `lib/data/services/folder_learning_service.dart`
- [ ] `lib/data/services/photo_sampling_service.dart`
- [ ] `lib/data/services/trash_service.dart`
- [ ] `lib/domain/models/file_operation_model.dart`
- [ ] `lib/domain/models/learned_label_model.dart`
- [ ] `android/app/src/main/kotlin/.../PhotoClassifierPlugin.kt`
- [ ] `android/app/src/main/kotlin/.../StorageAccessFramework.kt`
- [ ] `android/app/src/main/kotlin/.../PhotoObserverService.kt`
- [ ] `android/app/src/main/kotlin/.../BackgroundWorker.kt`
- [ ] `android/app/src/main/kotlin/.../FolderLearningWorker.kt`
- [ ] `android/app/src/main/kotlin/.../TrashCleanupWorker.kt`

### Modified Files (Estimated: 8 files)
- [ ] `lib/presentation/screens/onboarding_screen.dart` (rewrite)
- [ ] `lib/presentation/screens/main_screen.dart` (enhance)
- [ ] `lib/presentation/providers/folder_provider.dart` (expand)
- [ ] `lib/presentation/providers/onboarding_provider.dart` (expand)
- [ ] `lib/data/services/folder_discovery_service.dart` (rewrite)
- [ ] `lib/data/database/database_service.dart` (add trash methods)
- [ ] `android/app/build.gradle.kts` (add dependencies)
- [ ] `pubspec.yaml` (add platform_channel dependencies if needed)

---

## Estimated Timeline

| Wave | Duration | End Date |
|------|----------|----------|
| Wave 1: Platform Channel Foundation | 3 days | Day 3 |
| Wave 2: SAF Onboarding & Discovery | 4 days | Day 7 |
| Wave 3: Safe File Operations | 5 days | Day 12 |
| Wave 4: Folder Learning System | 5 days | Day 17 |
| Wave 5: Trash System | 3 days | Day 20 |
| Wave 6: Main Screen & Settings | 7 days | Day 27 |
| Buffer & Testing | 4 days | Day 31 |

**Total:** ~4 weeks (slightly over the 3-week estimate due to Flutter adaptation)

---

## Next Steps

1. **Approve this plan** — Confirm Flutter approach is correct
2. **Start Wave 1** — Platform channel foundation
3. **Test on device early** — Day 1 SAF picker test on real Android device
4. **Update STATE.md** — Track progress against this plan

---

*Generated: 2026-03-08*  
*Based on: 01-01 through 01-07 plans (adapted for Flutter)*
