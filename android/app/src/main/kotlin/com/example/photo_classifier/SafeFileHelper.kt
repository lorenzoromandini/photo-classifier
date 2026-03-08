package com.example.photo_classifier

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class SafeFileHelper(private val context: Context) {
    companion object {
        private const val TAG = "SafeFileHelper"
        private const val MIN_STORAGE_BUFFER = 100 * 1024 * 1024L // 100MB
    }
    
    /**
     * Copy file from source URI to destination URI via SAF
     * Returns the number of bytes copied
     */
    fun copyFile(sourceUri: Uri, destUri: Uri): Result<Long> {
        return try {
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return Result.failure(IllegalArgumentException("Source file not found: $sourceUri"))
            
            val destFile = DocumentFile.fromSingleUri(context, destUri)
                ?: return Result.failure(IllegalArgumentException("Destination file not found: $destUri"))
            
            if (!sourceFile.canRead()) {
                return Result.failure(SecurityException("Cannot read source file: $sourceUri"))
            }
            
            if (!destFile.canWrite()) {
                return Result.failure(SecurityException("Cannot write to destination: $destUri"))
            }
            
            val sourceSize = sourceFile.length()
            
            // Check available storage before copying
            val availableStorage = getAvailableStorageInternal(destUri)
            if (availableStorage < sourceSize + MIN_STORAGE_BUFFER) {
                return Result.failure(
                    IllegalStateException("Storage full: need ${sourceSize + MIN_STORAGE_BUFFER}, have $availableStorage")
                )
            }
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var count: Int
                    
                    while (inputStream.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                        bytesRead += count
                    }
                    
                    outputStream.flush()
                    Log.d(TAG, "Copied $bytesRead bytes from $sourceUri to $destUri")
                    Result.success(bytesRead)
                }
            } ?: Result.failure(IllegalStateException("Failed to open streams"))
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify file copy by comparing sizes (and optionally checksums)
     * Returns true if files match
     */
    fun verifyFile(sourceUri: Uri, destUri: Uri): Result<Boolean> {
        return try {
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return Result.failure(IllegalArgumentException("Source file not found: $sourceUri"))
            
            val destFile = DocumentFile.fromSingleUri(context, destUri)
                ?: return Result.failure(IllegalArgumentException("Destination file not found: $destUri"))
            
            val sourceSize = sourceFile.length()
            val destSize = destFile.length()
            
            // Size verification (primary check)
            if (sourceSize != destSize) {
                Log.e(TAG, "Size mismatch: source=$sourceSize, dest=$destSize")
                return Result.success(false)
            }
            
            // Optional: checksum verification for extra safety
            val sourceChecksum = calculateChecksum(sourceUri)
            val destChecksum = calculateChecksum(destUri)
            
            if (sourceChecksum != destChecksum) {
                Log.e(TAG, "Checksum mismatch")
                return Result.success(false)
            }
            
            Log.d(TAG, "Verification passed: $sourceUri -> $destUri")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete file via SAF
     */
    fun deleteFile(uri: Uri): Result<Unit> {
        return try {
            val file = DocumentFile.fromSingleUri(context, uri)
                ?: return Result.failure(IllegalArgumentException("File not found: $uri"))
            
            if (!file.canWrite()) {
                return Result.failure(SecurityException("Cannot delete file: $uri"))
            }
            
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted file: $uri")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to delete file: $uri"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get file size in bytes
     */
    fun getFileSize(uri: Uri): Result<Long> {
        return try {
            val file = DocumentFile.fromSingleUri(context, uri)
                ?: return Result.failure(IllegalArgumentException("File not found: $uri"))
            
            val size = file.length()
            Log.d(TAG, "File size for $uri: $size bytes")
            Result.success(size)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get available storage in bytes using StatFs
     */
    fun getAvailableStorage(): Result<Long> {
        return try {
            val result = getAvailableStorageInternal(null)
            Log.d(TAG, "Available storage: $result bytes")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available storage", e)
            Result.failure(e)
        }
    }
    
    /**
     * Internal method to get available storage
     */
    private fun getAvailableStorageInternal(uri: Uri?): Long {
        return try {
            val path = uri?.let {
                context.contentResolver.openFileDescriptor(it, "r")?.let { fd ->
                    fd.close()
                    return@let null // Will use default path
                }
            }
            
            // Use default external storage path
            StatFs(context.filesDir.absolutePath).let { statFs ->
                statFs.availableBytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating available storage", e)
            0L
        }
    }
    
    /**
     * Check if file exists
     */
    fun exists(uri: Uri): Result<Boolean> {
        return try {
            val file = DocumentFile.fromSingleUri(context, uri)
            val exists = file != null && file.exists()
            Log.d(TAG, "File exists: $uri -> $exists")
            Result.success(exists)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file existence", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate MD5 checksum of a file
     */
    private fun calculateChecksum(uri: Uri): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8192)
                var count: Int
                while (inputStream.read(buffer).also { count = it } != -1) {
                    md.update(buffer, 0, count)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum", e)
            ""
        }
    }
}
