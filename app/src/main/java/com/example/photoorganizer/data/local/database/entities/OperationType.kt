package com.example.photoorganizer.data.local.database.entities

/**
 * Enum representing the type of file operation in the transaction log.
 * Used for crash recovery and safe file operations.
 */
enum class OperationType {
    COPY,
    VERIFY,
    DELETE
}
