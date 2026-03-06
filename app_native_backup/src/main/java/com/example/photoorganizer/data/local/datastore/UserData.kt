package com.example.photoorganizer.data.local.datastore

/**
 * Domain model representing user preferences.
 * This is the public API exposed by UserPreferencesRepository.
 * Converts from protobuf to Kotlin-friendly types.
 *
 * @property confidenceThreshold ML Kit confidence threshold (0.0-1.0), default 0.9
 * @property onboardingCompleted Whether onboarding flow is complete
 * @property folderUris List of selected folder URIs for photo organization
 * @property learningSampleSize Number of photos for folder learning, default 50
 * @property firstLaunch Whether this is the first app launch
 * @property backgroundProcessingEnabled Whether background processing is enabled
 * @property notificationsEnabled Whether status notifications are enabled
 * @property themePreference UI theme preference: "light", "dark", or "system"
 * @property lastSyncTimestamp Unix timestamp of last sync, null if never synced
 */
data class UserData(
    val confidenceThreshold: Float = 0.9f,
    val onboardingCompleted: Boolean = false,
    val folderUris: List<String> = emptyList(),
    val learningSampleSize: Int = 50,
    val firstLaunch: Boolean = true,
    val backgroundProcessingEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val themePreference: String = "system",
    val lastSyncTimestamp: Long? = null
)

/**
 * Theme options for the app.
 */
sealed class ThemePreference {
    companion object {
        const val LIGHT = "light"
        const val DARK = "dark"
        const val SYSTEM = "system"
    }
}
