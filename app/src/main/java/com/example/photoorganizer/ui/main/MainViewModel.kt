package com.example.photoorganizer.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoorganizer.data.local.datastore.UserPreferencesRepository
import com.example.photoorganizer.data.local.safe.StorageChecker
import com.example.photoorganizer.data.repository.FolderRepository
import com.example.photoorganizer.domain.model.Folder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the main screen.
 *
 * Manages folder data, permission checking, and navigation state.
 * Provides reactive UI state via StateFlow.
 *
 * @property application Application context for permission checking
 * @property folderRepository Repository for folder operations
 * @property userPreferencesRepository Repository for user preferences
 * @property storageChecker For checking storage status
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val folderRepository: FolderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val storageChecker: StorageChecker
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        observeFolders()
        checkStorageStatus()
    }

    /**
     * Observe folders from repository and update UI state.
     */
    private fun observeFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                folderRepository.folders.collect { folders ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            folders = folders,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error observing folders")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load folders: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Check if permissions are still valid for configured folders.
     * Called to detect permission loss and trigger re-onboarding.
     *
     * @return true if all permissions are valid, false otherwise
     */
    suspend fun checkPermissions(): Boolean {
        return try {
            val userData = userPreferencesRepository.userData.value
            val folderUris = userData.folderUris

            if (folderUris.isEmpty()) {
                // No folders configured yet
                return false
            }

            // Check each folder has permission
            val allValid = folderUris.all { uri ->
                folderRepository.hasPermission(Uri.parse(uri))
            }

            if (!allValid) {
                Timber.w("Some folder permissions have been revoked")
                _uiState.update {
                    it.copy(shouldShowPermissionWarning = true)
                }
            }

            allValid
        } catch (e: Exception) {
            Timber.e(e, "Error checking permissions")
            false
        }
    }

    /**
     * Trigger manual refresh of folders.
     * Re-discovers folders and syncs with database.
     */
    fun refreshFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userData = userPreferencesRepository.userData.value
                val folderUris = userData.folderUris

                if (folderUris.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            folders = emptyList()
                        )
                    }
                    return@launch
                }

                // Rediscover from first folder
                val picturesUri = Uri.parse(folderUris.first())
                folderRepository.discoverAndSyncFolders(picturesUri).fold(
                    onSuccess = {
                        Timber.d("Folder refresh completed successfully")
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Folder refresh failed")
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                errorMessage = "Failed to refresh: ${error.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing folders")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Refresh failed: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Check storage status and update warning state.
     */
    private fun checkStorageStatus() {
        viewModelScope.launch {
            try {
                val available = storageChecker.getAvailableStorage()
                val threshold = storageChecker.minStorageBuffer
                val isLow = available < threshold

                if (isLow) {
                    Timber.w("Storage is low: ${available / (1024 * 1024)}MB available")
                }

                _uiState.update {
                    it.copy(
                        shouldShowStorageWarning = isLow,
                        availableStorage = available
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check storage")
            }
        }
    }

    /**
     * Dismiss the permission warning banner.
     */
    fun dismissPermissionWarning() {
        _uiState.update { it.copy(shouldShowPermissionWarning = false) }
    }

    /**
     * Dismiss the storage warning banner.
     */
    fun dismissStorageWarning() {
        _uiState.update { it.copy(shouldShowStorageWarning = false) }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Get count of completed folders (ready for organization).
     */
    fun getCompletedFolderCount(): Int {
        return _uiState.value.folders.count { folder ->
            folder.learningStatus.name == "COMPLETED"
        }
    }

    /**
     * Get total photo count across all folders.
     */
    fun getTotalPhotoCount(): Int {
        return _uiState.value.folders.sumOf { it.photoCount }
    }
}

/**
 * UI state for the main screen.
 *
 * @property folders List of discovered folders
 * @property isLoading Whether folders are being loaded
 * @property errorMessage Error message to display, if any
 * @property shouldShowPermissionWarning Whether to show permission warning
 * @property shouldShowStorageWarning Whether to show storage warning
 * @property availableStorage Available storage in bytes (for warning details)
 */
data class MainUiState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val shouldShowPermissionWarning: Boolean = false,
    val shouldShowStorageWarning: Boolean = false,
    val availableStorage: Long = 0
)
