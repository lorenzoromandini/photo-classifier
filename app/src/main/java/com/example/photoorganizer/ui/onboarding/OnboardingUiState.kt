package com.example.photoorganizer.ui.onboarding

import android.net.Uri

/**
 * UI State for the onboarding screen.
 * Represents the current stage and data displayed to the user.
 *
 * @property stage Current stage of the onboarding flow
 * @property permissionGranted Whether SAF permission has been granted
 * @property folderUri The selected folder URI (null if not selected)
 * @property discoveredFolders Number of folders discovered during scanning
 * @property totalPhotos Total number of photos found in discovered folders
 * @property errorMessage Error message to display (null if no error)
 * @property isLoading Whether an operation is in progress
 * @property folderName Name of the selected folder for display
 */
data class OnboardingUiState(
    val stage: OnboardingStage = OnboardingStage.WELCOME,
    val permissionGranted: Boolean = false,
    val folderUri: Uri? = null,
    val discoveredFolders: Int = 0,
    val totalPhotos: Int = 0,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val folderName: String? = null
)

/**
 * Enum representing the different stages of the onboarding flow.
 * Single-screen flow per user decision - no multi-step wizard.
 */
enum class OnboardingStage {
    /**
     * Initial welcome state showing app value proposition.
     */
    WELCOME,

    /**
     * Waiting for user to grant SAF permission.
     */
    REQUESTING_PERMISSION,

    /**
     * Actively discovering folders in the selected directory.
     */
    DISCOVERING,

    /**
     * Onboarding completed successfully.
     */
    COMPLETE,

    /**
     * Error occurred during onboarding.
     */
    ERROR
}

/**
 * Sealed class representing onboarding events that can be triggered from UI.
 */
sealed class OnboardingEvent {
    data class PermissionGranted(val treeUri: Uri) : OnboardingEvent()
    object PermissionDenied : OnboardingEvent()
    object PermissionCancelled : OnboardingEvent()
    object StartOnboarding : OnboardingEvent()
    object Retry : OnboardingEvent()
    object Complete : OnboardingEvent()
}

/**
 * Extension function to check if the current state allows navigation to main screen.
 */
fun OnboardingUiState.canNavigateToMain(): Boolean {
    return stage == OnboardingStage.COMPLETE
}

/**
 * Extension function to check if there's an active error.
 */
fun OnboardingUiState.hasError(): Boolean {
    return stage == OnboardingStage.ERROR || errorMessage != null
}

/**
 * Extension function to get user-friendly stage description.
 */
fun OnboardingStage.description(): String {
    return when (this) {
        OnboardingStage.WELCOME -> "Welcome to Photo Organizer"
        OnboardingStage.REQUESTING_PERMISSION -> "Grant Permission"
        OnboardingStage.DISCOVERING -> "Discovering Folders"
        OnboardingStage.COMPLETE -> "Setup Complete"
        OnboardingStage.ERROR -> "Something Went Wrong"
    }
}
