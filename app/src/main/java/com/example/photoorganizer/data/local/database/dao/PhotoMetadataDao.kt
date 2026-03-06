package com.example.photoorganizer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photoorganizer.data.local.database.entities.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PhotoMetadataEntity.
 * Tracks photo processing status and classification results.
 */
@Dao
interface PhotoMetadataDao {

    /**
     * Get all photo metadata as a reactive Flow.
     */
    @Query("SELECT * FROM photo_metadata ORDER BY processedAt DESC")
    fun getAll(): Flow<List<PhotoMetadataEntity>>

    /**
     * Get photos by processing status.
     * Useful for showing pending or failed photos.
     */
    @Query("SELECT * FROM photo_metadata WHERE status = :status")
    fun getByStatus(status: String): Flow<List<PhotoMetadataEntity>>

    /**
     * Get photos by target category.
     */
    @Query("SELECT * FROM photo_metadata WHERE targetCategoryId = :categoryId")
    fun getByCategory(categoryId: String): Flow<List<PhotoMetadataEntity>>

    /**
     * Get photos in a specific folder.
     */
    @Query("SELECT * FROM photo_metadata WHERE folderUri = :folderUri")
    fun getByFolder(folderUri: String): Flow<List<PhotoMetadataEntity>>

    /**
     * Get a single photo by URI (suspending).
     */
    @Query("SELECT * FROM photo_metadata WHERE uri = :uri")
    suspend fun getByUri(uri: String): PhotoMetadataEntity?

    /**
     * Insert or replace photo metadata.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoMetadataEntity)

    /**
     * Update photo metadata.
     */
    @Update
    suspend fun update(photo: PhotoMetadataEntity)

    /**
     * Check if a photo exists.
     */
    @Query("SELECT COUNT(*) FROM photo_metadata WHERE uri = :uri")
    suspend fun exists(uri: String): Int

    /**
     * Update photo status.
     */
    @Query("UPDATE photo_metadata SET status = :status WHERE uri = :uri")
    suspend fun updateStatus(uri: String, status: String)

    /**
     * Update target category for a photo.
     */
    @Query("UPDATE photo_metadata SET targetCategoryId = :categoryId WHERE uri = :uri")
    suspend fun updateCategory(uri: String, categoryId: String)

    /**
     * Delete photos by folder URI.
     * Called when a folder is removed.
     */
    @Query("DELETE FROM photo_metadata WHERE folderUri = :folderUri")
    suspend fun deleteByFolder(folderUri: String)

    /**
     * Count photos by status.
     */
    @Query("SELECT COUNT(*) FROM photo_metadata WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    /**
     * Get unprocessed photos.
     */
    @Query("SELECT * FROM photo_metadata WHERE status = 'PENDING' ORDER BY uri")
    suspend fun getPendingPhotos(): List<PhotoMetadataEntity>
}
