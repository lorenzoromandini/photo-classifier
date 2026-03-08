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
    private var pendingResult: MethodChannel.Result? = null
    
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "pickFolder" -> pickFolder(result)
            "discoverFolders" -> discoverFolders(call, result)
            "persistPermission" -> persistPermission(call, result)
            "hasPermission" -> hasPermission(call, result)
            "listPhotos" -> listPhotos(call, result)
            "countPhotos" -> countPhotos(call, result)
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
}
