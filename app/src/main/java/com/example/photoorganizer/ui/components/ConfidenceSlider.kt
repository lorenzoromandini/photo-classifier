package com.example.photoorganizer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Confidence threshold selector with three preset levels.
 *
 * User decision: Low (0.6), Medium (0.75), High (0.9 - default)
 * Higher threshold means more conservative organization.
 *
 * @param currentValue Current threshold value (0.0 - 1.0)
 * @param onValueChange Callback when threshold changes
 * @param modifier Modifier for styling
 */
@Composable
fun ConfidenceSlider(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Organization Confidence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = "Higher confidence means fewer mistakes but more photos left unorganized",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preset buttons using SegmentedButtonRow
            ConfidenceLevelButtons(
                currentValue = currentValue,
                onValueChange = onValueChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selected level description
            ConfidenceLevelDescription(currentValue = currentValue)
        }
    }
}

/**
 * Segmented button row for confidence level selection.
 */
@Composable
private fun ConfidenceLevelButtons(
    currentValue: Float,
    onValueChange: (Float) -> Unit
) {
    val options = listOf(
        ConfidenceLevel.LOW,
        ConfidenceLevel.MEDIUM,
        ConfidenceLevel.HIGH
    )

    // Find current selection
    val selectedIndex = options.indexOfFirst { it.value == currentValue }

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, level ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onValueChange(level.value) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                )
            ) {
                Text(level.label)
            }
        }
    }
}

/**
 * Description text for the currently selected confidence level.
 */
@Composable
private fun ConfidenceLevelDescription(currentValue: Float) {
    val level = when (currentValue) {
        ConfidenceLevel.LOW.value -> ConfidenceLevel.LOW
        ConfidenceLevel.MEDIUM.value -> ConfidenceLevel.MEDIUM
        ConfidenceLevel.HIGH.value -> ConfidenceLevel.HIGH
        else -> ConfidenceLevel.HIGH // Default
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (level) {
                ConfidenceLevel.LOW -> MaterialTheme.colorScheme.secondaryContainer
                ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.primaryContainer
                ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when (level) {
                    ConfidenceLevel.LOW -> MaterialTheme.colorScheme.secondary
                    ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                    ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.tertiary
                }
            )

            Column {
                Text(
                    text = "${level.label} (${(level.value * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (level) {
                        ConfidenceLevel.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
                        ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.onPrimaryContainer
                        ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )

                Text(
                    text = level.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (level) {
                        ConfidenceLevel.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
                        ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.onPrimaryContainer
                        ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }
        }
    }
}

/**
 * Confidence level configuration.
 */
private enum class ConfidenceLevel(
    val value: Float,
    val label: String,
    val description: String
) {
    LOW(
        value = 0.6f,
        label = "Low",
        description = "More photos organized, may include some mistakes"
    ),
    MEDIUM(
        value = 0.75f,
        label = "Medium",
        description = "Balanced approach - good accuracy with reasonable coverage"
    ),
    HIGH(
        value = 0.9f,
        label = "High",
        description = "Conservative - fewer mistakes, more photos left in place"
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfidenceSliderLowPreview() {
    MaterialTheme {
        ConfidenceSlider(
            currentValue = 0.6f,
            onValueChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfidenceSliderMediumPreview() {
    MaterialTheme {
        ConfidenceSlider(
            currentValue = 0.75f,
            onValueChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfidenceSliderHighPreview() {
    MaterialTheme {
        ConfidenceSlider(
            currentValue = 0.9f,
            onValueChange = {}
        )
    }
}
