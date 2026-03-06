package com.example.photoorganizer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.photoorganizer.data.local.database.entities.LearningStatus
import com.example.photoorganizer.domain.model.Folder

/**
 * Card component displaying a folder with its learning status.
 *
 * Shows folder name, photo count, and current learning status.
 * For completed folders, displays learned categories.
 *
 * @param folder The folder to display
 * @param onClick Optional click handler for folder selection
 * @param modifier Modifier for styling
 */
@Composable
fun FolderCard(
    folder: Folder,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick ?: {},
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Folder name and icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${folder.photoCount} photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Learning status indicator
            LearningStatusIndicator(folder.learningStatus)

            // Show learned categories if completed
            if (folder.learningStatus == LearningStatus.COMPLETED && folder.learnedLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LearnedCategoriesRow(labels = folder.learnedLabels)
            }
        }
    }
}

/**
 * Visual indicator for learning status with icon and label.
 */
@Composable
private fun LearningStatusIndicator(status: LearningStatus) {
    val (icon, label, color) = when (status) {
        LearningStatus.PENDING -> Triple(
            Icons.Default.Schedule,
            "Learning pending",
            MaterialTheme.colorScheme.outline
        )
        LearningStatus.IN_PROGRESS -> Triple(
            Icons.Default.Sync,
            "Learning in progress",
            MaterialTheme.colorScheme.primary
        )
        LearningStatus.COMPLETED -> Triple(
            Icons.Default.CheckCircle,
            "Learning complete",
            MaterialTheme.colorScheme.primary
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

/**
 * Row displaying learned category labels as chips.
 *
 * Shows top labels sorted by confidence score.
 *
 * @param labels Map of label names to confidence scores
 * @param maxLabels Maximum number of labels to display (default: 5)
 */
@Composable
private fun LearnedCategoriesRow(
    labels: Map<String, Float>,
    maxLabels: Int = 5
) {
    val sortedLabels = labels.entries
        .sortedByDescending { it.value }
        .take(maxLabels)

    if (sortedLabels.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Learned categories:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show labels as a flowing text
        val labelsText = sortedLabels.joinToString(", ") { it.key }
        Text(
            text = labelsText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderCardPendingPreview() {
    MaterialTheme {
        FolderCard(
            folder = Folder(
                uri = "content://uri1",
                name = "Vacation2024",
                displayName = "Vacation 2024",
                photoCount = 42,
                learningStatus = LearningStatus.PENDING
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderCardInProgressPreview() {
    MaterialTheme {
        FolderCard(
            folder = Folder(
                uri = "content://uri2",
                name = "Family",
                displayName = "Family Photos",
                photoCount = 156,
                learningStatus = LearningStatus.IN_PROGRESS
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderCardCompletedPreview() {
    MaterialTheme {
        FolderCard(
            folder = Folder(
                uri = "content://uri3",
                name = "Nature",
                displayName = "Nature Photography",
                photoCount = 89,
                learningStatus = LearningStatus.COMPLETED,
                learnedLabels = mapOf(
                    "mountain" to 0.92f,
                    "water" to 0.87f,
                    "sky" to 0.85f,
                    "tree" to 0.78f
                )
            )
        )
    }
}
