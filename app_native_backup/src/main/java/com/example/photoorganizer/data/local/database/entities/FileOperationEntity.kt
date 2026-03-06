package com.example.photoorganizer.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for transaction logging to support crash recovery.
 * Records all file operations (copy, verify, delete) with their status.
 * On app restart, pending operations can be resumed or rolled back.
 *
 * @property id Unique identifier (UUID)
 * @property sourceUri Source file URI
 * @property destUri Destination file URI (for copy operations)
 * @property operationType Type of operation: COPY, VERIFY, DELETE
 * @property status Current status: PENDING, COPYING, VERIFYING, DELETING, COMPLETED, FAILED
 * @property createdAt Unix timestamp when the operation was created
 * @property completedAt Unix timestamp when the operation completed, null if in progress
 * @property retryCount Number of retry attempts for failed operations
 * @property errorMessage Error message if the operation failed
 */
@Entity(
    tableName = "file_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["sourceUri"])
    ]
)
data class FileOperationEntity(
    @PrimaryKey
    val id: String,
    val sourceUri: String,
    val destUri: String,
    val operationType: OperationType,
    val status: OperationStatus,
    val createdAt: Long,
    val completedAt: Long?,
    val retryCount: Int = 0,
    val errorMessage: String?
)
