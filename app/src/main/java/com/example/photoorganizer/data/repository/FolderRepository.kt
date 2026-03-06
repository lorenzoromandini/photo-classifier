package com.example.photoorganizer.data.repository

import android.net.Uri
import com.example.photoorganizer.data.local.database.dao.FolderDao
import com.example.photoorganizer.data.local.database.entities.FolderEntity
import com.example.photoorganizer.data.local.database.entities.LearningStatus
import com.example.photoorganizer.data.local.saf.SafDataSource
import com.example.photoorganizer.domain.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for folder operations following offline-first pattern.
 *
 * The repository acts as a single source of truth for folder data:
 * - Local Room database is the source of truth
 * - SAF discovery provides the data source
 * - Changes are synced from SAF → Local DB → UI
 *
 * @property folderDao DAO for folder database operations
 * @property safDataSource Data source for SAF folder discovery
 */
@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val safDataSource: SafDataSource
) {

    /**
     * All folders as a reactive Flow.
     * Emits whenever folders change in the database.
     */
    val folders: Flow<List<Folder>> = folderDao.getAll()
        .map { entities -> entities.map { it.toDomainModel() } }

    /**
     * Active folders only (included in organization).
     */
    val activeFolders: Flow<List<Folder>> = folderDao.getActive()
        .map { entities -> entities.map { it.toDomainModel() } }

    /**
     * Discovers folders via SAF and syncs them to local database.
     * Implements offline-first pattern: SAF is data source, local DB is truth.
     *
     * @param picturesUri The Pictures/ directory URI to discover folders within
     * @return Result.success(Unit) on success, Result.failure(exception) on error
     */
    suspend fun discoverAndSyncFolders(picturesUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Discover folders from SAF
                val discoveredFolders = safDataSource.discoverFolders(picturesUri)

                // 2. Map to entities, preserving existing learning data
                val existingFolders = folderDao.getAll()
                    .map { list -> list.associateBy { it.uri } }
                    .firstOrNull() ?: emptyMap()

                val folderEntities = discoveredFolders.map { folderInfo ->
                    val existing = existingFolders[folderInfo.uri]
                    FolderEntity(
                        uri = folderInfo.uri,
                        name = folderInfo.name,
                        displayName = folderInfo.displayName,
                        photoCount = folderInfo.photoCount,
                        isActive = existing?.isActive ?: true,
                        learnedLabels = existing?.learnedLabels,
                        learningStatus = existing?.learningStatus ?: LearningStatus.PENDING.name,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                }

                // 3. Atomic sync: delete all, insert new
                folderDao.syncFolders(folderEntities)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Persists SAF URI permission for accessing the folder across reboots.
     *
     * @param uri The folder URI to persist permission for
     * @return Result.success(Unit) on success, Result.failure(exception) on error
     */
    suspend fun persistPermission(uri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                safDataSource.persistFolderPermission(uri)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Checks if persistable permission exists for the given URI.
     *
     * @param uri The URI to check
     * @return true if permission is persisted, false otherwise
     */
    suspend fun hasPermission(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            safDataSource.hasPersistablePermission(uri)
        }

    /**
     * Updates the learning status for a folder.
     *
     * @param uri The folder URI
     * @param status The new learning status
     */
    suspend fun updateLearningStatus(uri: String, status: LearningStatus) {
        withContext(Dispatchers.IO) {
            folderDao.updateLearningStatus(uri, status.name)
        }
    }

    /**
     * Updates the learned labels for a folder.
     * Called after ML learning phase completes.
     *
     * @param uri The folder URI
     * @param labels Map of label → confidence score
     */
    suspend fun updateLearnedLabels(uri: String, labels: Map<String, Float>) {
        withContext(Dispatchers.IO) {
            val jsonLabels = labels.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            // Note: In production, use proper JSON serialization (e.g., Gson or kotlinx.serialization)
            val jsonString = "{$jsonLabels}"
            folderDao.updateLearnedLabels(uri, jsonString)
        }
    }

    /**
     * Gets a folder by its URI.
     *
     * @param uri The folder URI
     * @return The folder if found, null otherwise
     */
    suspend fun getFolderByUri(uri: String): Folder? =
        withContext(Dispatchers.IO) {
            folderDao.getByUri(uri)?.toDomainModel()
        }

    /**
     * Toggles the active status of a folder.
     * Active folders are included in photo organization.
     *
     * @param uri The folder URI
     * @param isActive The new active status
     */
    suspend fun setFolderActive(uri: String, isActive: Boolean) {
        withContext(Dispatchers.IO) {
            folderDao.getByUri(uri)?.let { entity ->
                folderDao.update(entity.copy(isActive = isActive))
            }
        }
    }

    /**
     * Converts a FolderEntity to domain model.
     */
    private fun FolderEntity.toDomainModel(): Folder {
        return Folder(
            uri = uri,
            name = name,
            displayName = displayName,
            photoCount = photoCount,
            isActive = isActive,
            learningStatus = LearningStatus.valueOf(learningStatus),
            learnedLabels = parseLearnedLabels(learnedLabels),
            createdAt = createdAt
        )
    }

    /**
     * Parses JSON string of learned labels into a Map.
     * Note: In production, use proper JSON deserialization.
     */
    private fun parseLearnedLabels(json: String?): Map<String, Float> {
        if (json.isNullOrBlank()) return emptyMap()

        // Simple parsing: "{label1: 0.9, label2: 0.8}"
        return try {
            json.removeSurrounding("{", "}")
                .split(", ")
                .mapNotNull { pair ->
                    val parts = pair.split(": ")
                    if (parts.size == 2) {
                        parts[0] to parts[1].toFloatOrNull()
                    } else null
                }
                .filter { it.second != null }
                .associate { it.first to it.second!! }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}