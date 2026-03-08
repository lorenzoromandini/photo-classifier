package com.example.photo_classifier

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class PlatformChannelHandler(private val activity: Activity) : MethodChannel.MethodCallHandler {
    companion object {
        private const val TAG = "PlatformChannel"
        const val CHANNEL = "com.photo_classifier/platform"
        const val REQUEST_CODE_PICK_FOLDER = 1001
    }
    
    private val safHelper = SafHelper(activity)
    private val safeFileHelper = SafeFileHelper(activity)
    private val trashHelper = TrashHelper(activity)
    private var pendingResult: MethodChannel.Result? = null
    
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "pickFolder" -> pickFolder(result)
            "discoverFolders" -> discoverFolders(call, result)
            "persistPermission" -> persistPermission(call, result)
            "hasPermission" -> hasPermission(call, result)
            "listPhotos" -> listPhotos(call, result)
            "countPhotos" -> countPhotos(call, result)
            "copyFile" -> copyFile(call, result)
            "verifyFile" -> verifyFile(call, result)
            "deleteFile" -> deleteFile(call, result)
            "getFileSize" -> getFileSize(call, result)
            "getAvailableStorage" -> getAvailableStorage(result)
            "exists" -> exists(call, result)
            "createTrashFolder" -> createTrashFolder(call, result)
            "getTrashFolderUri" -> getTrashFolderUri(call, result)
            "moveToTrash" -> moveToTrash(call, result)
            "restoreFromTrash" -> restoreFromTrash(call, result)
            "permanentlyDelete" -> permanentlyDelete(call, result)
            else -> result.notImplemented()
        }
    }
    
    /**
     * Launch the SAF folder picker
     */
    private fun pickFolder(result: MethodChannel.Result) {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                putExtra("android.provider.extra.SHOW_ADVANCED", true)
            }
            
            pendingResult = result
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching folder picker", e)
            result.error("FOLDER_PICK_ERROR", "Failed to launch folder picker: ${e.message}", e)
            pendingResult = null
        }
    }
    
    /**
     * Handle the result from folder picker
     */
    fun handlePickFolderResult(requestCode: Int, resultCode: Int, data: Intent?): MethodChannel.Result? {
        if (requestCode != REQUEST_CODE_PICK_FOLDER) return null
        
        val res = pendingResult ?: return null
        pendingResult = null
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data ?: return res.also {
                it.error("NO_URI", "No URI returned from picker", null)
            }
            
            try {
                // Take persistable permission
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                val uriString = uri.toString()
                Log.d(TAG, "Folder selected, persisted permission: $uriString")
                
                res.success(uriString)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling folder selection", e)
                res.error("PERMISSION_ERROR", "Failed to persist permission: ${e.message}", e)
            }
        } else {
            res.success(null) // User cancelled
        }
        
        return res
    }
    
    /**
     * Discover folders in the given tree URI
     */
    private fun discoverFolders(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            val folders = safHelper.discoverFolders(uri)
            
            Log.d(TAG, "Discovered ${folders.size} folders")
            result.success(folders)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering folders", e)
            result.error("DISCOVERY_ERROR", "Failed to discover folders: ${e.message}", e)
        }
    }
    
    /**
     * Persist permission for the given URI
     */
    private fun persistPermission(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            val success = safHelper.persistPermission(uri)
            
            if (success) {
                result.success(true)
            } else {
                result.error("PERMISSION_ERROR", "Failed to persist permission", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting permission", e)
            result.error("PERMISSION_ERROR", "Failed to persist permission: ${e.message}", e)
        }
    }
    
    /**
     * Check if we have persistable permission for the given URI
     */
    private fun hasPermission(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            val hasPermission = safHelper.hasPersistablePermission(uri)
            
            Log.d(TAG, "Has permission for $uriString: $hasPermission")
            result.success(hasPermission)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permission", e)
            result.error("PERMISSION_ERROR", "Failed to check permission: ${e.message}", e)
        }
    }
    
    /**
     * List photos in the given folder
     */
    private fun listPhotos(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            val photos = safHelper.listPhotos(uri)
            
            Log.d(TAG, "Listed ${photos.size} photos")
            result.success(photos)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing photos", e)
            result.error("PHOTO_LIST_ERROR", "Failed to list photos: ${e.message}", e)
        }
    }
    
    /**
     * Count photos in the given folder
     */
    private fun countPhotos(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            val count = safHelper.countPhotos(uri)
            
            Log.d(TAG, "Counted $count photos")
            result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error counting photos", e)
            result.error("PHOTO_COUNT_ERROR", "Failed to count photos: ${e.message}", e)
        }
    }
    
    /**
     * Copy file from source to destination via SAF
     */
    private fun copyFile(call: MethodCall, result: MethodChannel.Result) {
        try {
            val sourceUriString = call.argument<String>("sourceUri")
                ?: return result.error("INVALID_URI", "Source URI is required", null)
            val destUriString = call.argument<String>("destUri")
                ?: return result.error("INVALID_URI", "Destination URI is required", null)
            
            val sourceUri = Uri.parse(sourceUriString)
            val destUri = Uri.parse(destUriString)
            
            safeFileHelper.copyFile(sourceUri, destUri)
                .fold(
                    onSuccess = { bytesCopied ->
                        Log.d(TAG, "Copied $bytesCopied bytes")
                        result.success(mapOf(
                            "success" to true,
                            "data" to bytesCopied
                        ))
                    },
                    onFailure = { error ->
                        val errorCode = when (error) {
                            is IllegalStateException -> "STORAGE_FULL"
                            is SecurityException -> "PERMISSION_DENIED"
                            is IllegalArgumentException -> "FILE_NOT_FOUND"
                            else -> "IO_ERROR"
                        }
                        Log.e(TAG, "Copy failed: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to errorCode
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in copyFile", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Verify file copy by comparing sizes and checksums
     */
    private fun verifyFile(call: MethodCall, result: MethodChannel.Result) {
        try {
            val sourceUriString = call.argument<String>("sourceUri")
                ?: return result.error("INVALID_URI", "Source URI is required", null)
            val destUriString = call.argument<String>("destUri")
                ?: return result.error("INVALID_URI", "Destination URI is required", null)
            
            val sourceUri = Uri.parse(sourceUriString)
            val destUri = Uri.parse(destUriString)
            
            safeFileHelper.verifyFile(sourceUri, destUri)
                .fold(
                    onSuccess = { matches ->
                        if (matches) {
                            Log.d(TAG, "Verification passed")
                            result.success(mapOf(
                                "success" to true,
                                "data" to true
                            ))
                        } else {
                            Log.e(TAG, "Verification failed: files don't match")
                            result.success(mapOf(
                                "success" to false,
                                "error" to "File verification failed: size or checksum mismatch",
                                "errorCode" to "VERIFICATION_FAILED"
                            ))
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Verification error: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "VERIFICATION_FAILED"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyFile", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "VERIFICATION_FAILED"
            ))
        }
    }
    
    /**
     * Delete file via SAF
     */
    private fun deleteFile(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            
            safeFileHelper.deleteFile(uri)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "File deleted successfully")
                        result.success(mapOf(
                            "success" to true,
                            "data" to true
                        ))
                    },
                    onFailure = { error ->
                        val errorCode = when (error) {
                            is SecurityException -> "PERMISSION_DENIED"
                            is IllegalArgumentException -> "FILE_NOT_FOUND"
                            else -> "IO_ERROR"
                        }
                        Log.e(TAG, "Delete failed: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to errorCode
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in deleteFile", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Get file size in bytes
     */
    private fun getFileSize(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            
            safeFileHelper.getFileSize(uri)
                .fold(
                    onSuccess = { size ->
                        result.success(mapOf(
                            "success" to true,
                            "data" to size
                        ))
                    },
                    onFailure = { error ->
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "FILE_NOT_FOUND"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getFileSize", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Get available storage in bytes
     */
    private fun getAvailableStorage(result: MethodChannel.Result) {
        try {
            safeFileHelper.getAvailableStorage()
                .fold(
                    onSuccess = { bytes ->
                        result.success(mapOf(
                            "success" to true,
                            "data" to bytes
                        ))
                    },
                    onFailure = { error ->
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "IO_ERROR"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAvailableStorage", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Check if file exists
     */
    private fun exists(call: MethodCall, result: MethodChannel.Result) {
        try {
            val uriString = call.argument<String>("uri")
                ?: return result.error("INVALID_URI", "URI is required", null)
            
            val uri = Uri.parse(uriString)
            
            safeFileHelper.exists(uri)
                .fold(
                    onSuccess = { fileExists ->
                        result.success(mapOf(
                            "success" to true,
                            "data" to fileExists
                        ))
                    },
                    onFailure = { error ->
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "IO_ERROR"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in exists", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Create the .trash folder in the given base URI
     */
    private fun createTrashFolder(call: MethodCall, result: MethodChannel.Result) {
        try {
            val baseUriString = call.argument<String>("baseUri")
                ?: return result.error("INVALID_URI", "Base URI is required", null)
            
            val baseUri = Uri.parse(baseUriString)
            
            trashHelper.createTrashFolder(baseUri)
                .fold(
                    onSuccess = { trashUri ->
                        Log.d(TAG, "Created trash folder: $trashUri")
                        result.success(mapOf(
                            "success" to true,
                            "data" to trashUri.toString()
                        ))
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to create trash folder: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "IO_ERROR"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in createTrashFolder", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Get the .trash folder URI if it exists
     */
    private fun getTrashFolderUri(call: MethodCall, result: MethodChannel.Result) {
        try {
            val baseUriString = call.argument<String>("baseUri")
                ?: return result.error("INVALID_URI", "Base URI is required", null)
            
            val baseUri = Uri.parse(baseUriString)
            
            trashHelper.getTrashFolderUri(baseUri)
                .fold(
                    onSuccess = { trashUri ->
                        result.success(mapOf(
                            "success" to true,
                            "data" to (trashUri?.toString() ?: "")
                        ))
                    },
                    onFailure = { error ->
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to "IO_ERROR"
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in getTrashFolderUri", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Move a file to the trash folder
     */
    private fun moveToTrash(call: MethodCall, result: MethodChannel.Result) {
        try {
            val sourceUriString = call.argument<String>("sourceUri")
                ?: return result.error("INVALID_URI", "Source URI is required", null)
            val trashFolderUriString = call.argument<String>("trashFolderUri")
                ?: return result.error("INVALID_URI", "Trash folder URI is required", null)
            val fileName = call.argument<String>("fileName")
                ?: return result.error("INVALID_FILE_NAME", "File name is required", null)
            
            val sourceUri = Uri.parse(sourceUriString)
            val trashFolderUri = Uri.parse(trashFolderUriString)
            
            trashHelper.moveToTrash(sourceUri, trashFolderUri, fileName)
                .fold(
                    onSuccess = { trashUri ->
                        Log.d(TAG, "Moved to trash: $trashUri")
                        result.success(mapOf(
                            "success" to true,
                            "data" to trashUri
                        ))
                    },
                    onFailure = { error ->
                        val errorCode = when (error) {
                            is SecurityException -> "PERMISSION_DENIED"
                            is IllegalArgumentException -> "FILE_NOT_FOUND"
                            else -> "IO_ERROR"
                        }
                        Log.e(TAG, "Move to trash failed: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to errorCode
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in moveToTrash", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Restore a file from trash to original location
     */
    private fun restoreFromTrash(call: MethodCall, result: MethodChannel.Result) {
        try {
            val trashUriString = call.argument<String>("trashUri")
                ?: return result.error("INVALID_URI", "Trash URI is required", null)
            val destUriString = call.argument<String>("destUri")
                ?: return result.error("INVALID_URI", "Destination URI is required", null)
            
            val trashUri = Uri.parse(trashUriString)
            val destUri = Uri.parse(destUriString)
            
            trashHelper.restoreFromTrash(trashUri, destUri)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "Restored from trash: $destUri")
                        result.success(mapOf(
                            "success" to true,
                            "data" to true
                        ))
                    },
                    onFailure = { error ->
                        val errorCode = when (error) {
                            is SecurityException -> "PERMISSION_DENIED"
                            is IllegalArgumentException -> "FILE_NOT_FOUND"
                            is IllegalStateException -> "DESTINATION_EXISTS"
                            else -> "IO_ERROR"
                        }
                        Log.e(TAG, "Restore failed: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to errorCode
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in restoreFromTrash", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
    
    /**
     * Permanently delete a file from trash
     */
    private fun permanentlyDelete(call: MethodCall, result: MethodChannel.Result) {
        try {
            val trashUriString = call.argument<String>("trashUri")
                ?: return result.error("INVALID_URI", "Trash URI is required", null)
            
            val trashUri = Uri.parse(trashUriString)
            
            trashHelper.permanentlyDelete(trashUri)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "Permanently deleted from trash: $trashUri")
                        result.success(mapOf(
                            "success" to true,
                            "data" to true
                        ))
                    },
                    onFailure = { error ->
                        val errorCode = when (error) {
                            is SecurityException -> "PERMISSION_DENIED"
                            is IllegalArgumentException -> "FILE_NOT_FOUND"
                            else -> "IO_ERROR"
                        }
                        Log.e(TAG, "Permanent delete failed: ${error.message}")
                        result.success(mapOf(
                            "success" to false,
                            "error" to (error.message ?: "Unknown error"),
                            "errorCode" to errorCode
                        ))
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Error in permanentlyDelete", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message ?: "Unknown error",
                "errorCode" to "IO_ERROR"
            ))
        }
    }
}
