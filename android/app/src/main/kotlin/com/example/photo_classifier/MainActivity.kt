package com.example.photo_classifier

import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var platformChannelHandler: PlatformChannelHandler
    
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        platformChannelHandler = PlatformChannelHandler(this)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            PlatformChannelHandler.CHANNEL
        ).setMethodCallHandler(platformChannelHandler)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle folder picker result
        val result = platformChannelHandler.handlePickFolderResult(requestCode, resultCode, data)
        if (result != null) {
            // Result was handled, clear the pending result
        }
    }
}
