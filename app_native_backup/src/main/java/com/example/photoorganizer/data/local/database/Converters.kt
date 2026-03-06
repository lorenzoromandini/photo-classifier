package com.example.photoorganizer.data.local.database

import androidx.room.TypeConverter
import com.example.photoorganizer.data.local.database.entities.OperationStatus
import com.example.photoorganizer.data.local.database.entities.OperationType
import java.time.Instant

/**
 * Room type converters for enums and Instant type.
 * Enables storing complex types as primitive database columns.
 */
class Converters {

    // OperationType converters
    @TypeConverter
    fun fromOperationType(value: OperationType): String {
        return value.name
    }

    @TypeConverter
    fun toOperationType(value: String): OperationType {
        return OperationType.valueOf(value)
    }

    // OperationStatus converters
    @TypeConverter
    fun fromOperationStatus(value: OperationStatus): String {
        return value.name
    }

    @TypeConverter
    fun toOperationStatus(value: String): OperationStatus {
        return OperationStatus.valueOf(value)
    }

    // Instant converters
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(millis: Long?): Instant? {
        return millis?.let { Instant.ofEpochMilli(it) }
    }
}
