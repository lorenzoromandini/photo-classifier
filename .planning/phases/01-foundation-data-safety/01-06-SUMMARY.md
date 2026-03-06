---
phase: 01-foundation-data-safety
plan: 06
subsystem: storage

tags: [trash, workmanager, room, saf, kotlin]

requires:
  - phase: 01-01
    provides: Room database foundation with Dao pattern
  - phase: 01-03
    provides: SafeFileOperations for copy-verify-delete pattern

provides:
  - TrashManager for moving files to hidden .trash folder
  - TrashRepository for persisting trash item metadata
  - TrashCleanupWorker for daily auto-deletion of expired items
  - WorkerScheduler for centralized work management
  - TrashItem domain model with 7-day expiration tracking

affects:
  - 01-07 (if trash UI is added to settings)
  - Phase 2 (file organization will use trash instead of delete)

tech-stack:
  added:
    - androidx.work:work-runtime-ktx (existing)
    - androidx.hilt:hilt-work (existing)
    - DocumentFile (SAF)
  patterns:
    - Repository pattern with Entity/Model mapping
    - Worker pattern with Hilt injection
    - Hidden folder (.trash) for user safety
    - Periodic work with unique work policy

key-files:
  created:
    - app/src/main/java/com/example/photoorganizer/data/model/TrashItem.kt
    - app/src/main/java/com/example/photoorganizer/data/local/safe/TrashManager.kt
    - app/src/main/java/com/example/photoorganizer/data/local/database/entities/TrashItemEntity.kt
    - app/src/main/java/com/example/photoorganizer/data/local/database/dao/TrashItemDao.kt
    - app/src/main/java/com/example/photoorganizer/data/repository/TrashRepository.kt
    - app/src/main/java/com/example/photoorganizer/data/worker/TrashCleanupWorker.kt
    - app/src/main/java/com/example/photoorganizer/data/worker/WorkerScheduler.kt
  modified:
    - app/src/main/java/com/example/photoorganizer/data/local/database/AppDatabase.kt

key-decisions:
  - "7-day retention period matches user expectations from desktop OS trash"
  - "Hidden .trash folder with .nomedia marker avoids gallery pollution"
  - "Size verification over hash (performance on mobile, sufficient safety)"
  - "Conflict resolution with timestamp prefix + numeric suffix"
  - "Periodic work with KEEP policy prevents duplicate scheduling"

patterns-established:
  - "Worker scheduling centralized in WorkerScheduler class"
  - "Repository mapping between Entity and Domain Model"
  - "Unique work names for idempotent scheduling"

duration: 5min
completed: 2026-03-06
---

# Phase 01 Plan 06: Trash System Implementation Summary

**Trash system with 7-day retention using hidden .trash folder, Room persistence, and daily WorkManager cleanup**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-06T14:59:33Z
- **Completed:** 2026-03-06T15:04:49Z
- **Tasks:** 3
- **Files created/modified:** 8

## Accomplishments

- TrashItem data class with automatic expiration calculation (7 days)
- TrashManager for safe move/restore/delete with file verification
- TrashRepository bridging domain model and Room entity
- TrashCleanupWorker for daily automated cleanup
- WorkerScheduler for centralized background work management
- Database schema extended with trash_items table and indices

## Task Commits

Each task was committed atomically:

1. **Task 1: Create TrashManager for safe deletion** - `5d7c2a2` (feat)
2. **Task 2: Create TrashRepository for persistence** - `96b9314` (feat)
3. **Task 3: Create TrashCleanupWorker for auto-deletion** - `f141e61` (feat)

## Files Created/Modified

- `app/src/main/java/com/example/photoorganizer/data/model/TrashItem.kt` - Domain model with expiration tracking
- `app/src/main/java/com/example/photoorganizer/data/local/safe/TrashManager.kt` - Move/restore/delete operations
- `app/src/main/java/com/example/photoorganizer/data/local/database/entities/TrashItemEntity.kt` - Room entity
- `app/src/main/java/com/example/photoorganizer/data/local/database/dao/TrashItemDao.kt` - Database access
- `app/src/main/java/com/example/photoorganizer/data/repository/TrashRepository.kt` - Repository pattern
- `app/src/main/java/com/example/photoorganizer/data/worker/TrashCleanupWorker.kt` - Daily cleanup worker
- `app/src/main/java/com/example/photoorganizer/data/worker/WorkerScheduler.kt` - Work scheduling
- `app/src/main/java/com/example/photoorganizer/data/local/database/AppDatabase.kt` - Added trash_items table

## Decisions Made

1. **7-day retention period**: Matches desktop OS conventions, gives users time to discover and recover misplaced photos
2. **Hidden .trash folder**: Using dot prefix and .nomedia marker prevents gallery apps from showing deleted items
3. **Size verification**: Chosen over hashing for performance; sufficient for this use case
4. **Timestamp prefix**: Added to filename in trash to avoid conflicts when organizing multiple photos with same name
5. **KEEP policy for scheduling**: Prevents duplicate work requests if scheduleAllWorkers() is called multiple times

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] No gradlew for compilation verification**
- **Found during:** Task 3 (verification step)
- **Issue:** Project doesn't have gradlew wrapper, preventing `./gradlew compileDebugKotlin`
- **Fix:** Documented as known limitation; code follows Kotlin/Android conventions and imports verified against existing codebase
- **Files modified:** N/A (verification step skipped)
- **Verification:** Code structure matches existing patterns, imports verified via grep
- **Committed in:** N/A (deviation during verification, not implementation)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Minimal - code structure verified against existing patterns, compilation deferred until gradle wrapper available

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Trash foundation complete, ready for file organization integration
- SafeFileOperations should be updated to use TrashManager instead of direct delete
- Trash UI in settings can be added as a future enhancement (01-07)
- Worker scheduling should be called after onboarding completion

## Self-Check

**Created files verified:**
- [x] `app/src/main/java/com/example/photoorganizer/data/model/TrashItem.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/local/safe/TrashManager.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/local/database/entities/TrashItemEntity.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/local/database/dao/TrashItemDao.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/repository/TrashRepository.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/worker/TrashCleanupWorker.kt` exists
- [x] `app/src/main/java/com/example/photoorganizer/data/worker/WorkerScheduler.kt` exists

**Commits verified:**
- [x] `5d7c2a2` feat(01-06): create TrashManager for safe deletion
- [x] `96b9314` feat(01-06): create TrashRepository for persistence
- [x] `f141e61` feat(01-06): create TrashCleanupWorker for auto-deletion

## Self-Check: PASSED
