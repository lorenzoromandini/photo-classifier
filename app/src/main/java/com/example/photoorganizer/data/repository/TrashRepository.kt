package com.example.photoorganizer.data.repository

import com.example.photoorganizer.data.local.database.dao.TrashItemDao
import com.example.photoorganizer.data.local.database.entities.TrashItemEntity
import com.example.photoorganizer.data.model.TrashItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for trash item persistence.
 *
 * Acts as the single source of truth for trash operations,
 * bridging between the domain model (TrashItem) and database entity (TrashItemEntity).
 *
 * @see TrashItem
 * @see TrashItemEntity
 * @see TrashManager
 */
@Singleton
class TrashRepository @Inject constructor(
    private val trashItemDao: TrashItemDao
) {

    /**
     * Add a new item to trash.
     *
     * @param item The TrashItem to persist
     */
    suspend fun addToTrash(item: TrashItem) {
        trashItemDao.insert(item.toEntity())
    }

    /**
     * Get all active (non-restored) trash items as a Flow.
     *
     * @return Flow of trash items, sorted by most recently moved first
     */
    fun getTrashItems(): Flow<List<TrashItem>> {
        return trashItemDao.getActiveItems().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Get all active items as a one-time query.
     *
     * @return List of active trash items
     */
    suspend fun getTrashItemsOnce(): List<TrashItem> {
        return trashItemDao.getActiveItemsOnce().map { it.toModel() }
    }

    /**
     * Get items that have expired (past 7-day retention period).
     *
     * @param now Current timestamp in milliseconds
     * @return List of expired trash items
     */
    suspend fun getExpiredItems(now: Long): List<TrashItem> {
        return trashItemDao.getExpiredItems(now).map { it.toModel() }
    }

    /**
     * Mark a trash item as restored.
     *
     * @param id The trash item ID
     */
    suspend fun markRestored(id: String) {
        trashItemDao.markRestored(id)
    }

    /**
     * Remove a trash item from the database.
     *
     * @param id The trash item ID
     */
    suspend fun removeFromTrash(id: String) {
        trashItemDao.deleteById(id)
    }

    /**
     * Get the total size of all active trash items.
     *
     * @return Total size in bytes
     */
    suspend fun getTrashSize(): Long {
        return trashItemDao.getTotalSize()
    }

    /**
     * Get the count of active trash items.
     *
     * @return Number of items in trash
     */
    suspend fun getTrashCount(): Int {
        return trashItemDao.getActiveCount()
    }

    /**
     * Get a specific trash item by ID.
     *
     * @param id The trash item ID
     * @return The TrashItem, or null if not found
     */
    suspend fun getById(id: String): TrashItem? {
        return trashItemDao.getById(id)?.toModel()
    }

    /**
     * Delete all restored items (cleanup old records).
     */
    suspend fun deleteAllRestored() {
        trashItemDao.deleteAllRestored()
    }

    /**
     * Convert TrashItem domain model to TrashItemEntity.
     */
    private fun TrashItem.toEntity(): TrashItemEntity {
        return TrashItemEntity(
            id = id,
            originalUri = originalUri,
            trashUri = trashUri,
            fileName = fileName,
            fileSize = fileSize,
            movedAt = movedAt,
            expiresAt = expiresAt,
            restored = restored
        )
    }

    /**
     * Convert TrashItemEntity to TrashItem domain model.
     */
    private fun TrashItemEntity.toModel(): TrashItem {
        return TrashItem(
            id = id,
            originalUri = originalUri,
            trashUri = trashUri,
            fileName = fileName,
            fileSize = fileSize,
            movedAt = movedAt,
            expiresAt = expiresAt,
            restored = restored
        )
    }
}
