import 'package:flutter/foundation.dart';
import '../platform/file_operation_service.dart';
import '../repositories/transaction_repository.dart';
import '../services/retry_service.dart';
import '../../domain/models/file_result.dart';
import '../../domain/models/storage_state.dart';
import '../../domain/models/transaction_status.dart';
import '../../domain/models/recovery_result.dart';

/// Service implementing crash-safe file operations with copy-verify-delete pattern
class SafeFileService {
  final FileOperationService _fileOperationService;
  final TransactionRepository _transactionRepository;
  final RetryService _retryService;

  static const int minStorageBuffer = 100 * 1024 * 1024; // 100MB
  static const int maxRetries = 5;

  SafeFileService({
    required FileOperationService fileOperationService,
    required TransactionRepository transactionRepository,
    RetryService? retryService,
  })  : _fileOperationService = fileOperationService,
        _transactionRepository = transactionRepository,
        _retryService = retryService ?? RetryService();

  /// Safely move file from source to destination
  /// Implements copy → verify → delete pattern with transaction logging
  Future<FileResult<void>> safeMove(String sourceUri, String destUri) async {
    debugPrint('SafeFileService: Starting safeMove from $sourceUri to $destUri');

    // Step 1: Create transaction log
    final transactionId = await _transactionRepository.createTransaction(
      sourceUri: sourceUri,
      destUri: destUri,
      operationType: OperationType.move,
    );

    try {
      // Step 2: Check storage before any operations
      final storageResult = await checkStorage();
      if (storageResult.isFailure) {
        await _transactionRepository.markFailed(
          transactionId,
          storageResult.error ?? 'Storage check failed',
        );
        return FileResult.failure(
          storageResult.error ?? 'Storage check failed',
          code: FileErrorCodes.storageFull,
        );
      }

      final storageInfo = storageResult.data!;
      if (!storageInfo.hasSpaceFor100MB) {
        final error = 'Insufficient storage: need ${minStorageBuffer ~/ 1024 ~/ 1024}MB buffer';
        await _transactionRepository.markFailed(transactionId, error);
        return FileResult.failure(error, code: FileErrorCodes.storageFull);
      }

      // Step 3: Copy
      debugPrint('SafeFileService: Status -> copying');
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.copying);

      final copyResult = await _retryService.executeWithRetry(
        () => _fileOperationService.copyFile(sourceUri, destUri),
        config: RetryConfig.defaultConfig,
        onRetry: (attempt, delay) {
          debugPrint('SafeFileService: Copy retry $attempt after ${delay.inSeconds}s');
        },
      );

      if (copyResult.isFailure) {
        throw _FileOperationException(copyResult.error ?? 'Copy failed', copyResult.errorCode);
      }

      debugPrint('SafeFileService: Copied ${copyResult.data} bytes');

      // Step 4: Verify
      debugPrint('SafeFileService: Status -> verifying');
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.verifying);

      final verifyResult = await _fileOperationService.verifyFile(sourceUri, destUri);

      if (verifyResult.isFailure || !(verifyResult.data ?? false)) {
        throw _FileOperationException(
          verifyResult.error ?? 'Verification failed',
          FileErrorCodes.verificationFailed,
        );
      }

      debugPrint('SafeFileService: Verification passed');

