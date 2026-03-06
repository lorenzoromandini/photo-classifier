package com.example.photoorganizer.data.local.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user preferences backed by Proto DataStore.
 * Provides type-safe access to user settings with reactive updates via Flow.
 *
 * Features:
 * - Reactive Flow for observing preference changes
 * - Type-safe preference updates
 * - Default values defined in UserData
 * - Thread-safe operations
 *
 * @param dataStore Proto DataStore instance injected via DI
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) {

    /**
     * Flow of user data, emitted whenever preferences change.
     * Maps protobuf to domain model automatically.
     */
    val userData: Flow<UserData> = dataStore.data.map { preferences ->
        UserData(
            confidenceThreshold = preferences.confidenceThreshold.takeIf { it > 0 } ?: 0.9f,
            onboardingCompleted = preferences.onboardingCompleted,
            folderUris = preferences.folderUrisList.toList(),
            learningSampleSize = preferences.learningSampleSize.takeIf { it > 0 } ?: 50,
            firstLaunch = preferences.firstLaunch,
            backgroundProcessingEnabled = preferences.backgroundProcessingEnabled,
            notificationsEnabled = preferences.notificationsEnabled,
            themePreference = preferences.themePreference.takeIf { it.isNotEmpty() } ?: "system",
            lastSyncTimestamp = preferences.lastSyncTimestamp.takeIf { it > 0 }
        )
    }

    /**
     * Set the confidence threshold for ML classification.
     * @param threshold Value between 0.0 and 1.0
     */
    suspend fun setConfidenceThreshold(threshold: Float) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setConfidenceThreshold(threshold.coerceIn(0.0f, 1.0f))
                .build()
        }
    }

    /**
     * Mark onboarding as completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setOnboardingCompleted(completed)
                .build()
        }
    }

    /**
     * Add a folder URI to the selected folders list.
     * @param uri Document URI string from SAF
     */
    suspend fun addFolderUri(uri: String) {
        dataStore.updateData { current ->
            if (uri in current.folderUrisList) {
                current
            } else {
                current.toBuilder()
                    .addFolderUris(uri)
                    .build()
            }
        }
    }

    /**
     * Remove a folder URI from the selected folders list.
     * @param uri Document URI string to remove
     */
    suspend fun removeFolderUri(uri: String) {
        dataStore.updateData { current ->
            val builder = current.toBuilder()
            builder.clearFolderUris()
            current.folderUrisList.filter { it != uri }.forEach { builder.addFolderUris(it) }
            builder.build()
        }
    }

    /**
     * Clear all selected folder URIs.
     */
    suspend fun clearFolderUris() {
        dataStore.updateData { current ->
            current.toBuilder()
                .clearFolderUris()
                .build()
        }
    }

    /**
     * Set the learning sample size for folder ML.
     * @param size Number of photos to sample (recommended 30-100)
     */
    suspend fun setLearningSampleSize(size: Int) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setLearningSampleSize(size.coerceIn(10, 200))
                .build()
        }
    }

    /**
     * Mark first launch as complete.
     * Called after showing welcome/onboarding.
     */
    suspend fun setFirstLaunchComplete() {
        dataStore.updateData { current ->
            current.toBuilder()
                .setFirstLaunch(false)
                .build()
        }
    }

    /**
     * Enable/disable background processing.
     * @param enabled Whether to allow background processing
     */
    suspend fun setBackgroundProcessingEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setBackgroundProcessingEnabled(enabled)
                .build()
        }
    }

    /**
     * Enable/disable status notifications.
     * @param enabled Whether to show processing notifications
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setNotificationsEnabled(enabled)
                .build()
        }
    }

    /**
     * Set the UI theme preference.
     * @param preference One of: "light", "dark", "system"
     */
    suspend fun setThemePreference(preference: String) {
        val validPreference = when (preference) {
            ThemePreference.LIGHT, ThemePreference.DARK -> preference
            else -> ThemePreference.SYSTEM
        }
        dataStore.updateData { current ->
            current.toBuilder()
                .setThemePreference(validPreference)
                .build()
        }
    }

    /**
     * Update the last sync timestamp.
     * Called after successful sync.
     */
    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setLastSyncTimestamp(timestamp)
                .build()
        }
    }

    /**
     * Reset all preferences to defaults.
     * Use with caution - clears all user settings.
     */
    suspend fun resetToDefaults() {
        dataStore.updateData { _ ->
            UserPreferences.getDefaultInstance()
        }
    }

    /**
     * Checks if onboarding has been completed.
     * Convenience method for navigation logic.
     */
    suspend fun isOnboardingComplete(): Boolean {
        return dataStore.data.first().onboardingCompleted
    }
}
