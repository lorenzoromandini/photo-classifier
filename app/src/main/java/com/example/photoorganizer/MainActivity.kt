package com.example.photoorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.photoorganizer.navigation.PhotoOrganizerNavigation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Photo Organizer app.
 *
 * Uses Compose for UI and Hilt for dependency injection.
 * Sets up edge-to-edge display and Material 3 theme.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoOrganizerNavigation()
                }
            }
        }
    }
}