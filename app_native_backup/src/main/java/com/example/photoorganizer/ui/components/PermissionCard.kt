package com.example.photoorganizer.ui.components

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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Reusable card component for requesting permissions with context.
 *
 * Displays a card with an icon, title, description explaining WHY the permission
 * is needed, and a primary action button to grant the permission.
 *
 * @param title The title of the permission card
 * @param description Explanation of why the permission is needed
 * @param icon The icon to display (defaults to FolderOpen)
 * @param buttonText Text for the grant button
 * @param onGrantClick Callback when the grant button is clicked
 * @param modifier Modifier for the card
 * @param isEnabled Whether the button is enabled
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.FolderOpen,
    buttonText: String = "Grant Access",
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description explaining WHY permission is needed
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grant button
            Button(
                onClick = onGrantClick,
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = buttonText)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardPreview() {
    MaterialTheme {
        PermissionCard(
            title = "Access Your Photos",
            description = "Photo Organizer needs access to your Pictures folder to automatically organize your photos. Your files stay on your device - we never upload them.",
            onGrantClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionCardDisabledPreview() {
    MaterialTheme {
        PermissionCard(
            title = "Access Your Photos",
            description = "Photo Organizer needs access to your Pictures folder to automatically organize your photos.",
            onGrantClick = {},
            isEnabled = false,
            buttonText = "Processing..."
        )
    }
}