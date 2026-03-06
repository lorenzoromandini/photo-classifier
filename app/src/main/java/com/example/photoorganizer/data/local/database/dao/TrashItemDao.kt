package com.example.photoorganizer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.photoorganizer.data.local.database.entities.TrashItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for trash item operations.
 *
 * Provides queries for:
 * - Getting active (non-restored) items sorted by move date
 * - Finding expired items for cleanup
 * - Basic CRUD operations
 */
@Dao
interface TrashItemDao {

    /**
     * Get all active (non-restored) trash items, sorted by most recently moved first.
     */
    @Query("SELECT * FROM trash_items WHERE restored = 0 ORDER BY movedAt DESC")
    fun getActiveItems(): Flow<List<TrashItemEntity>>

    /**
     * Get all active items as a one-time query (not Flow).
     */
    @Query("SELECT * FROM trash_items WHERE restored = 0 ORDER BY movedAt DESC")
    suspend fun getActiveItemsOnce(): List<TrashItemEntity>

    /**
     * Get items that have expired (past their retention period) and haven't been restored.
     *
     * @param now Current timestamp in milliseconds
     */
    @Query("SELECT * FROM trash_items WHERE expiresAt < :now AND restored = 0")
    suspend fun getExpiredItems(now: Long): List<TrashItemEntity>

    /**
     * Get total size of all active trash items.
     */
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM trash_items WHERE restored = 0")
    suspend fun getTotalSize(): Long

    /**
     * Get count of active trash items.
     */
    @Query("SELECT COUNT(*) FROM trash_items WHERE restored = 0")
    suspend fun getActiveCount(): Int

    /**
     * Insert a new trash item.
     */
    @Insert
    suspend fun insert(item: TrashItemEntity)

    /**
     * Update an existing trash item.
     */
    @Update
    suspend fun update(item: TrashItemEntity)

    /**
     * Delete a trash item from the database.
     */
    @Delete
    suspend fun delete(item: TrashItemEntity)

    /**
     * Delete trash item by ID.
     */
    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Mark a trash item as restored.
     */
    @Query("UPDATE trash_items SET restored = 1 WHERE id = :id")
    suspend fun markRestored(id: String)

    /**
     * Get a specific trash item by ID.
     */
    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TrashItemEntity?

    /**
     * Delete all restored items (cleanup old records).
     */
    @Query("DELETE FROM trash_items WHERE restored = 1")
    suspend fun deleteAllRestored()

    /**
     * Delete all items (use with caution - for testing only).
     */
    @Query("DELETE FROM trash_items")
    suspend fun deleteAll()
}
