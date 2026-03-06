package com.example.photoorganizer.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a discovered folder in the Pictures/ directory.
 * Folders are scanned on startup and can be learned from to improve classification.
 *
 * @property uri Document URI (primary key) - uniquely identifies the folder
 * @property name Folder name from the filesystem
 * @property displayName User-friendly display name
 * @property photoCount Number of photos in the folder (cached)
 * @property isActive Whether this folder is included in organization
 * @property learnedLabels JSON of aggregated labels from sample photos during learning phase
 * @property learningStatus Current learning phase status: PENDING, IN_PROGRESS, or COMPLETED
 * @property createdAt Unix timestamp when first discovered
 */
@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["learningStatus"])
    ]
)
data class FolderEntity(
    @PrimaryKey
    val uri: String,
    val name: String,
    val displayName: String,
    val photoCount: Int = 0,
    val isActive: Boolean = true,
    val learnedLabels: String?, // JSON of aggregated labels from 50 sample photos
    val learningStatus: String = LearningStatus.PENDING.name,
    val createdAt: Long
)

/**
 * Enum representing the learning status of a folder.
 * Used to track the ML learning process for folder-based classification.
 */
enum class LearningStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
