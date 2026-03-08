---
phase: 01-foundation-data-safety
plan: 06
subsystem: storage

tags: [trash, workmanager, sqflite, platform-channel, flutter, kotlin]

requires:
  - phase: 01-01
    provides: SQLite database foundation with CRUD operations
  - phase: 01-03
    provides: FileOperationService with copy-verify-delete pattern

provides:
  - TrashItem domain model with 7-day expiration tracking
  - TrashRepository for database persistence and queries
  - TrashService for safe move/restore/delete operations
  - TrashCleanupWorker for daily automatic deletion of expired items
  - TrashProvider for reactive UI state management
  - TrashItemTile widget for displaying trash items

affects:
  - 01-07 (trash UI can be integrated into settings)
  - Phase 2 (file organization will use trash instead of permanent delete)

tech-stack:
  added:
    - workmanager ^0.5.2 (already in dependencies)
    - DocumentFile (SAF)
  patterns:
    - Repository pattern for trash persistence
    - Platform channel for native Android trash operations
    - Hidden .trash folder with .nomedia marker
    - Periodic WorkManager task with KEEP policy
    - Reactive stream polling for UI updates

key-files:
  created:
    - lib/domain/models/trash_item.dart
    - lib/data/repositories/trash_repository.dart
    - lib/data/services/trash_service.dart
    - lib/data/workers/trash_cleanup_worker.dart
    - lib/presentation/providers/trash_provider.dart
    - lib/presentation/widgets/trash_item_tile.dart
    - android/app/src/main/kotlin/com/example/photo_classifier/TrashHelper.kt
  modified:
    - lib/data/database/database_service.dart
    - android/app/src/main/kotlin/com/example/photo_classifier/PlatformChannelHandler.kt
    - lib/data/platform/file_operation_service.dart
    - lib/main.dart

key-decisions:
  - "7-day retention period matches desktop OS conventions for user safety"
  - "Hidden .trash folder with .nomedia marker prevents gallery app pollution"
  - "Size verification sufficient for mobile (vs hash-based) per performance requirements"
  - "Timestamp prefix on trashed files prevents naming conflicts"
  - "KEEP policy for periodic work prevents duplicate scheduling"
  - "Reactive stream via polling (500ms) for UI updates"

patterns-established:
  - "Platform channel extension pattern for new native operations"
  - "Worker isolation with static callback dispatcher"
  - "State notifier pattern for mutation actions"

duration: 11min
completed: 2026-03-08
---

# Phase 01 Plan 06: Trash System Implementation Summary

**Trash system with 7-day retention using hidden .trash folder, SQLite persistence, and daily WorkManager cleanup**

## Performance

- **Duration:** 11 min
- **Started:** 2026-03-08T02:29:45Z
- **Completed:** 2026-03-08T02:40:51Z
- **Tasks:** 4/4
- **Files created/modified:** 11

## Accomplishments

- TrashItem domain model with automatic 7-day expiration calculation
- TrashRepository with full CRUD operations and reactive streams
- TrashService implementing safe move/restore/delete via platform channels
- TrashHelper.kt for native Android trash folder management
- TrashCleanupWorker for daily automatic deletion of expired items
- TrashProvider and TrashItemTile for UI integration
- .nomedia file creation to hide trash from gallery apps

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Trash Entity and Repository** - `8085c68` (feat)
2. **Task 2: Create TrashService with Platform Channel** - `09b37bf` (feat)
3. **Task 3: Create TrashCleanupWorker** - `329fd53` (feat)
4. **Task 4: Create TrashProvider for UI** - `4db3b99` (feat)

## Files Created/Modified

### Created:
- `lib/domain/models/trash_item.dart` - Domain model with 7-day expiration
- `lib/data/repositories/trash_repository.dart` - Repository pattern implementation
- `lib/data/services/trash_service.dart` - Service layer with platform channel integration
- `lib/data/workers/trash_cleanup_worker.dart` - WorkManager background worker
- `lib/presentation/providers/trash_provider.dart` - Riverpod state management
- `lib/presentation/widgets/trash_item_tile.dart` - UI widget for trash items
- `android/app/src/main/kotlin/.../TrashHelper.kt` - Native Android trash operations

