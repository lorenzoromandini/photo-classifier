package com.example.photoorganizer.data.model

/**
 * Represents a label detected by ML Kit Image Labeling.
 *
 * @property text The label text (e.g., "Food", "Document", "People")
 * @property confidence Confidence score from 0.0 to 1.0
 * @property index The label index in the ML Kit model
 */
data class MlLabel(
    val text: String,
    val confidence: Float,
    val index: Int
) {
    /**
     * Returns true if confidence meets the threshold for learning.
     * Learning threshold is lower than organization threshold to capture more data.
     */
    fun isAboveLearningThreshold(): Boolean = confidence >= LEARNING_THRESHOLD

    companion object {
        /**
         * Threshold for learning phase (lower than organization threshold).
         * Allows capturing borderline labels during folder learning.
         */
        const val LEARNING_THRESHOLD = 0.5f

        /**
         * Threshold for organization phase (high confidence required).
         */
        const val ORGANIZATION_THRESHOLD = 0.9f
    }
}
