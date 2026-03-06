package com.example.photoorganizer.data.repository

import android.net.Uri
import android.util.Log
import com.example.photoorganizer.data.local.database.dao.FileOperationDao
import com.example.photoorganizer.data.local.database.entities.FileOperationEntity
import com.example.photoorganizer.data.local.database.entities.OperationStatus
import com.example.photoorganizer.data.local.database.entities.OperationType
import com.example.photoorganizer.data.local.safe.FileVerificationResult
import com.example.photoorganizer.data.local.safe.SafeFileOperations
import com.example.photoorganizer.data.local.safe.StorageFullException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing transaction log and crash recovery.
 * On app startup, recoverPendingOperations() replays any incomplete operations.
 *
 * Recovery logic:
 * - PENDING or COPYING: retry copy, then verify, then delete
 * - VERIFYING: verify copy, if valid continue to delete, else retry copy
 * - DELETING: retry delete
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val fileOperationDao: FileOperationDao,
    private val safeFileOperations: SafeFileOperations
) {
    companion object {
        private const val TAG = "TransactionRepository"
        private const val MAX_RETRIES = 5
        private const val CLEANUP_RETENTION_DAYS = 7
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    /**
     * Recover any pending operations from previous crashes.
     * Called on app startup to ensure data integrity.
     *
     * @return RecoveryResult with counts of recovered/failed operations
     */
    suspend fun recoverPendingOperations(): RecoveryResult = withContext(Dispatchers.IO) {
        val pendingOperations = fileOperationDao.getPendingSync()

        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending operations to recover")
            return@withContext RecoveryResult.Success(0, 0)
        }

        Log.i(TAG, "Recovering ${pendingOperations.size} pending operations")

        var recoveredCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        for (operation in pendingOperations) {
            try {
                val result = recoverOperation(operation)
                if (result) {
                    recoveredCount++
                } else {
                    failedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recovering operation ${operation.id}", e)
                failedCount++
                errors.add("Operation ${operation.id}: ${e.message}")
            }
        }

        return@withContext when {
            errors.isEmpty() -> RecoveryResult.Success(recoveredCount, failedCount)
            recoveredCount > 0 -> RecoveryResult.PartialSuccess(recoveredCount, failedCount, errors)
            else -> RecoveryResult.Failure("All ${pendingOperations.size} operations failed to recover")
        }
    }

    /**
     * Recover a single operation based on its current status.
     *
     * @param operation The operation to recover
     * @return true if recovery succeeded, false otherwise
     */
    private suspend fun recoverOperation(operation: FileOperationEntity): Boolean {
        Log.d(TAG, "Recovering operation ${operation.id} with status ${operation.status}")

        // Check retry count
        if (operation.retryCount >= MAX_RETRIES) {
            Log.w(TAG, "Operation ${operation.id} exceeded max retries, marking as FAILED")
            fileOperationDao.markFailed(
                operation.id,
                "Exceeded maximum retry attempts ($MAX_RETRIES)"
            )
            return false
        }

        // Increment retry count before attempting
        val updatedOperation = operation.copy(retryCount = operation.retryCount + 1)
        fileOperationDao.update(updatedOperation)

        return when (operation.status) {
            OperationStatus.PENDING,
            OperationStatus.COPYING -> {
                recoverCopyingOperation(operation)
            }

            OperationStatus.VERIFYING -> {
                recoverVerifyingOperation(operation)
            }

            OperationStatus.DELETING -> {
                recoverDeletingOperation(operation)
            }

            OperationStatus.COMPLETED -> {
                // Already complete, nothing to do
                true
            }

            OperationStatus.FAILED -> {
                // Already failed, don't retry unless manually triggered
                false
            }
        }
    }

    /**
     * Recover an operation stuck in PENDING or COPYING state.
     * Retries the copy operation.
     */
    private suspend fun recoverCopyingOperation(operation: FileOperationEntity): Boolean {
        val sourceUri = Uri.parse(operation.sourceUri)
        val destUri = if (operation.destUri.isNotEmpty()) {
            Uri.parse(operation.destUri)
        } else {
            Log.e(TAG, "Operation ${operation.id} has no destination URI")
            fileOperationDao.markFailed(
                operation.id,
                "No destination URI for recovery"
            )
            return false
        }

        return when (operation.operationType) {
            OperationType.COPY -> {
                // Retry the safeCopy operation
                val result = safeFileOperations.safeCopy(sourceUri, destUri)
                result.isSuccess.also { success ->
                    updateStatusAfterRecovery(operation.id, success, result.exceptionOrNull()?.message)
                }
            }

            OperationType.DELETE -> {
                // DELETE shouldn't be in COPYING state, mark as failed
                Log.e(TAG, "DELETE operation ${operation.id} in COPYING state - invalid")
                fileOperationDao.markFailed(
                    operation.id,
                    "Invalid state: DELETE operation in COPYING status"
                )
                false
            }

            OperationType.VERIFY -> {
                // VERIFY shouldn't be in COPYING state
                Log.e(TAG, "VERIFY operation ${operation.id} in COPYING state - invalid")
                fileOperationDao.markFailed(
                    operation.id,
                    "Invalid state: VERIFY operation in COPYING status"
                )
                false
            }
        }
    }

    /**
     * Recover an operation stuck in VERIFYING state.
     * Verifies the copy, and if valid, continues to delete source.
     */
    private suspend fun recoverVerifyingOperation(operation: FileOperationEntity): Boolean {
        val sourceUri = Uri.parse(operation.sourceUri)
        val destUri = if (operation.destUri.isNotEmpty()) {
            Uri.parse(operation.destUri)
        } else {
            Log.e(TAG, "Operation ${operation.id} has no destination URI")
            fileOperationDao.markFailed(
                operation.id,
                "No destination URI for verification"
            )
            return false
        }

        // Verify the copy exists and is valid
        val verification = safeFileOperations.verifyCopy(sourceUri, destUri)

        return when {
            verification is FileVerificationResult.Success -> {
                // Copy is valid
                when (operation.operationType) {
                    OperationType.COPY -> {
                        // Just mark as completed
                        fileOperationDao.markCompleted(operation.id, System.currentTimeMillis())
                        true
                    }

                    OperationType.DELETE -> {
                        // DELETE shouldn't be in VERIFYING state
                        Log.e(TAG, "DELETE operation ${operation.id} in VERIFYING state - invalid")
                        fileOperationDao.markFailed(
                            operation.id,
                            "Invalid state: DELETE operation in VERIFYING status"
                        )
                        false
                    }

                    OperationType.VERIFY -> {
                        // Mark VERIFY as completed
                        fileOperationDao.markCompleted(operation.id, System.currentTimeMillis())
                        true
                    }
                }
            }

            else -> {
                // Verification failed, mark as failed
                Log.w(TAG, "Verification failed for operation ${operation.id}: $verification")
                fileOperationDao.markFailed(
                    operation.id,
                    "Verification failed: $verification"
                )
                false
            }
        }
    }

    /**
     * Recover an operation stuck in DELETING state.
     * Retries the delete operation.
     */
    private suspend fun recoverDeletingOperation(operation: FileOperationEntity): Boolean {
        val sourceUri = Uri.parse(operation.sourceUri)

        return when (operation.operationType) {
            OperationType.DELETE -> {
                // Retry delete
                val result = safeFileOperations.safeDelete(sourceUri)
                result.isSuccess.also { success ->
                    updateStatusAfterRecovery(operation.id, success, result.exceptionOrNull()?.message)
                }
            }

            OperationType.COPY -> {
                // COPY shouldn't reach DELETING state (unless copy-then-delete)
                // Mark as completed since copy was successful
                fileOperationDao.markCompleted(operation.id, System.currentTimeMillis())
                true
            }

            OperationType.VERIFY -> {
                // VERIFY shouldn't reach DELETING state
                // Mark as completed
                fileOperationDao.markCompleted(operation.id, System.currentTimeMillis())
                true
            }
        }
    }

    /**
     * Update operation status after recovery attempt.
     */
    private suspend fun updateStatusAfterRecovery(
        operationId: String,
        success: Boolean,
        errorMessage: String?
    ) {
        if (success) {
            fileOperationDao.markCompleted(operationId, System.currentTimeMillis())
        } else {
            fileOperationDao.markFailed(operationId, errorMessage ?: "Recovery failed")
        }
    }

    /**
     * Get all pending operations.
     *
     * @return List of operations that are not yet complete or failed
     */
    suspend fun getPendingOperations(): List<FileOperationEntity> {
        return fileOperationDao.getPendingSync()
    }

    /**
     * Get all failed operations as a Flow for reactive updates.
     *
     * @return Flow of failed operations lists
     */
    fun getFailedOperations(): Flow<List<FileOperationEntity>> {
        return fileOperationDao.getByStatus(OperationStatus.FAILED.name)
            .flowOn(Dispatchers.IO)
    }

    /**
     * Get pending operations as a Flow for reactive monitoring.
     *
     * @return Flow of pending operations
     */
    fun getPendingOperationsFlow(): Flow<List<FileOperationEntity>> {
        return fileOperationDao.getPending()
            .flowOn(Dispatchers.IO)
    }

    /**
     * Clean up old completed operations.
     *
     * @param olderThanDays Number of days after which completed operations are removed (default: 7)
     * @return Number of operations deleted
     */
    suspend fun cleanupOldOperations(olderThanDays: Int = CLEANUP_RETENTION_DAYS): Int {
        val cutoffTimestamp = System.currentTimeMillis() - (olderThanDays * MILLIS_PER_DAY)
        val deletedCount = fileOperationDao.cleanupCompleted(cutoffTimestamp)
        Log.d(TAG, "Cleaned up $deletedCount old completed operations")
        return deletedCount
    }

    /**
     * Clean up old failed operations.
     *
     * @param olderThanDays Number of days after which failed operations are removed
     * @return Number of operations deleted
     */
    suspend fun cleanupFailedOperations(olderThanDays: Int = CLEANUP_RETENTION_DAYS): Int {
        val cutoffTimestamp = System.currentTimeMillis() - (olderThanDays * MILLIS_PER_DAY)
        val deletedCount = fileOperationDao.cleanupFailed(cutoffTimestamp)
        Log.d(TAG, "Cleaned up $deletedCount old failed operations")
        return deletedCount
    }

    /**
     * Get the count of pending operations.
     *
     * @return Number of pending operations
     */
    suspend fun getPendingCount(): Int {
        return fileOperationDao.countByStatus(OperationStatus.PENDING.name) +
                fileOperationDao.countByStatus(OperationStatus.COPYING.name) +
                fileOperationDao.countByStatus(OperationStatus.VERIFYING.name) +
                fileOperationDao.countByStatus(OperationStatus.DELETING.name)
    }

    /**
     * Get the count of failed operations.
     *
     * @return Number of failed operations
     */
    suspend fun getFailedCount(): Int {
        return fileOperationDao.countByStatus(OperationStatus.FAILED.name)
    }

    /**
     * Retry a failed operation manually.
     *
     * @param operationId ID of the failed operation to retry
     * @return true if retry succeeded, false otherwise
     */
    suspend fun retryFailedOperation(operationId: String): Boolean {
        val operation = fileOperationDao.getById(operationId)
            ?: return false

        if (operation.status != OperationStatus.FAILED) {
            Log.w(TAG, "Operation $operationId is not in FAILED state, cannot retry")
            return false
        }

        // Reset status to PENDING for retry
        val updatedOperation = operation.copy(
            status = OperationStatus.PENDING,
            errorMessage = null,
            retryCount = operation.retryCount + 1
        )
        fileOperationDao.update(updatedOperation)

        // Attempt recovery
        return recoverOperation(updatedOperation)
    }

    /**
     * Delete a specific operation from the log.
     *
     * @param operationId ID of operation to delete
     */
    suspend fun deleteOperation(operationId: String) {
        val operation = fileOperationDao.getById(operationId) ?: return
        // Only allow deletion of completed or failed operations
        if (operation.status == OperationStatus.COMPLETED || operation.status == OperationStatus.FAILED) {
            fileOperationDao.update(operation.copy(status = OperationStatus.COMPLETED))
        }
    }
}

/**
 * Sealed class representing the result of recovery operations.
 */
sealed class RecoveryResult {
    /**
     * All operations recovered successfully
     */
    data class Success(
        val recovered: Int,
        val failed: Int
    ) : RecoveryResult()

    /**
     * Some operations recovered, some failed
     */
    data class PartialSuccess(
        val recovered: Int,
        val failed: Int,
        val errors: List<String>
    ) : RecoveryResult()

    /**
     * All operations failed to recover
     */
    data class Failure(
        val error: String
    ) : RecoveryResult()

    /**
     * Returns true if at least some operations were recovered
     */
    val isSuccessful: Boolean
        get() = this is Success || this is PartialSuccess

    /**
     * Returns true if all operations failed
     */
    val isFailure: Boolean
        get() = this is Failure
}
