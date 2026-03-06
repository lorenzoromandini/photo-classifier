package com.example.photoorganizer

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.photoorganizer.data.local.datastore.UserPreferencesRepository
import com.example.photoorganizer.data.local.safe.StorageChecker
import com.example.photoorganizer.data.repository.TransactionRepository
import com.example.photoorganizer.data.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Photo Organizer.
 *
 * Initializes:
 * 1. Hilt dependency injection
 * 2. Timber logging
 * 3. WorkManager with HiltWorkerFactory for DI support
 * 4. Crash recovery on startup (transaction log replay)
 * 5. Background worker scheduling
 * 6. Storage full detection
 */
@HiltAndroidApp
class PhotoOrganizerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var workerScheduler: WorkerScheduler

    @Inject
    lateinit var storageChecker: StorageChecker

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        initializeTimber()

        Timber.tag("Application").d("Photo Organizer initialized")

        // Check storage status early
        checkStorageStatus()

        // Run startup recovery if onboarding is complete
        applicationScope.launch {
            performStartupRecovery()
        }
    }

    /**
     * Initialize Timber for logging.
     * Debug builds use DebugTree, production uses custom tree.
     */
    private fun initializeTimber() {
        val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            // Production logging (e.g., to Crashlytics)
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Log to crash reporting service in production
                    if (priority >= Log.WARN) {
                        // TODO: Integrate with crash reporting (e.g., Firebase Crashlytics)
                    }
                }
            })
        }
    }

    /**
     * Check storage status and log warnings if space is low.
     * Storage full warnings are displayed to the user when relevant.
     */
    private fun checkStorageStatus() {
        applicationScope.launch {
            try {
                val available = storageChecker.getAvailableStorage()
                val threshold = storageChecker.minStorageBuffer

                if (available < threshold) {
                    Timber.w("Storage low: ${available / (1024 * 1024)}MB available, ${threshold / (1024 * 1024)}MB recommended")
                } else {
                    Timber.d("Storage status: ${available / (1024 * 1024)}MB available")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check storage status")
            }
        }
    }

    /**
     * Perform startup recovery by replaying pending transaction operations.
     * Called on every app startup to ensure data integrity after crashes.
     *
     * Recovery flow:
     * 1. Check if onboarding is complete
     * 2. Recover pending operations from transaction log
     * 3. Schedule background workers
     * 4. Log results
     */
    private suspend fun performStartupRecovery() {
        try {
            // Only run recovery if onboarding is complete
            val isOnboardingComplete = userPreferencesRepository.isOnboardingComplete()
            if (!isOnboardingComplete) {
                Timber.d("Onboarding not complete, skipping recovery")
                return
            }

            Timber.i("Starting crash recovery...")

            // Recover pending operations
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

            // Schedule background workers
            scheduleBackgroundWorkers()

        } catch (e: Exception) {
            Timber.e(e, "Error during startup recovery")
            // Don't crash the app - failed operations remain in PENDING for next attempt
        }
    }

    /**
     * Schedule all background workers after recovery.
     * Safe to call multiple times - existing work is kept.
     */
    private fun scheduleBackgroundWorkers() {
        try {
            workerScheduler.scheduleAllWorkers()
            Timber.d("Background workers scheduled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule background workers")
        }
    }

    /**
     * Check if storage is nearly full.
     * Used by UI to show storage warnings.
     *
     * @return true if storage is below warning threshold
     */
    fun isStorageLow(): Boolean {
        return try {
            val available = storageChecker.getAvailableStorage()
            available < storageChecker.minStorageBuffer
        } catch (e: Exception) {
            Timber.e(e, "Failed to check storage")
            false
        }
    }

    /**
     * Provides WorkManager configuration with HiltWorkerFactory.
     * This enables constructor injection in workers.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
