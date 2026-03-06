package com.example.photoorganizer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.photoorganizer.data.local.entity.FileOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for file operation transaction log.
 * Provides CRUD operations and queries for recovery purposes.
 */
@Dao
interface FileOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: FileOperationEntity): Long

    @Update
    suspend fun update(operation: FileOperationEntity)

    @Query("SELECT * FROM file_operations WHERE id = :id")
    suspend fun getById(id: Long): FileOperationEntity?

    @Query("SELECT * FROM file_operations WHERE status != 'COMPLETED' AND status != 'FAILED' ORDER BY created_at ASC")
    suspend fun getPendingOperations(): List<FileOperationEntity>

    @Query("SELECT * FROM file_operations WHERE status = 'FAILED' ORDER BY created_at DESC")
    fun getFailedOperations(): Flow<List<FileOperationEntity>>

    @Query("SELECT * FROM file_operations WHERE status = :status ORDER BY created_at ASC")
    suspend fun getByStatus(status: FileOperationEntity.OperationStatus): List<FileOperationEntity>

    @Query("DELETE FROM file_operations WHERE status = 'COMPLETED' AND created_at < :olderThanTimestamp")
    suspend fun deleteOldCompleted(olderThanTimestamp: Long): Int

    @Query("DELETE FROM file_operations WHERE status = 'COMPLETED'")
    suspend fun deleteAllCompleted(): Int

    @Query("SELECT COUNT(*) FROM file_operations WHERE status != 'COMPLETED' AND status != 'FAILED'")
    suspend fun getPendingCount(): Int

    @Query("UPDATE file_operations SET retry_count = retry_count + 1, updated_at = :timestamp WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE file_operations SET status = :status, error_message = :errorMessage, updated_at = :timestamp WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: FileOperationEntity.OperationStatus,
        errorMessage: String? = null,
        timestamp: Long = System.currentTimeMillis()
    )
}