      // Step 5: Delete source
      debugPrint('SafeFileService: Status -> deleting');
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.deleting);

      final deleteResult = await _retryService.executeWithRetry(
        () => _fileOperationService.deleteFile(sourceUri),
        config: RetryConfig.defaultConfig,
        onRetry: (attempt, delay) {
          debugPrint('SafeFileService: Delete retry $attempt after ${delay.inSeconds}s');
        },
      );

      if (deleteResult.isFailure) {
        throw _FileOperationException(deleteResult.error ?? 'Delete failed', deleteResult.errorCode);
      }

      // Step 6: Mark completed
      debugPrint('SafeFileService: Status -> completed');
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.completed);

      return FileResult.success(null);
    } on _FileOperationException catch (e) {
      debugPrint('SafeFileService: Operation failed: ${e.message}');
      // Check retry count and either retry or mark failed
      final retryCount = await _transactionRepository.getRetryCount(transactionId);
      if (retryCount < maxRetries && _isRetryable(e.errorCode)) {
        await _transactionRepository.incrementRetryCount(transactionId);
        await _transactionRepository.updateStatus(
          transactionId,
          TransactionStatus.pending,
          errorMessage: e.message,
        );
        return FileResult.failure(e.message, code: e.errorCode);
      } else {
        await _transactionRepository.markFailed(transactionId, e.message);
        return FileResult.failure(e.message, code: e.errorCode);
      }
    } catch (e) {
      debugPrint('SafeFileService: Unexpected error: $e');
      await _transactionRepository.markFailed(transactionId, e.toString());
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Safely copy file (without delete)
  Future<FileResult<void>> safeCopy(String sourceUri, String destUri) async {
    debugPrint('SafeFileService: Starting safeCopy from $sourceUri to $destUri');

    final transactionId = await _transactionRepository.createTransaction(
      sourceUri: sourceUri,
      destUri: destUri,
      operationType: OperationType.copy,
    );

    try {
      // Check storage
      final storageResult = await checkStorage();
      if (storageResult.isFailure) {
        await _transactionRepository.markFailed(
          transactionId,
          storageResult.error ?? 'Storage check failed',
        );
        return FileResult.failure(
          storageResult.error ?? 'Storage check failed',
          code: FileErrorCodes.storageFull,
        );
      }

      // Copy
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.copying);
      final copyResult = await _retryService.executeWithRetry(
        () => _fileOperationService.copyFile(sourceUri, destUri),
        config: RetryConfig.defaultConfig,
      );

      if (copyResult.isFailure) {
        throw _FileOperationException(copyResult.error ?? 'Copy failed', copyResult.errorCode);
      }

      // Verify
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.verifying);
      final verifyResult = await _fileOperationService.verifyFile(sourceUri, destUri);

      if (verifyResult.isFailure || !(verifyResult.data ?? false)) {
        throw _FileOperationException(
          verifyResult.error ?? 'Verification failed',
          FileErrorCodes.verificationFailed,
        );
      }

      // Complete
      await _transactionRepository.updateStatus(transactionId, TransactionStatus.completed);
      return FileResult.success(null);
    } catch (e) {
      await _transactionRepository.markFailed(transactionId, e.toString());
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Check available storage
  Future<FileResult<StorageInfo>> checkStorage() async {
    try {
      final result = await _fileOperationService.getAvailableStorage();

      if (result.isFailure) {
        return FileResult.failure(
          result.error ?? 'Failed to check storage',
          code: result.errorCode,
        );
      }

      final availableBytes = result.data ?? 0;
      final totalBytes = 0; // Can't get total from Android easily
      final state = StorageThresholds.determineState(availableBytes);

      return FileResult.success(StorageInfo(
        availableBytes: availableBytes,
        totalBytes: totalBytes,
        state: state,
      ));
    } catch (e) {
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Recover pending transactions on app startup
  /// Replays incomplete operations from crash recovery
  Future<RecoveryResult> recoverPendingOperations() async {
    debugPrint('SafeFileService: Starting recovery of pending transactions');

    final pending = await _transactionRepository.getPendingTransactions();
    if (pending.isEmpty) {
      debugPrint('SafeFileService: No pending transactions to recover');
      return RecoveryResult.success(recovered: 0);
    }

    debugPrint('SafeFileService: Found ${pending.length} pending transactions');

    int recovered = 0;
    int failed = 0;
    final errors = <String>[];

    for (final transaction in pending) {
      debugPrint('SafeFileService: Recovering transaction ${transaction.id} (status: ${transaction.status})');

      try {
        // Check retry count
        if (transaction.retryCount >= maxRetries) {
          errors.add('Transaction ${transaction.id}: max retries exceeded');
          failed++;
          continue;
        }

        final result = await _recoverTransaction(transaction);

        if (result.isSuccess) {
          recovered++;
          debugPrint('SafeFileService: Recovered transaction ${transaction.id}');
        } else {
          final newRetryCount = transaction.retryCount + 1;
          if (newRetryCount < maxRetries && _isRetryable(result.errorCode)) {
            await _transactionRepository.incrementRetryCount(transaction.id);
            await _transactionRepository.updateStatus(
              transaction.id,
              TransactionStatus.pending,
              errorMessage: result.error,
            );
          } else {
            await _transactionRepository.markFailed(
              transaction.id,
              result.error ?? 'Recovery failed',
            );
          }
          errors.add('Transaction ${transaction.id}: ${result.error}');
          failed++;
        }
      } catch (e) {
        errors.add('Transaction ${transaction.id}: $e');
        failed++;
      }
    }

    final success = failed == 0;
    debugPrint('SafeFileService: Recovery complete - recovered: $recovered, failed: $failed');

    return RecoveryResult(
      recovered: recovered,
      failed: failed,
      errors: errors,
      success: success,
    );
  }

  /// Recover a single transaction based on its status
  Future<FileResult<void>> _recoverTransaction(TransactionModel transaction) async {
    final status = TransactionStatusX.fromString(transaction.status);

    switch (status) {
      case TransactionStatus.pending:
      case TransactionStatus.copying:
        // Retry full copy-verify-delete cycle
        return safeMove(transaction.sourceUri, transaction.destUri);

      case TransactionStatus.verifying:
        // Verify and continue
        final verifyResult = await _fileOperationService.verifyFile(
          transaction.sourceUri,
          transaction.destUri,
        );

        if (verifyResult.isSuccess && (verifyResult.data ?? false)) {
          // Verification passed, complete delete
          await _transactionRepository.updateStatus(transaction.id, TransactionStatus.deleting);
          final deleteResult = await _fileOperationService.deleteFile(transaction.sourceUri);

          if (deleteResult.isSuccess) {
            await _transactionRepository.updateStatus(
              transaction.id,
              TransactionStatus.completed,
            );
            return FileResult.success(null);
          } else {
            return FileResult.failure(
              deleteResult.error ?? 'Delete failed during recovery',
              code: deleteResult.errorCode,
            );
          }
        } else {
          // Verification failed, retry full cycle
          return safeMove(transaction.sourceUri, transaction.destUri);
        }

      case TransactionStatus.deleting:
        // Source should still exist, retry delete
        final deleteResult = await _fileOperationService.deleteFile(transaction.sourceUri);

        if (deleteResult.isSuccess) {
          await _transactionRepository.updateStatus(transaction.id, TransactionStatus.completed);
          return FileResult.success(null);
        } else {
          return FileResult.failure(
            deleteResult.error ?? 'Delete failed during recovery',
            code: deleteResult.errorCode,
          );
        }

      case TransactionStatus.completed:
      case TransactionStatus.failed:
        // Already terminal states
        return FileResult.success(null);
    }
  }

  /// Check if error code indicates a retryable error
  bool _isRetryable(String? errorCode) {
    if (errorCode == FileErrorCodes.storageFull) return false;
    if (errorCode == FileErrorCodes.permissionDenied) return false;
    return true;
  }
}

/// Internal exception for file operations
class _FileOperationException implements Exception {
  final String message;
  final String? errorCode;

  _FileOperationException(this.message, [this.errorCode]);

  @override
  String toString() => message;
}
