package com.example.photoorganizer.data.local.saf

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Converts a URI to a DocumentFile.
 *
 * @param context Application context
 * @return DocumentFile if URI is valid and accessible, null otherwise
 */
suspend fun Uri.toDocumentFile(context: Context): DocumentFile? = withContext(Dispatchers.IO) {
    DocumentFile.fromTreeUri(context, this@toDocumentFile)
}

/**
 * Checks if a DocumentFile is an image file.
 * Uses MIME type check first, falls back to extension check.
 *
 * @return true if file is an image, false otherwise
 */
fun DocumentFile.isImage(): Boolean {
    if (!isFile) return false

    // Check by MIME type first
    type?.let { mimeType ->
        if (ImageMimeType.isSupported(mimeType)) {
            return true
        }
    }

    // Fall back to extension check
    val fileName = name ?: return false
    return ImageMimeType.isSupportedExtension(fileName)
}

/**
 * Gets the MIME type of a DocumentFile.
 *
 * @return MIME type string, or "application/octet-stream" if unknown
 */
fun DocumentFile.getMimeType(): String {
    return type ?: "application/octet-stream"
}

/**
 * Checks if this DocumentFile represents a valid folder for user content.
 * Filters out system folders like Android/, .thumbnails/, etc.
 *
 * @return true if valid user folder, false otherwise
 */
fun DocumentFile.isValidFolder(): Boolean {
    if (!isDirectory) return false
    val folderName = name ?: return false
    return !SafConstants.EXCLUDED_FOLDERS.contains(folderName)
}

/**
 * Gets the MediaStore URI for the Pictures directory.
 *
 * @return Uri for the Pictures directory
 */
fun getPicturesUri(): Uri {
    return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}

/**
 * Checks if a folder name is valid (not a system folder).
 *
 * @param name The folder name to check
 * @return true if valid, false if system folder
 */
fun isValidFolderName(name: String): Boolean {
    return !SafConstants.EXCLUDED_FOLDERS.contains(name)
}

/**
 * Constants for SAF operations.
 */
object SafConstants {

    /**
     * System folders to exclude from discovery.
     */
    val EXCLUDED_FOLDERS = setOf(
        "Android",
        ".thumbnails",
        ".trash",
        "lost+found",
        ".cache",
        "temp",
        ".tmp",
        ".nomedia"
    )

    /**
     * Supported image MIME types.
     */
    val SUPPORTED_IMAGE_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heic",
        "image/heif",
        "image/bmp",
        "image/gif",
        "image/tiff"
    )

    /**
     * Supported image file extensions (case-insensitive).
     */
    val SUPPORTED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif", "tiff", "tif"
    )
}

/**
 * Enum representing supported image MIME types.
 */
enum class ImageMimeType(val mimeType: String, val extensions: List<String>) {
    JPEG("image/jpeg", listOf("jpg", "jpeg")),
    PNG("image/png", listOf("png")),
    WEBP("image/webp", listOf("webp")),
    HEIC("image/heic", listOf("heic")),
    HEIF("image/heif", listOf("heif")),
    BMP("image/bmp", listOf("bmp")),
    GIF("image/gif", listOf("gif")),
    TIFF("image/tiff", listOf("tiff", "tif"));

    companion object {
        /**
         * Gets the ImageMimeType from a MIME type string.
         *
         * @param mimeType The MIME type string
         * @return ImageMimeType or null if not supported
         */
        fun fromMimeType(mimeType: String): ImageMimeType? {
            return values().find { it.mimeType == mimeType.lowercase() }
        }

        /**
         * Gets the ImageMimeType from a file extension.
         *
         * @param extension The file extension (with or without dot)
         * @return ImageMimeType or null if not supported
         */
        fun fromExtension(extension: String): ImageMimeType? {
            val ext = extension.removePrefix(".").lowercase()
            return values().find { it.extensions.contains(ext) }
        }

        /**
         * Checks if a MIME type is supported.
         *
         * @param mimeType The MIME type string
         * @return true if supported, false otherwise
         */
        fun isSupported(mimeType: String): Boolean {
            return values().any { it.mimeType == mimeType.lowercase() }
        }

        /**
         * Checks if a file extension is supported.
         *
         * @param fileName The file name or extension
         * @return true if supported, false otherwise
         */
        fun isSupportedExtension(fileName: String): Boolean {
            val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
                ?.lowercase()
                ?: fileName.substringAfterLast(".", "").lowercase()
            return SafConstants.SUPPORTED_EXTENSIONS.contains(extension)
        }
    }
}

/**
 * Data class representing folder information from SAF discovery.
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