package com.example.photoorganizer.data.repository

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles startup recovery operations.
 * 
 * Separate class for testability - Application delegates to this
 * for crash recovery on every app startup.
 *
 * Recovery flow:
 * 1. Check if onboarding is complete
 * 2. Recover pending operations from transaction log
 * 3. Log results: "Recovered X operations, Y failed"
 *
 * @property transactionRepository Repository for transaction operations
 */
@Singleton
class RecoveryStartup @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Initialize crash recovery.
     * Called from Application.onCreate after dependencies are ready.
     *
     * @return RecoveryResult with counts of recovered/failed operations
     */
    suspend fun initialize(): RecoveryResult {
        Timber.i("Starting crash recovery...")

        return try {
            val result = transactionRepository.recoverPendingOperations()

            when (result) {
                is RecoveryResult.Success -> {
                    Timber.i("Recovery complete: ${result.recovered} operations recovered, ${result.failed} failed")
                }
                is RecoveryResult.PartialSuccess -> {
                    Timber.w("Recovery partial: ${result.recovered} operations recovered, ${result.failed} failed")
                    result.errors.forEach { error ->
                        Timber.w("Recovery error: $error")
                    }
                }
                is RecoveryResult.Failure -> {
                    Timber.e("Recovery failed: ${result.error}")
                }
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Error during startup recovery")
            RecoveryResult.Failure("Unexpected error: ${e.message}")
        }
    }

    /**
     * Check if recovery is needed (has pending operations).
     *
     * @return true if there are pending operations to recover
     */
    suspend fun hasPendingOperations(): Boolean {
        return try {
            val pendingCount = transactionRepository.getPendingCount()
            pendingCount > 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for pending operations")
            false
        }
    }

    /**
     * Get count of pending operations.
     *
     * @return Number of pending operations, or -1 on error
     */
    suspend fun getPendingCount(): Int {
        return try {
            transactionRepository.getPendingCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get pending count")
            -1
        }
    }

    /**
     * Get count of failed operations.
     *
     * @return Number of failed operations, or -1 on error
     */
    suspend fun getFailedCount(): Int {
        return try {
            transactionRepository.getFailedCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get failed count")
            -1
        }
    }
}
