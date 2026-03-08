package com.example.photo_classifier

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper class for trash operations via Storage Access Framework
 * 
 * Creates and manages a hidden .trash folder in the Pictures directory.
 * Files moved to trash are retained for 7 days before permanent deletion.
 */
class TrashHelper(private val context: Context) {
    companion object {
        private const val TAG = "TrashHelper"
        private const val TRASH_FOLDER_NAME = ".trash"
        private const val NOMEDIA_FILENAME = ".nomedia"
    }
    
    /**
     * Create the .trash folder in the given base URI (e.g., Pictures folder)
     * Returns the URI of the trash folder
     */
    fun createTrashFolder(baseUri: Uri): Result<Uri> {
        return try {
            val baseFolder = DocumentFile.fromTreeUri(context, baseUri)
                ?: return Result.failure(IllegalArgumentException("Base folder not found: $baseUri"))
            
            // Check if .trash already exists
            val existingTrash = baseFolder.findFile(TRASH_FOLDER_NAME)
            if (existingTrash != null && existingTrash.isDirectory) {
                Log.d(TAG, "Trash folder already exists: ${existingTrash.uri}")
                return Result.success(existingTrash.uri)
            }
            
            // Create .trash folder (hidden by dot prefix)
            val trashFolder = baseFolder.createDirectory(TRASH_FOLDER_NAME)
                ?: return Result.failure(IllegalStateException("Failed to create trash folder"))
            
            // Create .nomedia file to prevent gallery apps from showing trash contents
            val nomediaFile = trashFolder.createFile("application/octet-stream", NOMEDIA_FILENAME)
            if (nomediaFile == null) {
                Log.w(TAG, "Failed to create .nomedia file, trash will be visible in galleries")
            } else {
                Log.d(TAG, "Created .nomedia file in trash folder")
            }
            
            Log.d(TAG, "Created trash folder: ${trashFolder.uri}")
            Result.success(trashFolder.uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating trash folder", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the .trash folder URI if it exists
     * Returns null if trash folder hasn't been created yet
     */
    fun getTrashFolderUri(baseUri: Uri): Result<Uri?> {
        return try {
            val baseFolder = DocumentFile.fromTreeUri(context, baseUri)
                ?: return Result.failure(IllegalArgumentException("Base folder not found: $baseUri"))
            
            val trashFolder = baseFolder.findFile(TRASH_FOLDER_NAME)
            if (trashFolder != null && trashFolder.isDirectory) {
                Log.d(TAG, "Found existing trash folder: ${trashFolder.uri}")
                Result.success(trashFolder.uri)
            } else {
                Log.d(TAG, "Trash folder does not exist yet")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting trash folder", e)
            Result.failure(e)
        }
    }
    
    /**
     * Move a file to the trash folder
     * Implements copy-verify-delete pattern for safety
     * Returns the new trash URI
     */
    fun moveToTrash(sourceUri: Uri, trashFolderUri: Uri, fileName: String): Result<String> {
        return try {
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
                ?: return Result.failure(IllegalArgumentException("Source file not found: $sourceUri"))
            
            val trashFolder = DocumentFile.fromTreeUri(context, trashFolderUri)
                ?: return Result.failure(IllegalArgumentException("Trash folder not found: $trashFolderUri"))
            
            // Generate unique filename with timestamp to avoid conflicts
            val timestamp = System.currentTimeMillis()
            val uniqueFileName = "${timestamp}_$fileName"
            
            // Check if file with same name exists in trash
            val fileSize = sourceFile.length()
            
            // Copy file to trash
            val copyResult = copyToTrash(sourceUri, trashFolderUri, uniqueFileName)
            if (copyResult.isFailure) {
                return Result.failure(copyResult.exceptionOrNull() ?: Exception("Copy to trash failed"))
            }
            
            val trashUri = copyResult.getOrNull()!!
            
            // Verify the copy
            val verifyResult = verifyTrashCopy(sourceUri, trashUri, fileSize)
            if (verifyResult.isFailure || !(verifyResult.getOrNull() ?: false)) {
                return Result.failure(Exception("Verification failed after moving to trash"))
            }
            
            // Delete original file
            val deleted = sourceFile.delete()
            if (!deleted) {
                Log.w(TAG, "Failed to delete original file after moving to trash")
                // Don't fail here - file is safely in trash
            }
            
            Log.d(TAG, "Moved file to trash: $trashUri")
            Result.success(trashUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error moving file to trash", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restore a file from trash to its original location
     * Implements copy-verify-delete pattern for safety
     */
    fun restoreFromTrash(trashUri: Uri, destUri: Uri): Result<Unit> {
        return try {
            val trashFile = DocumentFile.fromSingleUri(context, trashUri)
                ?: return Result.failure(IllegalArgumentException("Trash file not found: $trashUri"))
            
            val destFile = DocumentFile.fromSingleUri(context, destUri)
            if (destFile != null) {
                return Result.failure(IllegalStateException("Destination file already exists: $destUri"))
            }
            
            val fileSize = trashFile.length()
            
            // Get parent directory of destination
            val destParentUri = getParentUri(destUri)
            val destFolder = DocumentFile.fromTreeUri(context, destParentUri ?: trashUri)
                ?: return Result.failure(IllegalStateException("Destination folder not accessible"))
            
            // Extract original filename (remove timestamp prefix)
            val fileName = trashFile.name ?: return Result.failure(Exception("Cannot get filename"))
            val originalFileName = removeTimestampPrefix(fileName)
            
            // Copy back to original location
            context.contentResolver.openInputStream(trashUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (inputStream.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                    }
                    outputStream.flush()
                }
            }
            
            // Verify
            val verifyResult = verifyRestore(destUri, trashUri, fileSize)
            if (verifyResult.isFailure || !(verifyResult.getOrNull() ?: false)) {
                return Result.failure(Exception("Verification failed after restore"))
            }
            
            // Delete from trash
            val deleted = trashFile.delete()
            if (!deleted) {
                Log.w(TAG, "Failed to delete file from trash after restore")
            }
            
            Log.d(TAG, "Restored file from trash: $destUri")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring file from trash", e)
            Result.failure(e)
        }
    }
    
    /**
     * Permanently delete a file from trash
     */
    fun permanentlyDelete(trashUri: Uri): Result<Unit> {
        return try {
            val trashFile = DocumentFile.fromSingleUri(context, trashUri)
                ?: return Result.failure(IllegalArgumentException("Trash file not found: $trashUri"))
            
            val deleted = trashFile.delete()
            if (deleted) {
                Log.d(TAG, "Permanently deleted from trash: $trashUri")
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to delete from trash: $trashUri"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error permanently deleting from trash", e)
            Result.failure(e)
        }
    }
    
    /**
     * Copy file to trash folder
     */
    private fun copyToTrash(sourceUri: Uri, trashFolderUri: Uri, fileName: String): Result<String> {
        return try {
            val trashFolder = DocumentFile.fromTreeUri(context, trashFolderUri)
                ?: return Result.failure(IllegalStateException("Trash folder not found"))
            
            // Determine MIME type
            val sourceFile = DocumentFile.fromSingleUri(context, sourceUri)
            val mimeType = sourceFile?.type ?: "application/octet-stream"
            
            // Create file in trash
            val trashFile = trashFolder.createFile(mimeType, fileName)
                ?: return Result.failure(IllegalStateException("Failed to create file in trash"))
            
            // Copy content
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(trashFile.uri)?.use { outputStream ->
                    val buffer = ByteArray(8192)
                    var count: Int
                    while (inputStream.read(buffer).also { count = it } != -1) {
                        outputStream.write(buffer, 0, count)
                    }
                    outputStream.flush()
                }
            }
            
            Result.success(trashFile.uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to trash", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify file was copied to trash successfully
     */
    private fun verifyTrashCopy(sourceUri: Uri, trashUri: Uri, expectedSize: Long): Result<Boolean> {
        return try {
            val trashFile = DocumentFile.fromSingleUri(context, trashUri)
                ?: return Result.success(false)
            
            val trashSize = trashFile.length()
            
            // Size verification
            val matches = trashSize == expectedSize
            if (!matches) {
                Log.e(TAG, "Trash verification failed: source=$expectedSize, trash=$trashSize")
            }
            
            Result.success(matches)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying trash copy", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify file was restored successfully
     */
    private fun verifyRestore(destUri: Uri, trashUri: Uri, expectedSize: Long): Result<Boolean> {
        return try {
            val destFile = DocumentFile.fromSingleUri(context, destUri)
                ?: return Result.success(false)
            
            val destSize = destFile.length()
            
            // Size verification
            val matches = destSize == expectedSize
            if (!matches) {
                Log.e(TAG, "Restore verification failed: trash=$expectedSize, dest=$destSize")
            }
            
            Result.success(matches)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying restore", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get parent URI from a file URI
     */
    private fun getParentUri(fileUri: Uri): Uri? {
        // Simple implementation - in production, parse the URI properly
        val path = fileUri.path ?: return null
        val parentPath = path.substringBeforeLast("/")
        return Uri.parse(fileUri.toString().replace(path, parentPath))
    }
    
    /**
     * Remove timestamp prefix from filename
     * e.g., "1234567890_photo.jpg" -> "photo.jpg"
     */
    private fun removeTimestampPrefix(fileName: String): String {
        val underscoreIndex = fileName.indexOf('_')
        return if (underscoreIndex > 0) {
            fileName.substring(underscoreIndex + 1)
        } else {
            fileName
        }
    }
}
