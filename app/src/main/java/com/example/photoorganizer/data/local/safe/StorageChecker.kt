package com.example.photoorganizer.data.local.safe

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for checking device storage state.
 * Provides information about available space and storage health.
 *
 * Thresholds:
 * - OK: >= 500MB free
 * - LOW: 100MB - 500MB free
 * - CRITICAL: < 100MB free
 */
@Singleton
class StorageChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * Threshold for LOW storage state (500 MB)
         */
        const val LOW_SPACE_THRESHOLD_MB = 500L

        /**
         * Threshold for CRITICAL storage state (100 MB)
         */
        const val CRITICAL_SPACE_THRESHOLD_MB = 100L

        /**
         * Minimum free space buffer for operations (100 MB)
         */
        const val MIN_FREE_SPACE_MB = 100L

        /**
         * Bytes per MB
         */
        const val BYTES_PER_MB = 1024L * 1024L
    }

    /**
     * Represents the current storage state
     */
    enum class StorageState {
        /**
         * Healthy storage - plenty of space available
         */
        OK,

        /**
         * Storage is getting low - operations may start failing
         */
        LOW,

        /**
         * Critical storage - operations likely to fail
         */
        CRITICAL
    }

    /**
     * Get the available free space in bytes.
     *
     * @return Available bytes, or 0 if cannot determine
     */
    fun getAvailableBytes(): Long {
        return try {
            val statFs = StatFs(context.filesDir.path)
            statFs.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get the total storage space in bytes.
     *
     * @return Total bytes, or 0 if cannot determine
     */
    fun getTotalBytes(): Long {
        return try {
            val statFs = StatFs(context.filesDir.path)
            statFs.totalBytes
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get the used storage space in bytes.
     *
     * @return Used bytes
     */
    fun getUsedBytes(): Long {
        return getTotalBytes() - getAvailableBytes()
    }

    /**
     * Check if there's enough space for an operation.
     *
     * @param requiredBytes Bytes needed for the operation
     * @param bufferMb Additional buffer in MB (default: MIN_FREE_SPACE_MB)
     * @return true if space is available
     */
    fun hasSpaceFor(requiredBytes: Long, bufferMb: Int = MIN_FREE_SPACE_MB.toInt()): Boolean {
        val bufferBytes = bufferMb * BYTES_PER_MB
        return getAvailableBytes() >= (requiredBytes + bufferBytes)
    }

    /**
     * Get the current storage state based on available space.
     *
     * @return StorageState indicating storage health
     */
    fun getStorageState(): StorageState {
        val availableMb = getAvailableBytes() / BYTES_PER_MB

        return when {
            availableMb < CRITICAL_SPACE_THRESHOLD_MB -> StorageState.CRITICAL
            availableMb < LOW_SPACE_THRESHOLD_MB -> StorageState.LOW
            else -> StorageState.OK
        }
    }

    /**
     * Check if storage is in critical state.
     *
     * @return true if storage is critical
     */
    fun isCritical(): Boolean = getStorageState() == StorageState.CRITICAL

    /**
     * Check if storage is low or critical.
     *
     * @return true if storage is low or critical
     */
    fun isLowOrCritical(): Boolean = getStorageState() != StorageState.OK

    /**
     * Get a human-readable string of available space.
     *
     * @return Formatted string (e.g., "1.5 GB", "500 MB")
     */
    fun getAvailableSpaceString(): String {
        return formatBytes(getAvailableBytes())
    }

    /**
     * Get a human-readable string of total space.
     *
     * @return Formatted string (e.g., "64 GB")
     */
    fun getTotalSpaceString(): String {
        return formatBytes(getTotalBytes())
    }

    /**
     * Get a human-readable string of used space.
     *
     * @return Formatted string
     */
    fun getUsedSpaceString(): String {
        return formatBytes(getUsedBytes())
    }

    /**
     * Get storage usage percentage.
     *
     * @return Percentage used (0-100)
     */
    fun getUsagePercentage(): Int {
        val total = getTotalBytes()
        return if (total > 0) {
            ((getUsedBytes().toDouble() / total.toDouble()) * 100).toInt()
        } else {
            0
        }
    }

    /**
     * Check available space for a specific path.
     * Useful for checking external storage.
     *
     * @param path Path to check
     * @return Available bytes, or 0 if cannot determine
     */
    fun getAvailableBytesForPath(path: File): Long {
        return try {
            val statFs = StatFs(path.path)
            statFs.availableBytes
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Format bytes to human-readable string.
     *
     * @param bytes Bytes to format
     * @return Formatted string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
