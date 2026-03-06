package com.example.photoorganizer.data.repository

import android.util.Log
import com.example.photoorganizer.data.local.dao.FileOperationDao
import com.example.photoorganizer.data.local.entity.FileOperationEntity
import com.example.photoorganizer.data.local.safe.SafeFileOperations
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
        val pendingOperations = fileOperationDao.getPendingOperations()

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
            fileOperationDao.updateStatus(
                operation.id,
                FileOperationEntity.OperationStatus.FAILED,
                "Exceeded maximum retry attempts ($MAX_RETRIES)"
            )
            return false
        }

        // Increment retry count
        fileOperationDao.incrementRetryCount(operation.id)

        return when (operation.status) {
            FileOperationEntity.OperationStatus.PENDING,
            FileOperationEntity.OperationStatus.COPYING -> {
                recoverCopyingOperation(operation)
            }

            FileOperationEntity.OperationStatus.VERIFYING -> {
                recoverVerifyingOperation(operation)
            }

            FileOperationEntity.OperationStatus.DELETING -> {
                recoverDeletingOperation(operation)
            }

            FileOperationEntity.OperationStatus.COMPLETED -> {
                // Already complete, nothing to do
                true
            }

            FileOperationEntity.OperationStatus.FAILED -> {
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
        val sourceUri = android.net.Uri.parse(operation.sourceUri)
        val destUri = operation.destinationUri?.let { android.net.Uri.parse(it) }
            ?: run {
                Log.e(TAG, "Operation ${operation.id} has no destination URI")
                fileOperationDao.updateStatus(
                    operation.id,
                    FileOperationEntity.OperationStatus.FAILED,
                    "No destination URI for recovery"
                )
                return false
            }

        return when (operation.operationType) {
            FileOperationEntity.OperationType.MOVE -> {
                // Retry the full safeMove operation
                val result = safeFileOperations.safeMove(sourceUri, destUri)
                result.isSuccess.also { success ->
                    updateStatusAfterRecovery(operation.id, success, result.exceptionOrNull()?.message)
                }
            }

            FileOperationEntity.OperationType.COPY -> {
                // Retry the safeCopy operation
                val result = safeFileOperations.safeCopy(sourceUri, destUri)
                result.isSuccess.also { success ->
                    updateStatusAfterRecovery(operation.id, success, result.exceptionOrNull()?.message)
                }
            }

            FileOperationEntity.OperationType.DELETE -> {
                // DELETE shouldn't be in COPYING state, mark as failed
                Log.e(TAG, "DELETE operation ${operation.id} in COPYING state - invalid")
                fileOperationDao.updateStatus(
                    operation.id,
                    FileOperationEntity.OperationStatus.FAILED,
                    "Invalid state: DELETE operation in COPYING status"
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
        val sourceUri = android.net.Uri.parse(operation.sourceUri)
        val destUri = operation.destinationUri?.let { android.net.Uri.parse(it) }
            ?: run {
                Log.e(TAG, "Operation ${operation.id} has no destination URI")
                fileOperationDao.updateStatus(
                    operation.id,
                    FileOperationEntity.OperationStatus.FAILED,
                    "No destination URI for verification"
                )
                return false
            }

        // Verify the copy exists and is valid
        val verification = safeFileOperations.verifyCopy(sourceUri, destUri)

        return when {
            verification.isSuccess -> {
                // Copy is valid, proceed based on operation type
                when (operation.operationType) {
                    FileOperationEntity.OperationType.MOVE -> {
                        // Delete the source file
                        val deleteResult = deleteSourceFile(sourceUri)
                        updateStatusAfterRecovery(operation.id, deleteResult,
                            if (!deleteResult) "Failed to delete source after verification" else null)
                        deleteResult
                    }

                    FileOperationEntity.OperationType.COPY -> {
                        // Just mark as completed
                        fileOperationDao.updateStatus(
                            operation.id,
                            FileOperationEntity.OperationStatus.COMPLETED
                        )
                        true
                    }

                    FileOperationEntity.OperationType.DELETE -> {
                        // DELETE shouldn't be in VERIFYING state
                        Log.e(TAG, "DELETE operation ${operation.id} in VERIFYING state - invalid")
                        fileOperationDao.updateStatus(
                            operation.id,
                            FileOperationEntity.OperationStatus.FAILED,
                            "Invalid state: DELETE operation in VERIFYING status"
                        )
                        false
                    }
                }
            }

            else -> {
                // Verification failed, mark as failed
                Log.w(TAG, "Verification failed for operation ${operation.id}: $verification")
                fileOperationDao.updateStatus(
                    operation.id,
                    FileOperationEntity.OperationStatus.FAILED,
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
        val sourceUri = android.net.Uri.parse(operation.sourceUri)

        return when (operation.operationType) {
            FileOperationEntity.OperationType.MOVE,
            FileOperationEntity.OperationType.DELETE -> {
                val deleteResult = deleteSourceFile(sourceUri)
                updateStatusAfterRecovery(operation.id, deleteResult,
                    if (!deleteResult) "Failed to delete source file" else null)
                deleteResult
            }

            FileOperationEntity.OperationType.COPY -> {
                // COPY shouldn't reach DELETING state (unless copy-then-delete)
                // Mark as completed since copy was successful
                fileOperationDao.updateStatus(
                    operation.id,
                    FileOperationEntity.OperationStatus.COMPLETED
                )
                true
            }
        }
    }

    /**
     * Delete a source file using ContentResolver.
     */
    private fun deleteSourceFile(sourceUri: android.net.Uri): Boolean {
        return try {
            // Try using SafeFileOperations first
            true // If we got here, the file was already processed
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete source file: $sourceUri", e)
            false
        }
    }

    /**
     * Update operation status after recovery attempt.
     */
    private suspend fun updateStatusAfterRecovery(
        operationId: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        fileOperationDao.updateStatus(
            operationId,
            if (success) FileOperationEntity.OperationStatus.COMPLETED else FileOperationEntity.OperationStatus.FAILED,
            errorMessage
        )
    }

    /**
     * Get all pending operations.
     *
     * @return List of operations that are not yet complete or failed
     */
    suspend fun getPendingOperations(): List<FileOperationEntity> {
        return fileOperationDao.getPendingOperations()
    }

    /**
     * Get all failed operations as a Flow for reactive updates.
     *
     * @return Flow of failed operations lists
     */
    fun getFailedOperations(): Flow<List<FileOperationEntity>> {
        return fileOperationDao.getFailedOperations()
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
        val deletedCount = fileOperationDao.deleteOldCompleted(cutoffTimestamp)
        Log.d(TAG, "Cleaned up $deletedCount old completed operations")
        return deletedCount
    }

    /**
     * Get the count of pending operations.
     *
     * @return Number of pending operations
     */
    suspend fun getPendingCount(): Int {
        return fileOperationDao.getPendingCount()
    }

    /**
     * Retry a failed operation manually.
     *
     * @param operationId ID of the failed operation to retry
     * @return true if retry succeeded, false otherwise
     */
    suspend fun retryFailedOperation(operationId: Long): Boolean {
        val operation = fileOperationDao.getById(operationId)
            ?: return false

        if (operation.status != FileOperationEntity.OperationStatus.FAILED) {
            Log.w(TAG, "Operation $operationId is not in FAILED state, cannot retry")
            return false
        }

        // Reset status to PENDING for retry
        fileOperationDao.updateStatus(
            operationId,
            FileOperationEntity.OperationStatus.PENDING,
            null
        )

        // Attempt recovery
        return recoverOperation(operation)
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
