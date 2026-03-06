package com.example.photoorganizer.data.local.database.entities

/**
 * Enum representing the status of a file operation in the transaction log.
 * Tracks the state machine of copy-verify-delete operations for crash recovery.
 */
enum class OperationStatus {
    PENDING,
    COPYING,
    VERIFYING,
    DELETING,
    COMPLETED,
    FAILED
}
