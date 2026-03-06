package com.example.photoorganizer.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity tracking processed photo metadata and classification results.
 * Stores ML Kit detection results and organization status for each photo.
 *
 * @property uri Source photo URI (primary key) - uniquely identifies the photo
 * @property folderUri Document URI of the folder containing this photo
 * @property fileName Original filename
 * @property fileSize File size in bytes
 * @property processedAt Unix timestamp when ML processing occurred, null if pending
 * @property detectedLabels JSON array of ML labels with confidence scores
 * @property status Current processing status: PENDING, PROCESSED, FAILED, or SKIPPED_LOW_CONFIDENCE
 * @property targetCategoryId ID of the category this photo was assigned to, null if unassigned
 */
@Entity(
    tableName = "photo_metadata",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["folderUri"]),
        Index(value = ["status"]),
        Index(value = ["targetCategoryId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["uri"],
            childColumns = ["folderUri"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PhotoMetadataEntity(
    @PrimaryKey
    val uri: String,
    val folderUri: String,
    val fileName: String,
    val fileSize: Long,
    val processedAt: Long?,
    val detectedLabels: String?, // JSON array of ML labels with confidence
    val status: String = PhotoStatus.PENDING.name,
    val targetCategoryId: String?
)

/**
 * Enum representing the processing status of a photo.
 */
enum class PhotoStatus {
    PENDING,
    PROCESSED,
    FAILED,
    SKIPPED_LOW_CONFIDENCE
}
