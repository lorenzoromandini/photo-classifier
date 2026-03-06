package com.example.photoorganizer.data.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-defined photo category.
 * Categories map ML Kit labels to target folders for automatic photo organization.
 * Maximum 10 categories per user constraint enforced at application level.
 *
 * @property id Unique identifier (UUID)
 * @property name Display name for the category
 * @property folderUri Document URI of the target folder where matching photos are copied
 * @property mlLabels JSON array of associated ML Kit labels that trigger this category
 * @property confidenceThreshold Minimum confidence score (0.0-1.0) to trigger this category, default 0.9
 * @property createdAt Unix timestamp when the category was created
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val folderUri: String,
    val mlLabels: String, // JSON array
    val confidenceThreshold: Float = 0.9f,
    val createdAt: Long
)
