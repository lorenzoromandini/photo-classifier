package com.example.photoorganizer.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photoorganizer.ui.components.FolderList
import kotlinx.coroutines.delay

/**
 * Main screen showing discovered folders and learning progress.
 *
 * Features:
 * - Folder list with learning status
 * - Pull-to-refresh for manual sync
 * - Settings navigation
 * - Permission warnings
 * - Storage full warnings
 * - FAB for quick actions (if needed)
 *
 * @param onNavigateToSettings Callback to navigate to settings
 * @param onNavigateToOnboarding Callback to navigate back to onboarding for re-permissioning
 * @param viewModel The main screen ViewModel (injected via Hilt)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Check permissions on launch
    LaunchedEffect(Unit) {
        delay(500) // Brief delay to allow UI to settle
        val hasPermission = viewModel.checkPermissions()
        if (!hasPermission) {
            // Will show permission warning banner
        }
    }

    // Show errors in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    // Handle pull-to-refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshFolders()
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                onSettingsClick = onNavigateToSettings,
                onRefreshClick = { viewModel.refreshFolders() }
            )
        },
        floatingActionButton = {
            // Only show FAB if there are folders
            if (uiState.folders.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.refreshFolders() },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    text = { Text("Sync") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission warning banner
            AnimatedVisibility(visible = uiState.shouldShowPermissionWarning) {
                PermissionWarningBanner(
                    onFixClick = onNavigateToOnboarding,
                    onDismiss = { viewModel.dismissPermissionWarning() }
                )
            }

            // Storage warning banner
            AnimatedVisibility(visible = uiState.shouldShowStorageWarning) {
                StorageWarningBanner(
                    availableSpace = uiState.availableStorage,
                    onDismiss = { viewModel.dismissStorageWarning() }
                )
            }

            // Main content with pull-to-refresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                FolderList(
                    folders = uiState.folders,
                    onFolderClick = { folder ->
                        // Could navigate to folder detail view
                    },
                    onRefreshClick = { viewModel.refreshFolders() },
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage
                )

                // Pull-to-refresh indicator
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * Top app bar with title and action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text("Photo Organizer")
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh folders"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
    )
}

/**
 * Warning banner shown when permissions are lost.
 * Allows user to navigate back to onboarding to re-grant permissions.
 */
@Composable
private fun PermissionWarningBanner(
    onFixClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Photo Organizer needs access to your folders. Tap Fix to re-grant permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            TextButton(onClick = onFixClick) {
                Text("Fix")
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Warning banner shown when storage is low.
 */
@Composable
private fun StorageWarningBanner(
    availableSpace: Long,
    onDismiss: () -> Unit
) {
    val availableMB = availableSpace / (1024 * 1024)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Storage Running Low",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Only $availableMB MB available. Some operations may fail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    MaterialTheme {
        Column {
            MainTopAppBar(
                onSettingsClick = {},
                onRefreshClick = {}
            )
            PermissionWarningBanner(
                onFixClick = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageWarningPreview() {
    MaterialTheme {
        StorageWarningBanner(
            availableSpace = 50 * 1024 * 1024, // 50 MB
            onDismiss = {}
        )
    }
}
