package com.example.photoorganizer.ui.onboarding

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photoorganizer.ui.components.PermissionCard
import kotlinx.coroutines.delay

/**
 * Single-screen onboarding flow per user decision.
 * 
 * Shows app value proposition, requests SAF permission with context,
 * displays folder discovery progress, and completes to main screen.
 * No multi-step wizard - everything on one screen.
 *
 * @param onNavigateToMain Callback when onboarding completes
 * @param viewModel The onboarding ViewModel (injected via Hilt)
 */
@Composable
fun OnboardingScreen(
    onNavigateToMain: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Launcher for SAF document tree picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission immediately
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onPermissionGranted(uri)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Handle navigation when onboarding completes
    LaunchedEffect(uiState.stage) {
        if (uiState.stage == OnboardingStage.COMPLETE) {
            delay(1500) // Brief pause to show success state
            onNavigateToMain()
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState.stage,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 4 } togetherWith
                            fadeOut() + slideOutVertically { -it / 4 }
                },
                label = "onboarding_content"
            ) { stage ->
                when (stage) {
                    OnboardingStage.WELCOME,
                    OnboardingStage.REQUESTING_PERMISSION -> WelcomeContent(
                        isLoading = uiState.isLoading,
                        onGrantClick = {
                            viewModel.onRequestPermissionClick()
                            launcher.launch(null)
                        }
                    )

                    OnboardingStage.DISCOVERING -> DiscoveryContent(
                        discoveredFolders = uiState.discoveredFolders,
                        totalPhotos = uiState.totalPhotos,
                        isIndeterminate = true
                    )

                    OnboardingStage.COMPLETE -> CompleteContent(
                        discoveredFolders = uiState.discoveredFolders,
                        totalPhotos = uiState.totalPhotos
                    )

                    OnboardingStage.ERROR -> ErrorContent(
                        errorMessage = uiState.errorMessage ?: "An error occurred",
                        onRetry = { viewModel.onRetry() }
                    )
                }
            }
        }
    }
}

/**
 * Welcome content showing app value proposition and permission request.
 */
@Composable
private fun WelcomeContent(
    isLoading: Boolean,
    onGrantClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App icon/logo
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // App name and tagline
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Photo Organizer",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Automatically organize your photos with AI",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value proposition bullets
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ValueBullet("Smart AI categorization")
            ValueBullet("Automatic photo sorting")
            ValueBullet("Works offline - your photos stay private")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Permission card
        PermissionCard(
            title = "Access Your Photos",
            description = "Photo Organizer needs access to your Pictures folder to organize your photos. " +
                    "Your files stay on your device - we never upload them.",
            buttonText = if (isLoading) "Processing..." else "Grant Access",
            onGrantClick = onGrantClick,
            isEnabled = !isLoading
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * Value bullet point component.
 */
@Composable
private fun ValueBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Content shown during folder discovery.
 */
@Composable
private fun DiscoveryContent(
    discoveredFolders: Int,
    totalPhotos: Int,
    isIndeterminate: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        FolderDiscoveryProgress(
            discoveredCount = discoveredFolders,
            totalCount = null, // Indeterminate progress
            photoCount = totalPhotos,
            isIndeterminate = isIndeterminate
        )
    }
}

/**
 * Content shown when onboarding completes successfully.
 */
@Composable
private fun CompleteContent(
    discoveredFolders: Int,
    totalPhotos: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "All Set!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Found $discoveredFolders folders with $totalPhotos photos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Taking you to your photos...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CircularProgressIndicator()
    }
}

/**
 * Content shown when an error occurs.
 */
@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Something Went Wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Button(onClick = onRetry) {
            Text("Try Again")
        }

        TextButton(onClick = onRetry) {
            Text("Skip for now")
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
private fun OnboardingScreenPreview() {
    MaterialTheme {
        WelcomeContent(
            isLoading = false,
            onGrantClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoveryContentPreview() {
    MaterialTheme {
        DiscoveryContent(
            discoveredFolders = 5,
            totalPhotos = 127,
            isIndeterminate = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompleteContentPreview() {
    MaterialTheme {
        CompleteContent(
            discoveredFolders = 8,
            totalPhotos = 245
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentPreview() {
    MaterialTheme {
        ErrorContent(
            errorMessage = "Could not access your photos. Please check your permissions and try again.",
            onRetry = {}
        )
    }
}