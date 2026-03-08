package com.example.photo_classifier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile

class SafHelper(private val context: Context) {
    companion object {
        private const val TAG = "SafHelper"
        
        val SYSTEM_FOLDER_BLACKLIST = setOf(
            "Android",
            ".thumbnails",
            ".trash",
            "lost+found",
            "LOST.DIR"
        )
        
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
    }
    
    /**
     * Discover folders in the given base tree URI
     * Returns a list of maps with folder metadata
     */
    fun discoverFolders(baseTreeUri: Uri): List<Map<String, Any>> {
        val folders = mutableListOf<Map<String, Any>>()
        
        try {
            val baseFile = DocumentFile.fromTreeUri(context, baseTreeUri)
                ?: run {
                    Log.e(TAG, "Failed to get DocumentFile from tree URI: $baseTreeUri")
                    return emptyList()
                }
            
            if (!baseFile.exists() || !baseFile.canRead()) {
                Log.e(TAG, "Base folder doesn't exist or can't be read")
                return emptyList()
            }
            
            for (file in baseFile.listFiles()) {
                if (file.isDirectory) {
                    val name = file.name ?: continue
                    
                    // Skip system folders
                    if (isSystemFolder(name)) {
                        Log.d(TAG, "Skipping system folder: $name")
                        continue
                    }
                    
                    val photoCount = countPhotos(file.uri)
                    
                    folders.add(mapOf(
                        "uri" to file.uri.toString(),
                        "name" to name,
                        "displayName" to name,
                        "photoCount" to photoCount,
                        "createdAt" to System.currentTimeMillis()
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering folders", e)
        }
        
        return folders
    }
    
    /**
     * Count image files in the given folder
     */
    fun countPhotos(folderUri: Uri): Int {
        var count = 0
        
        try {
            // Try as tree URI first
            var folderFile = DocumentFile.fromTreeUri(context, folderUri)
            
            // If not a tree URI, try as single URI
            if (folderFile == null) {
                folderFile = DocumentFile.fromSingleUri(context, folderUri)
            }
            
            if (folderFile == null || !folderFile.canRead()) {
                Log.w(TAG, "Cannot access folder: $folderUri")
                return 0
            }
            
            if (folderFile.isDirectory) {
                for (child in folderFile.listFiles()) {
                    if (child.isFile && isImageFile(child.name ?: "")) {
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting photos in $folderUri", e)
        }
        
        return count
    }
    
    /**
     * List photos in the given folder
     * Returns a list of maps with photo metadata
     */
    fun listPhotos(folderUri: Uri): List<Map<String, Any>> {
        val photos = mutableListOf<Map<String, Any>>()
        
        try {
            val folderFile = DocumentFile.fromTreeUri(context, folderUri)
                ?: DocumentFile.fromSingleUri(context, folderUri)
                ?: return emptyList()
            
            if (!folderFile.isDirectory) {
                Log.e(TAG, "Not a directory: $folderUri")
                return emptyList()
            }
            
            for (file in folderFile.listFiles()) {
                if (file.isFile && isImageFile(file.name ?: "")) {
                    photos.add(mapOf(
                        "uri" to file.uri.toString(),
                        "fileName" to (file.name ?: "unknown"),
                        "fileSize" to (file.length()),
                        "mimeType" to (file.type ?: "image/*")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing photos", e)
        }
        
        return photos
    }
    
    /**
     * Persist read permission for the given URI
     */
    fun persistPermission(uri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Log.d(TAG, "Persisted permission for URI: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist permission for URI: $uri", e)
            false
        }
    }
    
    /**
     * Check if we have persistable permission for the given URI
     */
    fun hasPersistablePermission(uri: Uri): Boolean {
        return try {
            val persistedUriPermissions = context.contentResolver.persistedUriPermissions
            persistedUriPermissions.any { it.uri == uri || uri.toString().startsWith(it.uri.toString()) }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission for URI: $uri", e)
            false
        }
    }
    
    /**
     * Check if a folder name is a system folder
     */
    fun isSystemFolder(name: String): Boolean {
        return SYSTEM_FOLDER_BLACKLIST.contains(name) || name.startsWith(".")
    }
    
    /**
     * Check if a file is an image based on extension
     */
    fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return IMAGE_EXTENSIONS.contains(extension)
    }
}
