---
phase: 01-foundation-data-safety
plan: 07
subsystem: ui

tags:
  - compose
  - viewmodel
  - hilt
  - navigation

requires:
  - phase: 01-foundation-data-safety
    provides:
      - TransactionRepository for recovery
      - FolderRepository for folder display
      - UserPreferencesRepository for settings
      - WorkerScheduler for background tasks
      - StorageChecker for storage warnings
    depends_on: ["01-04", "01-05", "01-06"]

provides:
  - MainScreen with folder list and learning status
  - SettingsScreen with confidence threshold (0.6/0.75/0.9)
  - Application startup recovery
  - Permission denial handling with re-onboarding
  - Storage full warnings

affects:
  - 02-detection-classification
  - user-experience
  - settings-persistence

tech-stack:
  added:
    - Compose Material3 SegmentedButton (for confidence selector)
    - Compose pull-to-refresh
  patterns:
    - MVVM with StateFlow
    - Navigation Compose with type-safe routes
    - HiltViewModel dependency injection
    - LazyColumn for performance

key-files:
  created:
    - app/src/main/java/com/example/photoorganizer/PhotoOrganizerApplication.kt (updated with recovery)
    - app/src/main/java/com/example/photoorganizer/data/repository/RecoveryStartup.kt
    - app/src/main/java/com/example/photoorganizer/ui/main/MainScreen.kt
    - app/src/main/java/com/example/photoorganizer/ui/main/MainViewModel.kt
    - app/src/main/java/com/example/photoorganizer/ui/components/FolderCard.kt
    - app/src/main/java/com/example/photoorganizer/ui/components/FolderList.kt
    - app/src/main/java/com/example/photoorganizer/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/example/photoorganizer/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/example/photoorganizer/ui/components/ConfidenceSlider.kt
  modified:
    - app/src/main/java/com/example/photoorganizer/navigation/PhotoOrganizerNavigation.kt (added routes)

key-decisions:
  - "Default confidence threshold is 0.9 (High) per user decision for conservative organization"
  - "Three confidence levels: Low (0.6), Medium (0.75), High (0.9)"
  - "Application performs crash recovery on startup after onboarding completion"
  - "Permission loss triggers re-onboarding flow via MainScreen warning banner"
  - "Storage warnings shown when space below 100MB threshold"

duration: 45min
completed: 2026-03-06
---

# Phase 1 Plan 07: Main Screen and Settings UI

**Main screen with discovered folders and learning progress, settings with confidence threshold (0.6/0.75/0.9), crash recovery on startup, permission/storage warnings**

## Performance

- **Duration:** 45 min
- **Started:** 2026-03-06
- **Completed:** 2026-03-06
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments

1. **Application startup recovery**: Transaction log replay on every app start, logs results, schedules background workers
2. **MainScreen implementation**: Folder list with learning status (PENDING/IN_PROGRESS/COMPLETED), pull-to-refresh, permission/storage warnings
3. **SettingsScreen implementation**: Confidence threshold selector (Low/Medium/High), folder list, notification toggles, about section
4. **Navigation integration**: Routes for main/settings with back navigation, re-onboarding on permission loss

## Task Commits

Each task was committed atomically:

1. **Task 1: Application startup recovery** - `45cd706` (feat)
2. **Task 2: MainScreen with folder list** - `1b28a50` (feat)
3. **Task 3: SettingsScreen with confidence threshold** - `8af4830` (feat)
4. **Navigation integration** - `f80e201` (feat)
5. **RecoveryStartup helper** - `a4531ff` (feat)

**Plan metadata:** `a4531ff` (docs: complete plan)

## Files Created/Modified

- `app/src/main/java/com/example/photoorganizer/PhotoOrganizerApplication.kt` - Updated with startup recovery, storage checking, worker scheduling
- `app/src/main/java/com/example/photoorganizer/data/repository/RecoveryStartup.kt` - Helper class for crash recovery (testable)
- `app/src/main/java/com/example/photoorganizer/ui/main/MainScreen.kt` - Main UI with folder list, warnings, navigation
- `app/src/main/java/com/example/photoorganizer/ui/main/MainViewModel.kt` - State management, permission checking
- `app/src/main/java/com/example/photoorganizer/ui/components/FolderCard.kt` - Folder card showing name, photo count, status
- `app/src/main/java/com/example/photoorganizer/ui/components/FolderList.kt` - LazyColumn with grouping by learning status
- `app/src/main/java/com/example/photoorganizer/ui/settings/SettingsScreen.kt` - Settings with confidence, folders, about
- `app/src/main/java/com/example/photoorganizer/ui/settings/SettingsViewModel.kt` - Settings state management
- `app/src/main/java/com/example/photoorganizer/ui/components/ConfidenceSlider.kt` - Segmented buttons for threshold (0.6/0.75/0.9)
- `app/src/main/java/com/example/photoorganizer/navigation/PhotoOrganizerNavigation.kt` - Added MAIN and SETTINGS routes

## Decisions Made

- **Default confidence threshold: 0.9 (High)** - Per user decision for conservative organization with fewer mistakes
- **Three preset levels**: Low (0.6) for more coverage, Medium (0.75) balanced, High (0.9) conservative
- **Recovery after onboarding**: Only runs recovery if onboarding is complete (has folder permissions)
- **Re-onboarding on permission loss**: MainScreen shows warning banner with "Fix" button to navigate back to onboarding

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added RecoveryStartup.kt file**
- **Found during:** Task 1 (Application class with startup recovery)
- **Issue:** Plan referenced `RecoveryStartup.kt` in `files_modified` and `key_links` sections but file didn't exist
- **Fix:** Created `RecoveryStartup.kt` helper class with `initialize()` method for testability
- **Files modified:** `app/src/main/java/com/example/photoorganizer/data/repository/RecoveryStartup.kt`
- **Verification:** File exists and implements startup recovery delegation
- **Committed in:** `a4531ff`

---

**Total deviations:** 1 auto-fixed (1 blocking - missing file reference)
**Impact on plan:** Minimal - file was expected by plan, added to match specification

## Issues Encountered

1. **Navigation placeholder**: Existing navigation had `MainScreenPlaceholder()` - replaced with actual `MainScreen` and added `SettingsScreen` route.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Phase 1 Foundation & Data Safety is COMPLETE.**

All Phase 1 requirements implemented:
- ✅ Room Database & Proto DataStore (01-01)
- ✅ SAF DataSource & Repository (01-02)
- ✅ Crash-Safe File Operations (01-03)
- ✅ Onboarding Flow (01-04)
- ✅ Folder Learning System (01-05)
- ✅ Trash System (01-06)
- ✅ Main Screen & Settings (01-07)

**Ready for Phase 2: Detection & Classification Pipeline**

Phase 2 dependencies satisfied:
- Transaction log for recovery operations
- User preferences with confidence threshold
- Folder learning with labels
- Navigation framework
- Settings UI for configuration

---
*Phase: 01-foundation-data-safety*
*Completed: 2026-03-06*
