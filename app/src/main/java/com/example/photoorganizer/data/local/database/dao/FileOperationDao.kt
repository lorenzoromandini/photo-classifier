package com.example.photoorganizer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photoorganizer.data.local.database.entities.FileOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FileOperationEntity.
 * Transaction log for crash recovery and safe file operations.
 * On app restart, pending operations can be resumed or rolled back.
 */
@Dao
interface FileOperationDao {

    /**
     * Get all pending operations as a reactive Flow.
     * Pending status includes: PENDING, COPYING, VERIFYING, DELETING
     */
    @Query("SELECT * FROM file_operations WHERE status IN ('PENDING', 'COPYING', 'VERIFYING', 'DELETING') ORDER BY createdAt ASC")
    fun getPending(): Flow<List<FileOperationEntity>>

    /**
     * Get pending operations synchronously.
     * Used on app startup to check for incomplete operations.
     */
    @Query("SELECT * FROM file_operations WHERE status IN ('PENDING', 'COPYING', 'VERIFYING', 'DELETING') ORDER BY createdAt ASC")
    suspend fun getPendingSync(): List<FileOperationEntity>

    /**
     * Get operations by status.
     */
    @Query("SELECT * FROM file_operations WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: String): Flow<List<FileOperationEntity>>

    /**
     * Insert a new file operation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: FileOperationEntity)

    /**
     * Update an operation.
     */
    @Update
    suspend fun update(operation: FileOperationEntity)

    /**
     * Update operation status.
     */
    @Query("UPDATE file_operations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /**
     * Mark operation as completed.
     */
    @Query("UPDATE file_operations SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, completedAt: Long)

    /**
     * Mark operation as failed with error message.
     */
    @Query("UPDATE file_operations SET status = 'FAILED', errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: String, errorMessage: String)

    /**
     * Delete completed operations older than cutoff timestamp.
     * Prevents log from growing indefinitely.
     */
    @Query("DELETE FROM file_operations WHERE createdAt < :cutoff AND status = 'COMPLETED'")
    suspend fun cleanupCompleted(cutoff: Long)

    /**
     * Delete failed operations older than cutoff.
     */
    @Query("DELETE FROM file_operations WHERE createdAt < :cutoff AND status = 'FAILED'")
    suspend fun cleanupFailed(cutoff: Long)

    /**
     * Get operation by ID.
     */
    @Query("SELECT * FROM file_operations WHERE id = :id")
    suspend fun getById(id: String): FileOperationEntity?

    /**
     * Count operations by status.
     */
    @Query("SELECT COUNT(*) FROM file_operations WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
