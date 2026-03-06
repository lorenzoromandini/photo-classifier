package com.example.photoorganizer.data.worker

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for background workers.
 *
 * Centralizes scheduling logic for all periodic background tasks:
 * - Trash cleanup (daily)
 * - Future: model training, photo scanning, etc.
 *
 * Should be called after onboarding is complete to ensure permissions
 * and app setup are finalized before background work begins.
 *
 * @see TrashCleanupWorker
 */
@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WorkerScheduler"
    }

    private val workManager: WorkManager = WorkManager.getInstance(context)

    /**
     * Schedule all periodic background workers.
     *
     * Call this after onboarding is complete to start background processing.
     * Safe to call multiple times - existing work is kept, not duplicated.
     */
    fun scheduleAllWorkers() {
        Log.d(TAG, "Scheduling background workers")
        scheduleTrashCleanup()
    }

    /**
     * Schedule daily trash cleanup.
     *
     * Uses unique work name to prevent duplicate scheduling.
     * Existing work is kept if already scheduled.
     */
    private fun scheduleTrashCleanup() {
        val workRequest = TrashCleanupWorker.createWorkRequest()

        workManager.enqueueUniquePeriodicWork(
            TrashCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Don't duplicate if already scheduled
            workRequest
        )

        Log.d(TAG, "Trash cleanup scheduled (runs every ${TrashCleanupWorker.REPEAT_INTERVAL_HOURS} hours)")
    }

    /**
     * Cancel all scheduled workers.
     *
     * Use when user logs out or app is being reset.
     */
    fun cancelAllWorkers() {
        Log.d(TAG, "Canceling all background workers")
        workManager.cancelUniqueWork(TrashCleanupWorker.WORK_NAME)
    }

    /**
     * Cancel only trash cleanup worker.
     */
    fun cancelTrashCleanup() {
        Log.d(TAG, "Canceling trash cleanup worker")
        workManager.cancelUniqueWork(TrashCleanupWorker.WORK_NAME)
    }

    /**
     * Check if trash cleanup is scheduled.
     *
     * @return true if work is scheduled
     */
    fun isTrashCleanupScheduled(): Boolean {
        val workInfo = workManager.getWorkInfosForUniqueWork(TrashCleanupWorker.WORK_NAME)
        val workList = workInfo.get()
        return workList?.isNotEmpty() == true
    }
}
