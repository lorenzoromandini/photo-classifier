package com.example.photoorganizer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.photoorganizer.data.local.database.entities.LearningStatus
import com.example.photoorganizer.domain.model.Folder

/**
 * List component displaying folders grouped by learning status.
 *
 * Shows folders in a LazyColumn for performance with large lists.
 * Groups folders by: Completed, In Progress, Pending.
 *
 * @param folders List of folders to display
 * @param onFolderClick Optional click handler for folder selection
 * @param onRefreshClick Optional handler for refresh action
 * @param isLoading Whether to show loading state
 * @param errorMessage Optional error message to display
 * @param modifier Modifier for styling
 */
@Composable
fun FolderList(
    folders: List<Folder>,
    onFolderClick: ((Folder) -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                LoadingState()
            }
            errorMessage != null -> {
                ErrorState(
                    message = errorMessage,
                    onRetry = onRefreshClick
                )
            }
            folders.isEmpty() -> {
                EmptyState(onAction = onRefreshClick)
            }
            else -> {
                FolderListContent(
                    folders = folders,
                    onFolderClick = onFolderClick
                )
            }
        }
    }
}

/**
 * Content showing the folder list with section headers.
 */
@Composable
private fun FolderListContent(
    folders: List<Folder>,
    onFolderClick: ((Folder) -> Unit)? = null
) {
    // Group folders by learning status
    val completedFolders = folders.filter { it.learningStatus == LearningStatus.COMPLETED }
    val inProgressFolders = folders.filter { it.learningStatus == LearningStatus.IN_PROGRESS }
    val pendingFolders = folders.filter { it.learningStatus == LearningStatus.PENDING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Completed section
        if (completedFolders.isNotEmpty()) {
            item {
                SectionHeader(title = "Ready (${completedFolders.size})")
            }

            items(
                items = completedFolders,
                key = { it.uri }
            ) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick?.invoke(folder) }
                )
            }
        }

        // In Progress section
        if (inProgressFolders.isNotEmpty()) {
            item {
                SectionHeader(title = "Learning (${inProgressFolders.size})")
            }

            items(
                items = inProgressFolders,
                key = { it.uri }
            ) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick?.invoke(folder) }
                )
            }
        }

        // Pending section
        if (pendingFolders.isNotEmpty()) {
            item {
                SectionHeader(title = "Pending (${pendingFolders.size})")
            }

            items(
                items = pendingFolders,
                key = { it.uri }
            ) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick?.invoke(folder) }
                )
            }
        }
    }
}

/**
 * Section header for folder groups.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Loading state with circular progress indicator.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading folders...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Empty state shown when no folders are discovered.
 */
@Composable
private fun EmptyState(onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOff,
            contentDescription = null,
            modifier = androidx.compose.foundation.layout.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(16.dp))

        Text(
            text = "No folders found",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(8.dp))

        Text(
            text = "Complete onboarding to discover your photo folders",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onAction != null) {
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(24.dp))

            OutlinedButton(onClick = onAction) {
                Text("Refresh")
            }
        }
    }
}

/**
 * Error state with retry option.
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Error,
            contentDescription = null,
            modifier = androidx.compose.foundation.layout.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(16.dp))

        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.height(24.dp))

            OutlinedButton(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderListPreview() {
    MaterialTheme {
        FolderList(
            folders = listOf(
                Folder(
                    uri = "content://uri1",
                    name = "Nature",
                    displayName = "Nature Photos",
                    photoCount = 89,
                    learningStatus = LearningStatus.COMPLETED,
                    learnedLabels = mapOf("mountain" to 0.92f, "water" to 0.87f)
                ),
                Folder(
                    uri = "content://uri2",
                    name = "Family",
                    displayName = "Family",
                    photoCount = 156,
                    learningStatus = LearningStatus.IN_PROGRESS
                ),
                Folder(
                    uri = "content://uri3",
                    name = "Vacation",
                    displayName = "Vacation 2024",
                    photoCount = 42,
                    learningStatus = LearningStatus.PENDING
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderListEmptyPreview() {
    MaterialTheme {
        FolderList(folders = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderListLoadingPreview() {
    MaterialTheme {
        FolderList(folders = emptyList(), isLoading = true)
    }
}
