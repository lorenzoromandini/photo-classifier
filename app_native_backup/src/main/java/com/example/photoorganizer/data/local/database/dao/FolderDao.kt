package com.example.photoorganizer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.photoorganizer.data.local.database.entities.FolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FolderEntity.
 * Manages discovered folders and their learning status.
 */
@Dao
interface FolderDao {

    /**
     * Get all folders as a reactive Flow.
     */
    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FolderEntity>>

    /**
     * Get folders by learning status.
     * Used during folder learning phase.
     */
    @Query("SELECT * FROM folders WHERE learningStatus = :status")
    fun getByLearningStatus(status: String): Flow<List<FolderEntity>>

    /**
     * Get folders with PENDING learning status.
     * Used to find folders that need learning.
     */
    @Query("SELECT * FROM folders WHERE learningStatus = 'PENDING'")
    suspend fun getPendingFolders(): List<FolderEntity>

    /**
     * Get active folders only.
     * Active folders are included in photo organization.
     */
    @Query("SELECT * FROM folders WHERE isActive = 1")
    fun getActive(): Flow<List<FolderEntity>>

    /**
     * Insert or replace multiple folders.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    /**
     * Update a single folder.
     */
    @Update
    suspend fun update(folder: FolderEntity)

    /**
     * Delete all folders.
     * Called before syncing new folder list.
     */
    @Query("DELETE FROM folders")
    suspend fun deleteAll()

    /**
     * Get a folder by its URI.
     */
    @Query("SELECT * FROM folders WHERE uri = :uri")
    suspend fun getByUri(uri: String): FolderEntity?

    /**
     * Update learning status for a folder.
     */
    @Query("UPDATE folders SET learningStatus = :status WHERE uri = :uri")
    suspend fun updateLearningStatus(uri: String, status: String)

    /**
     * Update learned labels for a folder.
     */
    @Query("UPDATE folders SET learnedLabels = :labels WHERE uri = :uri")
    suspend fun updateLearnedLabels(uri: String, labels: String)

    /**
     * Transaction to sync folders: delete all then insert new.
     * Ensures atomic operation.
     */
    @Transaction
    suspend fun syncFolders(folders: List<FolderEntity>) {
        deleteAll()
        insertAll(folders)
    }
}
