package com.example.photoorganizer.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.example.photoorganizer.data.local.safe.TrashManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily worker for automatic trash cleanup.
 *
 * Runs periodically (every 24 hours) to permanently delete photos
 * that have exceeded the 7-day retention period. Uses WorkManager
 * for reliable background execution with battery-aware constraints.
 *
 * This worker is scheduled after onboarding completion and persists
 * across app restarts using unique work name "trash_cleanup".
 *
 * @see TrashManager
 * @see TrashItem
 */
@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trashManager: TrashManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TrashCleanupWorker"

        /** Unique work name for trash cleanup scheduling */
        const val WORK_NAME = "trash_cleanup"

        /** Repeat interval in hours (24 hours = daily) */
        const val REPEAT_INTERVAL_HOURS = 24L

        /** Initial delay in minutes (start 1 hour after app launch/onboarding) */
        const val INITIAL_DELAY_MINUTES = 60L

        /** Key for deleted count in output data */
        const val OUTPUT_DELETED_COUNT = "deleted_count"

        /**
         * Create a periodic work request for daily trash cleanup.
         *
         * Uses battery-aware constraints to avoid running during low battery.
         * Existing work with the same name is kept (not replaced).
         */
        fun createWorkRequest(): WorkRequest {
            return PeriodicWorkRequestBuilder<TrashCleanupWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setInitialDelay(INITIAL_DELAY_MINUTES, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
        }
    }

    /**
     * Execute trash cleanup.
     *
     * Gets expired items from TrashManager and permanently deletes them.
     * Returns success with deleted count regardless of individual item
     * failures - each item failure is logged but doesn't block others.
     *
     * @return Result.success with deleted count, or Result.retry if critical error
     */
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting daily trash cleanup")

        return try {
            // Get current timestamp
            val now = System.currentTimeMillis()

            // Get expired items (older than 7 days)
            val expiredItems = trashManager.getExpiredItems(now)
            Log.d(TAG, "Found ${expiredItems.size} expired items to delete")

            if (expiredItems.isEmpty()) {
                Log.d(TAG, "No expired items to clean up")
                return Result.success(
                    Data.Builder()
                        .putInt(OUTPUT_DELETED_COUNT, 0)
                        .build()
                )
            }

            // Delete each expired item
            var deletedCount = 0
            var failedCount = 0

            for (item in expiredItems) {
                trashManager.permanentlyDelete(item).fold(
                    onSuccess = {
                        deletedCount++
                        Log.d(TAG, "Deleted: ${item.fileName}")
                    },
                    onFailure = { e ->
                        failedCount++
                        Log.e(TAG, "Failed to delete: ${item.fileName}", e)
                        // Continue with other items - don't let one failure block cleanup
                    }
                )
            }

            Log.d(TAG, "Cleanup complete: $deletedCount deleted, $failedCount failed")

            // Return success even if some items failed - they'll be retried next run
            val outputData = Data.Builder()
                .putInt(OUTPUT_DELETED_COUNT, deletedCount)
                .putInt("failed_count", failedCount)
                .build()

            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Critical error during trash cleanup", e)
            // Retry on critical errors (database connectivity, etc.)
            Result.retry()
        }
    }
}
