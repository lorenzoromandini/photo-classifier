package com.example.photoorganizer.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a photo in the trash.
 * Stored in Room database for tracking and cleanup.
 *
 * @property id Unique identifier (UUID)
 * @property originalUri The source URI before moving to trash
 * @property trashUri The URI within the hidden .trash folder
 * @property fileName The original filename
 * @property fileSize Size in bytes
 * @property movedAt Timestamp when moved to trash (milliseconds)
 * @property expiresAt Timestamp when item should be auto-deleted (movedAt + 7 days)
 * @property restored Whether the file has been restored from trash
 */
@Entity(
    tableName = "trash_items",
    indices = [
        Index(value = ["expiresAt"], name = "idx_trash_expires"),
        Index(value = ["restored"], name = "idx_trash_restored")
    ]
)
data class TrashItemEntity(
    @PrimaryKey
    val id: String,
    val originalUri: String,
    val trashUri: String,
    val fileName: String,
    val fileSize: Long,
    val movedAt: Long,
    val expiresAt: Long,
    val restored: Boolean = false
)
