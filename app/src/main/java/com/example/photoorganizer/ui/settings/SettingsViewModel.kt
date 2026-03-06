package com.example.photoorganizer.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoorganizer.data.local.datastore.UserPreferencesRepository
import com.example.photoorganizer.data.repository.FolderRepository
import com.example.photoorganizer.data.repository.TrashRepository
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
 * ViewModel for the settings screen.
 *
 * Manages settings like confidence threshold, folder management,
 * and provides access to trash items for potential restoration.
 *
 * @property application Application context
 * @property userPreferencesRepository Repository for user preferences
 * @property folderRepository Repository for folder operations
 * @property trashRepository Repository for trash operations
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val folderRepository: FolderRepository,
    private val trashRepository: TrashRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeUserData()
        observeFolders()
    }

    /**
     * Observe user preferences and update UI state.
     */
    private fun observeUserData() {
        viewModelScope.launch {
            userPreferencesRepository.userData.collect { userData ->
                _uiState.update { currentState ->
                    currentState.copy(
                        confidenceThreshold = userData.confidenceThreshold,
                        notificationsEnabled = userData.notificationsEnabled,
                        backgroundProcessingEnabled = userData.backgroundProcessingEnabled
                    )
                }
            }
        }
    }

    /**
     * Observe folders from repository.
     */
    private fun observeFolders() {
        viewModelScope.launch {
            folderRepository.folders.collect { folders ->
                _uiState.update { currentState ->
                    currentState.copy(
                        folders = folders
                    )
                }
            }
        }
    }

    /**
     * Set the confidence threshold for ML classification.
     * Persists immediately to DataStore.
     *
     * @param value Threshold value between 0.0 and 1.0
     */
    fun setConfidenceThreshold(value: Float) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setConfidenceThreshold(value)
                _uiState.update { it.copy(showSavedIndicator = true) }

                // Hide saved indicator after delay
                launch {
                    kotlinx.coroutines.delay(1500)
                    _uiState.update { it.copy(showSavedIndicator = false) }
                }

                Timber.d("Confidence threshold set to $value")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set confidence threshold")
            }
        }
    }

    /**
     * Toggle folder active status.
     * Active folders are included in photo organization.
     *
     * @param uri Folder URI
     * @param isActive New active status
     */
    fun setFolderActive(uri: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                folderRepository.setFolderActive(uri, isActive)
                Timber.d("Folder $uri active status set to $isActive")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set folder active status")
            }
        }
    }

    /**
     * Toggle notifications enabled state.
     *
     * @param enabled Whether notifications should be enabled
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setNotificationsEnabled(enabled)
                Timber.d("Notifications enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set notifications enabled")
            }
        }
    }

    /**
     * Toggle background processing enabled state.
     *
     * @param enabled Whether background processing should be enabled
     */
    fun setBackgroundProcessingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setBackgroundProcessingEnabled(enabled)
                Timber.d("Background processing enabled: $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set background processing enabled")
            }
        }
    }

    /**
     * Clear all app data and reset to defaults.
     * Use with caution - this is primarily for debugging/support.
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                // Reset preferences
                userPreferencesRepository.resetToDefaults()

                // Note: Database clearing would require access to DAOs
                // This is a simplified version - full implementation would
                // clear all database tables via appropriate repository calls

                Timber.w("All user data cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear data")
            }
        }
    }

    /**
     * Trigger folder refresh/sync.
     */
    fun refreshFolders() {
        viewModelScope.launch {
            // This would typically trigger a re-discovery
            // For now, just log
            Timber.d("Folder refresh requested from settings")
        }
    }

    /**
     * Get the app version for the About section.
     *
     * @return App version string
     */
    fun getAppVersion(): String {
        return try {
            val packageInfo = getApplication()<Application>()
                .packageManager
                .getPackageInfo(getApplication().packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get count of active folders.
     */
    fun getActiveFolderCount(): Int {
        return _uiState.value.folders.count { it.isActive }
    }

    /**
     * Get count of completed folders (ready for organization).
     */
    fun getCompletedFolderCount(): Int {
        return _uiState.value.folders.count {
            it.learningStatus.name == "COMPLETED"
        }
    }
}

/**
 * UI state for the settings screen.
 *
 * @property confidenceThreshold Current confidence threshold (0.0 - 1.0)
 * @property folders List of configured folders
 * @property notificationsEnabled Whether notifications are enabled
 * @property backgroundProcessingEnabled Whether background processing is enabled
 * @property showSavedIndicator Whether to show "Saved" indicator temporarily
 * @property isLoading Whether settings are being loaded
 * @property errorMessage Error message to display, if any
 */
data class SettingsUiState(
    val confidenceThreshold: Float = 0.9f, // Default: High confidence
    val folders: List<Folder> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val backgroundProcessingEnabled: Boolean = true,
    val showSavedIndicator: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
