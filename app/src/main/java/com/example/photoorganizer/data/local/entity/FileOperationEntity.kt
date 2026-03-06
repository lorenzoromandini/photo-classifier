package com.example.photoorganizer.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Entity representing a file operation transaction log entry.
 * Used for crash recovery - operations can be replayed if app crashes mid-operation.
 */
@Entity(tableName = "file_operations")
data class FileOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "operation_type")
    val operationType: OperationType,

    @ColumnInfo(name = "source_uri")
    val sourceUri: String,

    @ColumnInfo(name = "destination_uri")
    val destinationUri: String?,

    @ColumnInfo(name = "status")
    val status: OperationStatus,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
) {
    /**
     * Type of file operation being performed
     */
    enum class OperationType {
        MOVE,
        COPY,
        DELETE
    }

    /**
     * Current status of the operation
     */
    enum class OperationStatus {
        PENDING,    // Operation created but not started
        COPYING,    // File copy in progress
        VERIFYING,  // Verifying copy succeeded
        DELETING,   // Deleting source file
        COMPLETED,  // Operation fully complete
        FAILED      // Operation failed, not recoverable
    }

    /**
     * Returns true if operation is in a recoverable state
     */
    fun isRecoverable(): Boolean = status != OperationStatus.COMPLETED &&
            status != OperationStatus.FAILED

    /**
     * Returns true if operation can be retried
     */
    fun canRetry(maxRetries: Int = 5): Boolean =
        status == OperationStatus.FAILED && retryCount < maxRetries
}
