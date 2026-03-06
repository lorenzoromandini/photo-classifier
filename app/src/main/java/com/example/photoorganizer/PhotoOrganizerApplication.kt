package com.example.photoorganizer

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Photo Organizer.
 *
 * Initializes:
 * 1. Hilt dependency injection
 * 2. Timber logging
 * 3. WorkManager with HiltWorkerFactory for DI support
 */
@HiltAndroidApp
class PhotoOrganizerApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        // In debug builds, use DebugTree. In production, use custom tree.
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

        Timber.tag("Application").d("Photo Organizer initialized")
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