### Modified:
- `lib/data/database/database_service.dart` - Added trash table with id field and indexes
- `android/app/src/main/kotlin/.../PlatformChannelHandler.kt` - Added 5 trash method handlers
- `lib/data/platform/file_operation_service.dart` - Added invokePlatformMethod helper
- `lib/main.dart` - WorkManager initialization and cleanup scheduling

## Decisions Made

1. **7-day retention period**: Matches desktop OS conventions (Windows Recycle Bin, macOS Trash), giving users time to discover and recover misplaced photos without consuming storage indefinitely.

2. **Hidden .trash folder**: Using dot prefix and .nomedia marker prevents gallery apps from displaying deleted items, maintaining clean photo browsing experience.

3. **Size verification over hashing**: Per established pattern from Plan 01-03, size verification is sufficient for this use case while providing better mobile performance.

4. **Timestamp prefix on trashed files**: Prevents naming conflicts when multiple files with same name are deleted, e.g., "IMG_001.jpg" from different folders.

5. **KEEP policy for scheduling**: Prevents duplicate work requests if `scheduleDailyCleanup()` is called multiple times during app lifecycle.

6. **Polling-based reactive streams**: SQLite lacks built-in reactive support; polling every 500ms provides responsive UI without complex database triggers.

## Deviations from Plan

### None - Plan Executed Exactly as Written

All tasks completed according to plan specification. No auto-fixes, blocking issues, or architectural changes were required.

## Issues Encountered

None - implementation proceeded smoothly following established patterns from Plans 01-01 through 01-03.

## User Setup Required

None - no external service configuration required. WorkManager scheduling is automatic after onboarding completion.

## Next Phase Readiness

- Trash foundation complete and ready for integration with file organization (Phase 2)
- SafeFileService should be updated to use TrashService.moveToTrash() instead of direct delete
- Trash UI can be integrated into settings screen (Plan 01-07) as a trash viewing/restoring interface
- Worker scheduling is automatic in main.dart after onboarding

## Success Criteria Verification

- [x] Deleted photos moved to .trash folder instead of permanent delete (TrashService.moveToTrash)
- [x] Trash items tracked in SQLite with expiration date (trash table with expires_at field)
- [x] 7-day retention enforced (TrashItem.create sets expiresAt = movedAt + 7 days)
- [x] TrashCleanupWorker runs daily via WorkManager (registerPeriodicTask with 1-day frequency)
- [x] Restore functionality moves files back to original location (TrashService.restoreFromTrash)
- [x] Trash visible in app settings (via TrashProvider and trashCountProvider)
- [x] SafeFileService integration point ready (TrashService exists, integration in next phase)
- [x] Hidden .trash folder in Pictures/ root (TrashHelper.createTrashFolder with .nomedia)

## Self-Check

**Created files verified:**
- [x] `lib/domain/models/trash_item.dart` exists
- [x] `lib/data/repositories/trash_repository.dart` exists
- [x] `lib/data/services/trash_service.dart` exists
- [x] `lib/data/workers/trash_cleanup_worker.dart` exists
- [x] `lib/presentation/providers/trash_provider.dart` exists
- [x] `lib/presentation/widgets/trash_item_tile.dart` exists
- [x] `android/app/src/main/kotlin/com/example/photo_classifier/TrashHelper.kt` exists

**Commits verified:**
- [x] `8085c68` feat(01-06): create TrashItem model and repository
- [x] `09b37bf` feat(01-06): create TrashService with platform channel support
- [x] `329fd53` feat(01-06): create TrashCleanupWorker for daily auto-deletion
- [x] `4db3b99` feat(01-06): create TrashProvider for UI state management

## Self-Check: PASSED
