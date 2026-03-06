package com.example.photoorganizer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.photoorganizer.data.repository.LearningRepository
import com.example.photoorganizer.data.repository.FolderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background folder learning.
 *
 * This worker runs the ML learning process for pending folders in the background,
 * respecting battery and storage constraints. It:
 *
 * 1. Gets all pending folders
 * 2. Processes each folder through learning (samples 50 photos, analyzes with ML Kit)
 * 3. Reports progress via WorkManager Data
 * 4. Retries on failure (up to 3 times)
 *
 * Constraints:
 * - Requires battery not low
 * - Requires storage not low
 * - No network required (uses bundled ML model)
 *
 * @param context Application context
 * @param params Worker parameters
 * @param learningRepository Repository for learning operations
 * @param folderRepository Repository for folder operations
 */
@HiltWorker
class FolderLearningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val learningRepository: LearningRepository,
    private val folderRepository: FolderRepository
) : CoroutineWorker(context, params) {

    /**
     * Executes the learning work.
     *
     * @return Result.success() if all folders processed, Result.retry() if should retry
     */
    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("Starting folder learning work")

        try {
            // Get all pending folders
            val pendingFolders = learningRepository.getPendingFolders()

            if (pendingFolders.isEmpty()) {
                Timber.tag(TAG).d("No pending folders to learn")
                return Result.success()
            }

            val totalFolders = pendingFolders.size
            Timber.tag(TAG).d("Found $totalFolders pending folders")

            // Process each pending folder
            var processedCount = 0
            var failureCount = 0

            pendingFolders.forEachIndexed { index, folder ->
                val folderName = folder.name
                val folderUri = folder.uri

                // Report progress
                val progress = workDataOf(
                    KEY_FOLDER_NAME to folderName,
                    KEY_CURRENT to index + 1,
                    KEY_TOTAL to totalFolders,
                    KEY_PROGRESS to (index + 1) * 100 / totalFolders
                )
                setProgress(progress)

                Timber.tag(TAG).d("Learning folder ${index + 1}/$totalFolders: $folderName")

                // Run learning for this folder
                learningRepository.startLearning(folderUri)
                    .onSuccess { result ->
                        Timber.tag(TAG).d(
                            "Successfully learned $folderName: " +
                            "${result.sampleCount} samples, " +
                            "top labels: ${result.topLabels.take(3)}"
                        )
                        processedCount++
                    }
                    .onFailure { error ->
                        Timber.tag(TAG).e(error, "Failed to learn folder $folderName")
                        failureCount++
                    }
            }

            // Final progress update
            val finalProgress = workDataOf(
                KEY_FOLDER_NAME to "Complete",
                KEY_CURRENT to totalFolders,
                KEY_TOTAL to totalFolders,
                KEY_PROGRESS to 100,
                KEY_PROCESSED to processedCount,
                KEY_FAILED to failureCount
            )
            setProgress(finalProgress)

            Timber.tag(TAG).d(
                "Learning complete: $processedCount succeeded, $failureCount failed"
            )

            // Return success if at least some folders were processed
            // Failures are logged but don't fail the work entirely
            return if (processedCount > 0 || failureCount == 0) {
                Result.success()
            } else {
                // All folders failed, retry
                Result.retry()
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Critical error during folder learning")
            return if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "FolderLearningWorker"
        const val WORK_NAME = "folder_learning_work"

        // Progress data keys
        const val KEY_FOLDER_NAME = "folder_name"
        const val KEY_CURRENT = "current"
        const val KEY_TOTAL = "total"
        const val KEY_PROGRESS = "progress"
        const val KEY_PROCESSED = "processed"
        const val KEY_FAILED = "failed"

        // Retry configuration
        const val MAX_RETRIES = 3

        /**
         * Enqueues folder learning work.
         *
         * Uses unique work to prevent duplicate learning jobs.
         * Replaces any existing pending work.
         *
         * @param context Application context
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<FolderLearningWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            Timber.tag(TAG).d("Enqueued folder learning work")
        }

        /**
         * Checks if learning work is currently running or enqueued.
         *
         * @param context Application context
         * @return true if work is pending or running
         */
        fun isRunning(context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            val workInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
            return workInfo.any { 
                it.state == androidx.work.WorkInfo.State.RUNNING ||
                it.state == androidx.work.WorkInfo.State.ENQUEUED
            }
        }

        /**
         * Cancels any pending or running learning work.
         *
         * @param context Application context
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.tag(TAG).d("Cancelled folder learning work")
        }

        private const val BACKOFF_DELAY_MS = 10_000L // 10 seconds
    }
}
