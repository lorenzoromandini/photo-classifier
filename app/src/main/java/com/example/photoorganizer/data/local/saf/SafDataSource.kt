package com.example.photoorganizer.data.local.saf

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Storage Access Framework (SAF) operations.
 * Wraps SAF API to provide type-safe, coroutine-friendly access to user-selected folders.
 *
 * @property context Application context for content resolver and DocumentFile operations
 */
@Singleton
class SafDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Information about a discovered folder.
     *
     * @property uri The URI of the folder (as string for persistence)
     * @property name The technical folder name
     * @property displayName The user-friendly display name
     * @property photoCount Number of photos in the folder
     */
    data class FolderInfo(
        val uri: String,
        val name: String,
        val displayName: String,
        val photoCount: Int
    )

    /**
     * Discovers all folders within the base URI (typically Pictures/).
     * Filters out system folders and calculates photo counts.
     *
     * @param baseUri The tree URI to discover folders within
     * @return List of discovered folders with metadata
     * @throws SafException.InvalidUriException if URI is invalid
     * @throws SafException.PermissionDeniedException if permission is denied
     */
    suspend fun discoverFolders(baseUri: Uri): List<FolderInfo> = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromTreeUri(context, baseUri)
            ?: throw SafException.InvalidUriException(baseUri.toString())

        if (!documentFile.exists()) {
            throw SafException.FolderNotFoundException(baseUri.toString())
        }

        try {
            documentFile.listFiles()
                .filter { it.isDirectory && it.isValidFolder() }
                .map { folder ->
                    val photoCount = countPhotos(folder.uri)
                    FolderInfo(
                        uri = folder.uri.toString(),
                        name = folder.name ?: "unknown",
                        displayName = folder.name?.let { formatDisplayName(it) } ?: "Unknown Folder",
                        photoCount = photoCount
                    )
                }
                .sortedBy { it.displayName }
        } catch (e: SecurityException) {
            throw SafException.PermissionDeniedException(baseUri.toString())
        }
    }

    /**
     * Counts the number of image files in the specified folder.
     *
     * @param folderUri The URI of the folder to count photos in
     * @return Number of image files
     * @throws SafException.InvalidUriException if URI is invalid
     * @throws SafException.PermissionDeniedException if permission is denied
     */
    suspend fun countPhotos(folderUri: Uri): Int = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw SafException.InvalidUriException(folderUri.toString())

        try {
            documentFile.listFiles()
                .count { it.isImage() }
        } catch (e: SecurityException) {
            throw SafException.PermissionDeniedException(folderUri.toString())
        }
    }

    /**
     * Lists all photo files in the specified folder.
     *
     * @param folderUri The URI of the folder to list photos from
     * @return List of DocumentFile representing image files
     * @throws SafException.InvalidUriException if URI is invalid
     * @throws SafException.PermissionDeniedException if permission is denied
     */
    suspend fun listPhotos(folderUri: Uri): List<DocumentFile> = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw SafException.InvalidUriException(folderUri.toString())

        if (!documentFile.exists()) {
            throw SafException.FolderNotFoundException(folderUri.toString())
        }

        try {
            documentFile.listFiles()
                .filter { it.isImage() }
                .sortedBy { it.name }
        } catch (e: SecurityException) {
            throw SafException.PermissionDeniedException(folderUri.toString())
        }
    }

    /**
     * Persists URI permission using takePersistableUriPermission.
     * This allows the app to access the folder across reboots.
     *
     * @param uri The tree URI to persist permission for
     * @throws SafException.PermissionDeniedException if permission cannot be persisted
     */
    suspend fun persistFolderPermission(uri: Uri): Unit = withContext(Dispatchers.IO) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            throw SafException.PermissionDeniedException(uri.toString())
        }
    }

    /**
     * Checks if persistable permission exists for the given URI.
     *
     * @param uri The URI to check
     * @return true if permission is persisted, false otherwise
     */
    suspend fun hasPersistablePermission(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        contentResolver.persistedUriPermissions.any { persistedUri ->
            persistedUri.uri == uri && persistedUri.isReadPermission && persistedUri.isWritePermission
        }
    }

    /**
     * Gets the persisted permissions for debugging/logging purposes.
     *
     * @return List of persisted URI strings
     */
    suspend fun getPersistedPermissions(): List<String> = withContext(Dispatchers.IO) {
        contentResolver.persistedUriPermissions.map { it.uri.toString() }
    }

    /**
     * Formats folder name for display (e.g., "vacation_photos" → "Vacation Photos").
     */
    private fun formatDisplayName(name: String): String {
        return name
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    companion object {
        /**
         * System folders to exclude from discovery.
         */
        private val EXCLUDED_FOLDERS = setOf(
            "Android",
            ".thumbnails",
            ".trash",
            "lost+found",
            ".cache",
            "temp",
            ".tmp"
        )

        /**
         * Supported image MIME types.
         */
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif",
            "image/bmp",
            "image/gif"
        )

        /**
         * Supported image file extensions (case-insensitive).
         */
        private val SUPPORTED_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif"
        )

        /**
         * Checks if a DocumentFile is a valid folder (not system folder).
         */
        fun DocumentFile.isValidFolder(): Boolean {
            val folderName = name ?: return false
            return isDirectory && !EXCLUDED_FOLDERS.contains(folderName)
        }

        /**
         * Checks if a DocumentFile is an image file.
         */
        fun DocumentFile.isImage(): Boolean {
            if (!isFile) return false

            // Check by MIME type first
            type?.let { mimeType ->
                if (SUPPORTED_IMAGE_TYPES.contains(mimeType.lowercase())) {
                    return true
                }
            }

            // Fall back to extension check
            val fileName = name ?: return false
            val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
                ?.lowercase()
                ?: fileName.substringAfterLast(".", "").lowercase()

            return SUPPORTED_EXTENSIONS.contains(extension)
        }
    }
}