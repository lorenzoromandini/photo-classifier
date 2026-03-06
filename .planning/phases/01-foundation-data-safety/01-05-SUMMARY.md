---
phase: 01-foundation-data-safety
plan: 05
subsystem: ml

# Dependency graph
requires:
  - phase: 01-01
    provides: Room database with FolderEntity, LearningStatus
  - phase: 01-02
    provides: SafDataSource for photo listing
provides:
  - ML Kit Image Labeling wrapper (MlLabelingDataSource)
  - LearningRepository for folder learning
  - FolderLearningWorker for background processing
  - WorkManager integration with Hilt
  - PhotoOrganizerApplication with Timber logging
affects:
  - 01-05 (Category Management - uses learned labels)
  - 02-01 (Detection Pipeline - uses ML labels)
  - 02-04 (Auto-Discover Categories - depends on learned labels)

# Tech tracking
tech-stack:
  added:
    - "com.google.mlkit:image-labeling:17.0.9 (bundled model)"
    - "androidx.work:work-runtime-ktx:2.9.0"
    - "androidx.hilt:hilt-work:1.1.0"
    - "com.jakewharton.timber:timber:5.0.1"
  patterns:
    - WorkManager with @HiltWorker for DI
    - suspendCancellableCoroutine for async ML Kit calls
    - Result pattern for error handling
    - Progress tracking via WorkManager Data

key-files:
  created:
    - app/src/main/java/com/example/photoorganizer/data/local/ml/MlLabelingDataSource.kt
    - app/src/main/java/com/example/photoorganizer/data/model/MlLabel.kt
    - app/src/main/java/com/example/photoorganizer/data/model/LearningResult.kt
    - app/src/main/java/com/example/photoorganizer/data/repository/LearningRepository.kt
    - app/src/main/java/com/example/photoorganizer/data/worker/FolderLearningWorker.kt
    - app/src/main/java/com/example/photoorganizer/di/WorkerModule.kt
    - app/src/main/java/com/example/photoorganizer/PhotoOrganizerApplication.kt
  modified:
    - app/src/main/java/com/example/photoorganizer/data/local/database/dao/FolderDao.kt
    - app/build.gradle.kts

key-decisions:
  - "Learning confidence threshold 0.5 (vs organization 0.9) per user decision"
  - "50 photos sampled per folder for learning phase"
  - "Folder name boosts matching labels: exact +0.15, partial +0.08"
  - "Used bundled ML Kit model (5.7MB, works offline)"
  - "WorkManager with battery/storage constraints for background learning"

patterns-established:
  - "ML Kit: Use suspendCancellableCoroutine for async callbacks"
  - "WorkManager: @HiltWorker with @AssistedInject for DI in workers"
  - "Label aggregation: Combine frequency (30%) + confidence (70%)"
  - "Progress tracking: WorkManager Data with progress keys"

# Metrics
duration: 12min
completed: 2026-03-06
---

# Phase 01 Plan 05: Folder Learning System Summary

**ML Kit Image Labeling with folder learning - samples 50 photos per folder, aggregates labels with confidence scoring and folder name boosting, runs in background via WorkManager with battery-aware constraints.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-03-06T14:59:29Z
- **Completed:** 2026-03-06T16:11:42Z
- **Tasks:** 3
- **Files created/modified:** 9

## Accomplishments

- ML Kit Image Labeling wrapper with learning (0.5f) and organization (0.9f) thresholds
- LearningResult data model with label aggregation and JSON serialization
- LearningRepository that samples 50 photos and aggregates ML labels
- Folder name boosting algorithm (exact match +15%, partial match +8%)
- FolderLearningWorker with @HiltWorker for DI support
- WorkManager constraints (battery not low, storage not low)
- Progress tracking via WorkManager Data API
- PhotoOrganizerApplication with Timber logging and HiltWorkerFactory
- getPendingFolders() query added to FolderDao

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ML Kit Image Labeling wrapper** - `34ad3cb` (feat)
   - MlLabel and LearningResult data classes
   - MlLabelingDataSource with dual thresholds
   - ML Kit and WorkManager dependencies

2. **Task 2: Create LearningRepository for label aggregation** - `fe31f71` (feat)
   - LearningRepository with sampling and aggregation logic
   - Folder name boosting (exact +0.15f, partial +0.08f)
   - LearningProgress data class

3. **Task 3: Create FolderLearningWorker with WorkManager** - `23a5df5` (feat)
   - FolderLearningWorker with @HiltWorker
   - Battery/storage constraints
   - PhotoOrganizerApplication with HiltWorkerFactory
   - Timber logging integration

