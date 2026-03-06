package com.example.photoorganizer.data.local.safe

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.photoorganizer.data.model.TrashItem
import com.example.photoorganizer.data.repository.TrashRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the trash folder for deleted photos.
 *
 * Instead of permanently deleting files, they are moved to a hidden .trash folder
 * in the Pictures root directory. Files are retained for 7 days before automatic
 * deletion by TrashCleanupWorker.
 *
 * This provides a safety net for users - if the app misplaces a photo during
 * organization, they have 7 days to recover it.
 *
 * @see TrashItem
 * @see TrashCleanupWorker
 * @see TrashRepository
 */
@Singleton
class TrashManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashRepository: TrashRepository
) {
    companion object {
        private const val TAG = "TrashManager"
        private const val TRASH_FOLDER_NAME = ".trash"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Move a file to the trash folder.
     *
     * The file is copied to .trash/, verified, then deleted from source.
     * A TrashItem is created and persisted to track the operation.
     *
     * @param sourceUri The URI of the file to move to trash
     * @param fileName The filename (used for creating the trash entry)
     * @return Result.success with TrashItem on success, Result.failure on error
     */
    suspend fun moveToTrash(sourceUri: Uri, fileName: String): Result<TrashItem> = withContext(Dispatchers.IO) {
        runCatching {
            // Get or create trash folder in Pictures root
            val trashFolder = getOrCreateTrashFolder()
                ?: throw IOException("Failed to create or access trash folder")

            // Get file size
            val fileSize = getFileSize(sourceUri)
                ?: throw IOException("Cannot determine file size: $sourceUri")

            // Create unique filename in trash (prepend timestamp to avoid conflicts)
            val timestamp = System.currentTimeMillis()
            val trashFileName = "${timestamp}_${fileName}"

            // Create the trash file
            val trashFile = trashFolder.createFile("image/*", trashFileName)
                ?: throw IOException("Failed to create trash file: $trashFileName")

            // Copy file to trash
            copyFile(sourceUri, trashFile.uri)

            // Verify copy succeeded (check size)
            val copiedSize = getFileSize(trashFile.uri)
            if (copiedSize != fileSize) {
                // Copy failed or incomplete - clean up trash file
                trashFile.delete()
                throw IOException("File copy verification failed. Source: $fileSize, Copied: $copiedSize")
            }

            // Delete original file
            val deleted = deleteFile(sourceUri)
            if (!deleted) {
                // Original delete failed - clean up trash copy to avoid duplicates
                trashFile.delete()
                throw IOException("Failed to delete original file after copy: $sourceUri")
            }

            // Create and persist trash item
            val movedAt = System.currentTimeMillis()
            val trashItem = TrashItem(
                originalUri = sourceUri.toString(),
                trashUri = trashFile.uri.toString(),
                fileName = fileName,
                fileSize = fileSize,
                movedAt = movedAt
            )

            trashRepository.addToTrash(trashItem)

            Log.d(TAG, "Moved to trash: $fileName (size: $fileSize)")
            trashItem
        }
    }

    /**
     * Restore a file from trash back to its original location.
     *
     * If the original location already has a file with the same name,
     * a numeric suffix is appended (e.g., "photo_001.jpg").
     *
     * @param trashItem The trash item to restore
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun restoreFromTrash(trashItem: TrashItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceUri = Uri.parse(trashItem.originalUri)
            val trashUri = Uri.parse(trashItem.trashUri)

            // Determine the parent directory of original location
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(
                sourceUri,
                DocumentsContract.getTreeDocumentId(sourceUri)
            )

            val parentDir = DocumentFile.fromTreeUri(context, parentUri)
                ?: throw IOException("Cannot access parent directory for: $sourceUri")

            // Generate unique filename if conflict exists
            val restoredFileName = generateUniqueFileName(parentDir, trashItem.fileName)

            // Create the restored file
            val restoredFile = parentDir.createFile("image/*", restoredFileName)
                ?: throw IOException("Failed to create restored file: $restoredFileName")

            // Copy from trash back to original location
            copyFile(trashUri, restoredFile.uri)

            // Verify copy
            val restoredSize = getFileSize(restoredFile.uri)
            if (restoredSize != trashItem.fileSize) {
                restoredFile.delete()
                throw IOException("Restore verification failed")
            }

            // Delete from trash
            val trashFile = DocumentFile.fromSingleUri(context, trashUri)
            trashFile?.delete()

            // Mark as restored in database
            trashRepository.markRestored(trashItem.id)

            Log.d(TAG, "Restored from trash: ${trashItem.fileName} to $restoredFileName")
        }
    }

    /**
     * Permanently delete a file from trash.
     *
     * This is called by TrashCleanupWorker for items past retention period,
     * or can be called manually for immediate deletion.
     *
     * @param trashItem The trash item to permanently delete
     * @return Result.success(Unit) on success, Result.failure on error
     */
    suspend fun permanentlyDelete(trashItem: TrashItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trashUri = Uri.parse(trashItem.trashUri)

            // Delete the actual file
            val trashFile = DocumentFile.fromSingleUri(context, trashUri)
            val deleted = trashFile?.delete() ?: false

            // Remove from database regardless of file deletion result
            // (file may have been manually deleted)
            trashRepository.removeFromTrash(trashItem.id)

            if (deleted) {
                Log.d(TAG, "Permanently deleted: ${trashItem.fileName}")
            } else {
                Log.w(TAG, "File already gone or delete failed: ${trashItem.fileName}")
            }
        }
    }

    /**
     * Get all active (non-restored) trash items.
     *
     * @return Flow of trash items, sorted by most recently moved first
     */
    fun getTrashItems() = trashRepository.getTrashItems()

    /**
     * Get expired items that should be cleaned up.
     *
     * @param now Current timestamp (defaults to System.currentTimeMillis())
     * @return List of expired trash items
     */
    suspend fun getExpiredItems(now: Long = System.currentTimeMillis()): List<TrashItem> {
        return trashRepository.getExpiredItems(now)
    }

    /**
     * Clean up expired items (items older than 7 days).
     *
     * This is called by TrashCleanupWorker during daily cleanup.
     *
     * @param now Current timestamp
     * @return Number of items deleted
     */
    suspend fun cleanupExpired(now: Long = System.currentTimeMillis()): Int {
        val expiredItems = getExpiredItems(now)
        var deletedCount = 0

        for (item in expiredItems) {
            permanentlyDelete(item).onSuccess {
                deletedCount++
            }.onFailure { e ->
                Log.e(TAG, "Failed to delete expired item: ${item.fileName}", e)
            }
        }

        Log.d(TAG, "Cleanup completed: $deletedCount items deleted")
        return deletedCount
    }

    /**
     * Get the size of the trash folder.
     *
     * @return Total size in bytes of all active trash items
     */
    suspend fun getTrashSize(): Long {
        return trashRepository.getTrashSize()
    }

    /**
     * Get or create the hidden .trash folder in Pictures root.
     *
     * The folder is created as a hidden folder (dot prefix) to avoid
     * appearing in most gallery apps.
     *
     * @return DocumentFile representing the trash folder, or null if creation fails
     */
    private suspend fun getOrCreateTrashFolder(): DocumentFile? = withContext(Dispatchers.IO) {
        // Get Pictures directory
        val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_PICTURES
        )

        // Create .trash folder path
        val trashDir = java.io.File(picturesDir, TRASH_FOLDER_NAME)

        if (!trashDir.exists()) {
            val created = trashDir.mkdirs()
            if (!created) {
                Log.e(TAG, "Failed to create trash folder: $trashDir")
                return@withContext null
            }
            // Mark as hidden by creating .nomedia file
            val nomediaFile = java.io.File(trashDir, ".nomedia")
            nomediaFile.createNewFile()
        }

        // Convert to DocumentFile
        DocumentFile.fromFile(trashDir)
    }

    /**
     * Copy file from source to destination.
     */
    private fun copyFile(sourceUri: Uri, destUri: Uri) {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            context.contentResolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output, BUFFER_SIZE)
            } ?: throw IOException("Cannot open output stream: $destUri")
        } ?: throw IOException("Cannot open input stream: $sourceUri")
    }

    /**
     * Get file size from URI.
     */
    private fun getFileSize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            null
        }
    }

    /**
     * Delete file at URI.
     */
    private fun deleteFile(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            false
        }
    }

    /**
     * Generate a unique filename by appending a numeric suffix if needed.
     */
    private fun generateUniqueFileName(parentDir: DocumentFile, originalName: String): String {
        if (parentDir.findFile(originalName) == null) {
            return originalName
        }

        val extension = originalName.substringAfterLast(".", "")
        val baseName = originalName.substringBeforeLast(".", originalName)

        var counter = 1
        var newName: String
        do {
            newName = if (extension.isNotEmpty()) {
                "${baseName}_${counter.toString().padStart(3, '0')}.$extension"
            } else {
                "${baseName}_${counter.toString().padStart(3, '0')}"
            }
            counter++
        } while (parentDir.findFile(newName) != null)

        return newName
    }
}
