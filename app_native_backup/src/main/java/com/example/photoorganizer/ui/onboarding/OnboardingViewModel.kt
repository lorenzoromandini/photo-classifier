package com.example.photoorganizer.ui.onboarding

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoorganizer.data.local.datastore.UserPreferencesRepository
import com.example.photoorganizer.data.local.saf.SafException
import com.example.photoorganizer.data.repository.FolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel managing the onboarding flow.
 *
 * Handles permission requests, folder discovery, and navigation state.
 * Uses StateFlow for reactive UI updates.
 *
 * @property application Application context for SAF operations
 * @property folderRepository Repository for folder discovery and persistence
 * @property userPreferencesRepository Repository for user preferences
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    application: Application,
    private val folderRepository: FolderRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Initializes the onboarding flow.
     * Called when the onboarding screen is first displayed.
     */
    fun startOnboarding() {
        if (_uiState.value.stage == OnboardingStage.WELCOME) {
            // Already in welcome state, nothing to do
            Timber.d("Starting onboarding - already in WELCOME state")
        }
    }

    /**
     * Called when the user taps to grant permission.
     * Triggers the SAF document tree picker.
     */
    fun onRequestPermissionClick() {
        _uiState.update { currentState ->
            currentState.copy(
                stage = OnboardingStage.REQUESTING_PERMISSION,
                isLoading = true
            )
        }
        Timber.d("User initiated permission request")
    }

    /**
     * Called when SAF permission is granted by the user.
     *
     * @param treeUri The URI of the selected directory tree
     */
    fun onPermissionGranted(treeUri: Uri) {
        Timber.d("Permission granted for URI: $treeUri")

        _uiState.update { currentState ->
            currentState.copy(
                permissionGranted = true,
                folderUri = treeUri,
                folderName = treeUri.lastPathSegment,
                isLoading = true,
                errorMessage = null
            )
        }

        // Start folder discovery
        discoverFolders(treeUri)
    }

    /**
     * Called when the user denies or cancels the permission request.
     */
    fun onPermissionDenied() {
        Timber.w("Permission denied or cancelled by user")

        _uiState.update { currentState ->
            currentState.copy(
                stage = OnboardingStage.ERROR,
                isLoading = false,
                errorMessage = "Permission is required to organize your photos. Please try again."
            )
        }
    }

    /**
     * Called when permission request is cancelled (e.g., back button).
     */
    fun onPermissionCancelled() {
        Timber.d("Permission request cancelled")

        _uiState.update { currentState ->
            currentState.copy(
                stage = OnboardingStage.WELCOME,
                isLoading = false
            )
        }
    }

    /**
     * Discovers folders within the selected directory.
     *
     * @param treeUri The URI to discover folders within
     */
    private fun discoverFolders(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(stage = OnboardingStage.DISCOVERING) }

            try {
                // First, persist the permission so it survives reboots
                folderRepository.persistPermission(treeUri).fold(
                    onSuccess = {
                        Timber.d("Permission persisted successfully")
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to persist permission")
                        throw SafException.PermissionPersistenceException(
                            "Could not save permission: ${error.message}"
                        )
                    }
                )

                // Discover and sync folders
                folderRepository.discoverAndSyncFolders(treeUri).fold(
                    onSuccess = {
                        Timber.d("Folder discovery completed successfully")

                        // Get discovered folder count
                        val folders = folderRepository.folders.value
                        val folderCount = folders?.size ?: 0
                        val photoCount = folders?.sumOf { it.photoCount.toLong() }?.toInt() ?: 0

                        _uiState.update { currentState ->
                            currentState.copy(
                                discoveredFolders = folderCount,
                                totalPhotos = photoCount,
                                isLoading = false
                            )
                        }

                        // Complete onboarding
                        completeOnboarding()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Folder discovery failed")
                        throw SafException.DiscoveryException(
                            "Failed to discover folders: ${error.message}"
                        )
                    }
                )
            } catch (e: SafException) {
                handleDiscoveryError(e)
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during folder discovery")
                handleDiscoveryError(
                    SafException.DiscoveryException("An unexpected error occurred: ${e.message}")
                )
            }
        }
    }

    /**
     * Handles discovery errors and updates UI state.
     */
    private fun handleDiscoveryError(error: SafException) {
        _uiState.update { currentState ->
            currentState.copy(
                stage = OnboardingStage.ERROR,
                isLoading = false,
                errorMessage = error.userMessage
            )
        }
        Timber.e("Discovery error: ${error.userMessage}")
    }

    /**
     * Completes the onboarding flow and persists completion state.
     */
    private fun completeOnboarding() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setOnboardingCompleted(true)
                Timber.d("Onboarding completion persisted")

                _uiState.update { currentState ->
                    currentState.copy(
                        stage = OnboardingStage.COMPLETE,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist onboarding completion")
                // Still mark as complete in UI even if persistence failed
                // The user can complete onboarding again if needed
                _uiState.update { currentState ->
                    currentState.copy(
                        stage = OnboardingStage.COMPLETE,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Called when user retries after an error.
     */
    fun onRetry() {
        Timber.d("User requested retry")

        _uiState.update { currentState ->
            currentState.copy(
                stage = OnboardingStage.WELCOME,
                errorMessage = null,
                discoveredFolders = 0,
                totalPhotos = 0,
                isLoading = false
            )
        }
    }

    /**
     * Called to skip onboarding (for testing or advanced users).
     * Not exposed in normal UI flow.
     */
    fun skipOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(stage = OnboardingStage.COMPLETE) }
        }
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Checks if onboarding is already complete.
     * Can be used by navigation to skip onboarding screen.
     */
    suspend fun isOnboardingComplete(): Boolean {
        return userPreferencesRepository.isOnboardingComplete()
    }
}