**Plan metadata:** `TBD` (docs: complete plan)

## Files Created/Modified

**ML Kit Integration:**
- `app/src/main/java/com/example/photoorganizer/data/local/ml/MlLabelingDataSource.kt` - ML Kit wrapper with suspend functions
- `app/src/main/java/com/example/photoorganizer/data/model/MlLabel.kt` - Label with confidence threshold helpers
- `app/src/main/java/com/example/photoorganizer/data/model/LearningResult.kt` - Aggregated learning results

**Learning Repository:**
- `app/src/main/java/com/example/photoorganizer/data/repository/LearningRepository.kt` - Sample photos, aggregate labels, boost folder matches

**Background Worker:**
- `app/src/main/java/com/example/photoorganizer/data/worker/FolderLearningWorker.kt` - WorkManager worker with progress tracking
- `app/src/main/java/com/example/photoorganizer/di/WorkerModule.kt` - Hilt module for WorkManager
- `app/src/main/java/com/example/photoorganizer/PhotoOrganizerApplication.kt` - Application class with HiltWorkerFactory

**DAO Updates:**
- `app/src/main/java/com/example/photoorganizer/data/local/database/dao/FolderDao.kt` - Added getPendingFolders() query

**Build:**
- `app/build.gradle.kts` - ML Kit, WorkManager, Hilt Work, Timber dependencies

## Decisions Made

1. **Learning threshold 0.5 vs Organization 0.9** - Per user decision: lower threshold during learning captures more diverse labels; higher threshold during organization ensures accuracy
2. **50 photos sampled per folder** - User decision: sufficient for pattern recognition without excessive processing
3. **Folder name boost algorithm** - Exact match +15%, partial match +8% - combines visual and contextual signals
4. **Bundled ML Kit model** - Adds 5.7MB but works offline, no API keys needed
5. **WorkManager constraints** - Battery not low + storage not low to respect user device resources

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added Application class with HiltWorkerFactory**
- **Found during:** Task 3 (Worker configuration)
- **Issue:** @HiltWorker requires HiltWorkerFactory configured, which needs Application class
- **Fix:** Created PhotoOrganizerApplication implementing Configuration.Provider
- **Files created:** PhotoOrganizerApplication.kt
- **Verification:** WorkerModule simplified, DI works correctly
- **Committed in:** 23a5df5 (Task 3)

**2. [Rule 2 - Missing Critical] Added Timber logging dependency**
- **Found during:** Task 3 (FolderLearningWorker)
- **Issue:** Worker uses Timber for structured logging but dependency not in build.gradle
- **Fix:** Added `com.jakewharton.timber:timber:5.0.1` to dependencies
- **Files modified:** app/build.gradle.kts
- **Verification:** Timber available for Worker logging
- **Committed in:** 23a5df5 (Task 3)

**3. [Rule 2 - Missing Critical] Added getPendingFolders() to FolderDao**
- **Found during:** Task 2 (LearningRepository)
- **Issue:** Repository needs to query pending folders, DAO only had Flow-based queries
- **Fix:** Added `suspend fun getPendingFolders()` returning List<FolderEntity>
- **Files modified:** FolderDao.kt
- **Verification:** LearningRepository.getPendingFolders() compiles
- **Committed in:** fe31f71 (Task 2)

---

**Total deviations:** 3 auto-fixed (1 blocking, 2 missing critical)
**Impact on plan:** All necessary for correct functionality. No scope creep.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **Ready for 01-05 (Category Management):** Learned labels available for auto-discover
- **Ready for 02-01 (Detection Pipeline):** MlLabelingDataSource ready for organization
- **Ready for 02-04 (Auto-Discover Categories):** Folder learning foundation complete

**No blockers.**

---
*Phase: 01-foundation-data-safety*
*Completed: 2026-03-06*

## Self-Check: PASSED

- [x] MlLabelingDataSource exists with learning/organization thresholds
- [x] LearningResult model with JSON serialization
- [x] LearningRepository samples 50 photos and aggregates labels
- [x] FolderLearningWorker with @HiltWorker annotation
- [x] WorkerModule for Hilt DI
- [x] PhotoOrganizerApplication with HiltWorkerFactory
- [x] Timber dependency added
- [x] getPendingFolders() query in FolderDao
- [x] 3 commits with proper format
- [x] SUMMARY.md created in plan directory
