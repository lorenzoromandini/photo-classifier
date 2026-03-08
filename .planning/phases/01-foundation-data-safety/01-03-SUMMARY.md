---
phase: 01-foundation-data-safety
plan: 03
subsystem: data-safety
tags:
  - crash-recovery
  - file-operations
  - transaction-logging
  - safe-operations
  - flutter-rewrite
requires:
  - 01-01
  - 01-02
provides:
  - SafeFileService.safeMove() with copy-verify-delete
  - TransactionRepository for logging all operations
  - Recovery logic for incomplete transactions
  - Storage check before operations (100MB buffer)
  - RetryService with exponential backoff
affects:
  - Phase 2: detection pipeline (safe file moves)
  - Phase 3: trash system (delete operations)
tech-stack:
  added:
    - SafeFileHelper.kt: Native SAF file operations
    - FileResult<T>: Type-safe error handling
    - TransactionStatus: State machine for operations
    - RetryService: Exponential backoff strategy
  patterns:
    - Repository pattern for transaction management
    - Result pattern for error handling
    - State machine for transaction lifecycle
key-files:
  created:
    - android/app/src/main/kotlin/com/example/photo_classifier/SafeFileHelper.kt
    - android/app/src/main/kotlin/com/example/photo_classifier/PlatformChannelHandler.kt (modified)
    - lib/data/platform/file_operation_service.dart
    - lib/data/repositories/transaction_repository.dart
    - lib/data/services/retry_service.dart
    - lib/data/services/safe_file_service.dart
    - lib/domain/models/file_result.dart
    - lib/domain/models/transaction_status.dart
    - lib/domain/models/recovery_result.dart
    - lib/domain/models/storage_state.dart
  modified:
    - None
key-decisions:
  - Size verification over hash-based for performance on mobile
  - 100MB minimum storage buffer for safety margin
  - Three retry strategies (default/conservative/aggressive)
  - MD5 checksum as secondary verification after size check
metrics:
  start-time: "2026-03-08T02:06:24Z"
  completed: "2026-03-08T02:15:00Z"
  duration-minutes: 9
  tasks: 4
  files-created: 10
  files-modified: 1
---

# Phase 01 Plan 03: Safe File Operations Summary

**Crash-safe file operations with transaction logging and retry logic for Flutter implementation**

## One-Liner

Implemented crash-safe file operations using copy-verify-delete pattern with SQLite transaction logging, exponential backoff retry, and 100MB storage buffer.

## Performance

- **Duration:** 9 min
- **Started:** 2026-03-08T02:06:24Z
- **Completed:** 2026-03-08T02:15:00Z
- **Tasks:** 4
- **Files created:** 10
- **Files modified:** 1

## Accomplishments

- SafeFileHelper.kt with native SAF file operations via ContentResolver
- FileOperationService wrapping platform channel with FileResult<T> pattern
- TransactionRepository with full CRUD and recovery logic
- RetryService with exponential backoff and jitter
- SafeFileService.safeMove() implementing copy-verify-delete with transaction logging

## Task Commits

1. **Task 1: SafeFileHelper for native SAF operations** - `9c42868` (feat)
2. **Task 1 continued: PlatformChannelHandler updates** - `7a9454a` (feat)
3. **Task 2: FileOperationService with FileResult pattern** - `c439007` (feat)
4. **Task 3: TransactionRepository for logging and recovery** - `4731be2` (feat)
5. **Task 4: SafeFileService with copy-verify-delete** - `2de18cb` (feat)

## Files Created

### Kotlin (Android Native)

1. **SafeFileHelper.kt** (220 lines) - Native SAF file operations:
   - `copyFile(sourceUri, destUri)` - Copies via ContentResolver, returns bytes copied
   - `verifyFile(sourceUri, destUri)` - Compares sizes and MD5 checksums
   - `deleteFile(uri)` - Deletes via DocumentFile
   - `getFileSize(uri)` - Returns file size in bytes
   - `getAvailableStorage()` - Returns available bytes via StatFs
   - `exists(uri)` - Checks if file exists

2. **PlatformChannelHandler.kt** (modified, +250 lines) - Added 6 new method handlers:
   - `copyFile`, `verifyFile`, `deleteFile`, `getFileSize`, `getAvailableStorage`, `exists`
   - Maps errors to error codes (STORAGE_FULL, PERMISSION_DENIED, etc.)

### Dart (Flutter)

