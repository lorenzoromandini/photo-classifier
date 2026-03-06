package com.example.photoorganizer.domain.model

import com.example.photoorganizer.data.local.database.entities.LearningStatus

/**
 * Domain model representing a photo folder.
 * This is the public API for folders in the domain layer.
 *
 * @property uri The SAF URI of the folder (primary identifier)
 * @property name Technical folder name from filesystem
 * @property displayName User-friendly display name
 * @property photoCount Number of photos in the folder
 * @property isActive Whether this folder is included in organization
 * @property learningStatus Current learning phase status
 * @property learnedLabels Map of aggregated labels from ML learning (label → confidence)
 * @property createdAt Unix timestamp when first discovered
 */
data class Folder(
    val uri: String,
    val name: String,
    val displayName: String,
    val photoCount: Int = 0,
    val isActive: Boolean = true,
    val learningStatus: LearningStatus = LearningStatus.PENDING,
    val learnedLabels: Map<String, Float> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)