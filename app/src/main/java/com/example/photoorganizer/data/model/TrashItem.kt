package com.example.photoorganizer.data.model

import java.util.UUID

/**
 * Domain model representing a photo in the trash.
 * Photos are retained for 7 days before auto-deletion.
 *
 * @property id Unique identifier
 * @property originalUri The source URI before moving to trash
 * @property trashUri The URI within the hidden .trash folder
 * @property fileName The original filename
 * @property fileSize Size in bytes
 * @property movedAt Timestamp when moved to trash (milliseconds)
 * @property expiresAt Timestamp when item should be auto-deleted (movedAt + 7 days)
 * @property restored Whether the file has been restored from trash
 */
data class TrashItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: String,
    val trashUri: String,
    val fileName: String,
    val fileSize: Long,
    val movedAt: Long,
    val expiresAt: Long = movedAt + RETENTION_PERIOD_MS,
    val restored: Boolean = false
) {
    companion object {
        /** 7 days in milliseconds */
        const val RETENTION_PERIOD_MS = 7L * 24 * 60 * 60 * 1000
    }

    /**
     * Check if this trash item has expired (past retention period).
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return now > expiresAt
    }

    /**
     * Calculate days remaining before auto-deletion.
     */
    fun daysRemaining(now: Long = System.currentTimeMillis()): Int {
        if (isExpired(now)) return 0
        return ((expiresAt - now) / (24 * 60 * 60 * 1000)).toInt()
    }
}