3. **file_result.dart** (47 lines) - Type-safe error wrapper:
   - `FileResult<T>` with success/failure states
   - `FileErrorCodes` constants

4. **file_operation_service.dart** (197 lines) - Platform channel wrapper:
   - All file operation methods returning `FileResult<T>`

5. **transaction_status.dart** (88 lines) - Status enums:
   - `TransactionStatus` enum (pending, copying, verifying, deleting, completed, failed)
   - `OperationType` enum (copy, move, delete)

6. **recovery_result.dart** (37 lines) - Recovery result model

7. **transaction_repository.dart** (143 lines) - Transaction CRUD:
   - `createTransaction()`, `updateStatus()`, `incrementRetryCount()`
   - `getPendingTransactions()`, `getFailedTransactions()`
   - `cleanupOldTransactions()`

8. **storage_state.dart** (47 lines) - Storage info:
   - `StorageInfo` with `hasSpaceFor100MB`
   - `StorageState` enum (ok, low, critical)

9. **retry_service.dart** (85 lines) - Exponential backoff:
   - `RetryConfig` with three presets (default, conservative, aggressive)
   - `executeWithRetry()` with jitter

10. **safe_file_service.dart** (328 lines) - Main service:
    - `safeMove()` with full copy-verify-delete
    - `safeCopy()` without delete
    - `checkStorage()` for pre-op checks
    - `recoverPendingOperations()` for crash recovery

## Decisions Made

1. **Size verification over hash-only**: Use size comparison as primary, MD5 checksum as secondary. Size check is instant, hash catches corruption.

2. **100MB minimum storage buffer**: Prevents mid-operation failures. Typical photo: 3-10MB, buffer allows ~10-30 photos of headroom.

3. **Three retry strategies**:
   - Conservative: 3 retries, 500ms-1min delay
   - Default: 5 retries, 1s-5min delay
   - Aggressive: 10 retries, 2s-10min delay

4. **MD5 checksum (not SHA-256)**: Sufficient for integrity checking, faster computation on mobile devices.

5. **Non-retryable errors**: STORAGE_FULL and PERMISSION_DENIED require user action, don't retry automatically.

## Transaction Flow

### Successful Move
```
1. createTransaction() → pending
2. checkStorage() → OK
3. updateStatus() → copying
4. copyFile() with retry
5. updateStatus() → verifying
6. verifyFile() → size + checksum match
7. updateStatus() → deleting
8. deleteFile() with retry
9. updateStatus() → completed
```

### Recovery Logic
```
recoverPendingOperations() replays based on status:
- pending/copying: Retry full cycle
- verifying: Verify → if OK, delete; else retry copy
- deleting: Retry delete
- completed/failed: Terminal states (no action)
```

## Integration Points

### Requires from Previous Plans
- **01-01**: DatabaseService for transaction storage
- **01-02**: Platform channel infrastructure

### Provides for Future Plans
- **01-04**: Safe file moves for folder setup
- **01-05**: Safe operations during label extraction
- **01-06**: Delete operations for 7-day trash cleanup
- **01-07**: Storage state for UI feedback

## Deviations from Plan

**None - plan executed exactly as written.**

All 4 tasks completed per specification with all deliverables implemented.

## Issues Encountered

**None.** Kotlin and Dart code follows existing patterns successfully.

## Self-Check: PASSED

All created files verified:
- ✅ SafeFileHelper.kt exists
- ✅ PlatformChannelHandler.kt modified
- ✅ file_operation_service.dart exists
- ✅ transaction_repository.dart exists
- ✅ retry_service.dart exists
- ✅ safe_file_service.dart exists
- ✅ All model files created

## Next Phase Readiness

- **01-04 (Onboarding)**: Can use safeMove for initial folder configuration
- **01-05 (Folder Learning)**: Safe file operations during ML labeling
- **01-06 (Trash)**: Uses deleteFile for cleanup, recovery for interrupted deletion
- **01-07 (Main Screen)**: Storage checks for UI feedback

## Verification Notes

All components follow existing codebase patterns:
- SafeFileHelper matches SafHelper code style
- FileResult pattern matches existing Result.dart pattern
- TransactionRepository uses DatabaseService from 01-01
- Platform channel method names consistent (Kotlin ↔ Dart)

**Manual testing recommended:** Requires Android device to verify SAF operations and crash recovery.

---

*Phase: 01-foundation-data-safety*  
*Completed: 2026-03-08*  
*Flutter rewrite from previous Kotlin implementation*
