package com.example.photoorganizer.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Component showing progress during folder discovery.
 *
 * Displays either an indeterminate progress indicator (when total is unknown)
 * or a determinate progress bar with count. Shows discovered folder count
 * and total photo count with appropriate Material 3 styling.
 *
 * @param discoveredCount Number of folders discovered so far
 * @param totalCount Total number of folders (null for indeterminate)
 * @param photoCount Total number of photos found
 * @param modifier Modifier for the component
 * @param isIndeterminate Whether to show indeterminate progress (when total unknown)
 */
@Composable
fun FolderDiscoveryProgress(
    discoveredCount: Int,
    totalCount: Int? = null,
    photoCount: Int = 0,
    modifier: Modifier = Modifier,
    isIndeterminate: Boolean = totalCount == null || totalCount == 0
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Scanning Your Photos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress indicator
        if (isIndeterminate) {
            // Indeterminate progress when we don't know total
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Determinate progress bar
            val progress by animateFloatAsState(
                targetValue = discoveredCount.toFloat() / totalCount!!.toFloat(),
                label = "discovery_progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Discovery stats
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Folder count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildFolderText(discoveredCount, totalCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Photo count (if any found)
            if (photoCount > 0) {
                Text(
                    text = "$photoCount photos found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Helper text
        Text(
            text = "This may take a moment for large libraries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Helper function to build folder discovery text.
 */
private fun buildFolderText(discovered: Int, total: Int?): String {
    return if (total != null && total > 0) {
        "Found $discovered of $total folders"
    } else {
        "Found $discovered ${if (discovered == 1) "folder" else "folders"}"
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderDiscoveryProgressIndeterminatePreview() {
    MaterialTheme {
        FolderDiscoveryProgress(
            discoveredCount = 5,
            totalCount = null,
            photoCount = 127
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderDiscoveryProgressDeterminatePreview() {
    MaterialTheme {
        FolderDiscoveryProgress(
            discoveredCount = 3,
            totalCount = 8,
            photoCount = 245
        )
    }
}