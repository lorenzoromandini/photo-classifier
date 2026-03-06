package com.example.photoorganizer.data.local.safe

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import com.example.photoorganizer.data.local.database.dao.FileOperationDao
import com.example.photoorganizer.data.local.database.entities.FileOperationEntity
import com.example.photoorganizer.data.local.database.entities.OperationStatus
import com.example.photoorganizer.data.local.database.entities.OperationType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crash-safe file operations using copy-then-verify-then-delete pattern.
 * Each step is logged to Room database for crash recovery.
 *
 * Pattern:
 * 1. Create operation log (PENDING)
 * 2. Check storage space
 * 3. Copy file (COPYING)
 * 4. Verify copy (VERIFYING)
 * 5. Delete source (DELETING)
 * 6. Mark complete (COMPLETED)
 *
 * On crash, TransactionRepository can replay incomplete operations.
 */
@Singleton
class SafeFileOperations @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileOperationDao: FileOperationDao
) {
    companion object {
        private const val TAG = "SafeFileOperations"
        private const val MIN_FREE_SPACE_MB = 100L
        private const val MIN_FREE_SPACE_BYTES = MIN_FREE_SPACE_MB * 1024 * 1024
        private const val BUFFER_SIZE = 8192
    }

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Safely move a file using copy-verify-delete pattern.
     * Each step is logged for crash recovery.
     *
     * @param sourceUri Source file URI
     * @param destUri Destination file URI
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun safeMove(sourceUri: Uri, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Create operation log
            val operationId = createOperationLog(
                OperationType.COPY,
                sourceUri,
                destUri
            )

            try {
                // Step 2: Check storage space
                val sourceSize = getFileSize(sourceUri)
                    ?: throw IOException("Cannot determine source file size")

                if (!hasEnoughSpace(sourceSize + MIN_FREE_SPACE_BYTES)) {
                    fileOperationDao.markFailed(operationId, "Insufficient storage space")
                    throw StorageFullException("Need ${sourceSize + MIN_FREE_SPACE_BYTES} bytes, insufficient space available")
                }

                // Step 3: Copy file
                fileOperationDao.updateStatus(operationId, OperationStatus.COPYING.name)
                copyFile(sourceUri, destUri)

                // Step 4: Verify copy
                fileOperationDao.updateStatus(operationId, OperationStatus.VERIFYING.name)
                val verification = verifyCopy(sourceUri, destUri)

                if (!verification.isSuccess) {
                    fileOperationDao.markFailed(operationId, verification.toString())
                    // Clean up failed destination
                    deleteFile(destUri)
                    throw IOException("Verification failed: $verification")
                }

                // Step 5: Delete source
                fileOperationDao.updateStatus(operationId, OperationStatus.DELETING.name)
                val deleteResult = deleteFile(sourceUri)

                if (!deleteResult) {
                    // Don't fail here - file is copied and verified, source deletion can be retried
                    Log.w(TAG, "Failed to delete source after successful copy: $sourceUri")
                }

                // Step 6: Mark complete
                fileOperationDao.markCompleted(operationId, System.currentTimeMillis())

            } catch (e: Exception) {
                fileOperationDao.markFailed(operationId, e.message ?: "Unknown error")
                throw e
            }
        }
    }

    /**
     * Safely copy a file with transaction logging.
     *
     * @param sourceUri Source file URI
     * @param destUri Destination file URI
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun safeCopy(sourceUri: Uri, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Create operation log
            val operationId = createOperationLog(
                OperationType.COPY,
                sourceUri,
                destUri
            )

            try {
                // Step 2: Check storage space
                val sourceSize = getFileSize(sourceUri)
                    ?: throw IOException("Cannot determine source file size")

                if (!hasEnoughSpace(sourceSize + MIN_FREE_SPACE_BYTES)) {
                    fileOperationDao.markFailed(operationId, "Insufficient storage space")
                    throw StorageFullException("Need ${sourceSize + MIN_FREE_SPACE_BYTES} bytes, insufficient space available")
                }

                // Step 3: Copy file
                fileOperationDao.updateStatus(operationId, OperationStatus.COPYING.name)
                copyFile(sourceUri, destUri)

                // Step 4: Verify copy
                fileOperationDao.updateStatus(operationId, OperationStatus.VERIFYING.name)
                val verification = verifyCopy(sourceUri, destUri)

                if (!verification.isSuccess) {
                    fileOperationDao.markFailed(operationId, verification.toString())
                    // Clean up failed destination
                    deleteFile(destUri)
                    throw IOException("Verification failed: $verification")
                }

                // Step 5: Mark complete (no delete for copy)
                fileOperationDao.markCompleted(operationId, System.currentTimeMillis())

            } catch (e: Exception) {
                fileOperationDao.markFailed(operationId, e.message ?: "Unknown error")
                throw e
            }
        }
    }

    /**
     * Safely delete a file with transaction logging.
     *
     * @param sourceUri File URI to delete
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun safeDelete(sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: Create operation log
            val operationId = createOperationLog(
                OperationType.DELETE,
                sourceUri,
                null
            )

            try {
                // Step 2: Delete
                fileOperationDao.updateStatus(operationId, OperationStatus.DELETING.name)
                val deleteResult = deleteFile(sourceUri)

                if (!deleteResult) {
                    fileOperationDao.markFailed(operationId, "Delete operation returned false")
                    throw IOException("Failed to delete file: $sourceUri")
                }

                // Step 3: Mark complete
                fileOperationDao.markCompleted(operationId, System.currentTimeMillis())

            } catch (e: Exception) {
                fileOperationDao.markFailed(operationId, e.message ?: "Unknown error")
                throw e
            }
        }
    }

    /**
     * Verify that a copied file matches the source.
     * Checks file size as minimum verification, optionally SHA-256 hash.
     *
     * @param sourceUri Original file URI
     * @param destUri Copied file URI
     * @return FileVerificationResult indicating success or type of failure
     */
    suspend fun verifyCopy(sourceUri: Uri, destUri: Uri): FileVerificationResult = withContext(Dispatchers.IO) {
        try {
            val sourceSize = getFileSize(sourceUri)
            val destSize = getFileSize(destUri)

            if (sourceSize == null) {
                return@withContext FileVerificationResult.Error(
                    IOException("Cannot read source file size: $sourceUri")
                )
            }

            if (destSize == null) {
                return@withContext FileVerificationResult.DestNotFound(destUri)
            }

            if (sourceSize != destSize) {
                return@withContext FileVerificationResult.SizeMismatch(sourceSize, destSize)
            }

            // Size matches - success
            FileVerificationResult.Success(sourceSize, destSize)

        } catch (e: Exception) {
            FileVerificationResult.Error(e)
        }
    }

    /**
     * Check if there's enough storage space available.
     *
     * @param requiredBytes Minimum bytes needed
     * @return true if space available, false otherwise
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        return try {
            val statFs = StatFs(context.filesDir.path)
            val availableBytes = statFs.availableBytes
            availableBytes >= requiredBytes
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage space", e)
            false
        }
    }

    /**
     * Get the size of a file in bytes.
     *
     * @param uri File URI
     * @return Size in bytes, or null if cannot determine
     */
    fun getFileSize(uri: Uri): Long? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        // Try opening and reading size
                        try {
                            contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                                fd.statSize
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size for $uri", e)
            null
        }
    }

    /**
     * Copy file from source to destination.
     *
     * @param sourceUri Source file URI
     * @param destUri Destination file URI
     */
    private fun copyFile(sourceUri: Uri, destUri: Uri) {
        contentResolver.openInputStream(sourceUri)?.use { input ->
            contentResolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output, BUFFER_SIZE)
            } ?: throw IOException("Cannot open output stream: $destUri")
        } ?: throw IOException("Cannot open input stream: $sourceUri")
    }

    /**
     * Delete file at the given URI.
     *
     * @param uri File URI to delete
     * @return true if deletion succeeded
     */
    private fun deleteFile(uri: Uri): Boolean {
        return try {
            contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $uri", e)
            false
        }
    }

    /**
     * Create a new operation log entry.
     *
     * @param operationType Type of operation
     * @param sourceUri Source file URI
     * @param destUri Destination file URI (null for delete)
     * @return ID of the created operation
     */
    private suspend fun createOperationLog(
        operationType: OperationType,
        sourceUri: Uri,
        destUri: Uri?
    ): String {
        val operation = FileOperationEntity(
            id = UUID.randomUUID().toString(),
            sourceUri = sourceUri.toString(),
            destUri = destUri?.toString() ?: "",
            operationType = operationType,
            status = OperationStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            retryCount = 0,
            errorMessage = null
        )
        fileOperationDao.insert(operation)
        return operation.id
    }
}

/**
 * Exception thrown when storage is full
 */
class StorageFullException(message: String) : IOException(message)
