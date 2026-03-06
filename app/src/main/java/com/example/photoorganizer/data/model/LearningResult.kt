package com.example.photoorganizer.data.model

/**
 * Represents the result of learning from a folder's sample photos.
 * Aggregates ML labels across multiple photos to understand folder content.
 *
 * @property folderUri The URI of the folder that was learned
 * @property folderName The name of the folder
 * @property sampleCount Number of photos sampled (up to 50)
 * @property aggregatedLabels Map of label text to average confidence score
 * @property topLabels Top 10 labels sorted by frequency and confidence
 * @property completedAt Unix timestamp when learning completed
 */
data class LearningResult(
    val folderUri: String,
    val folderName: String,
    val sampleCount: Int,
    val aggregatedLabels: Map<String, Float>,
    val topLabels: List<String>,
    val completedAt: Long
) {
    /**
     * Returns the top N labels by confidence.
     *
     * @param n Number of labels to return (default 10)
     * @return List of label texts sorted by confidence descending
     */
    fun getTopLabels(n: Int = 10): List<String> {
        return aggregatedLabels
            .entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key }
    }

    /**
     * Converts the aggregated labels to JSON for storage in Room.
     */
    fun toJson(): String {
        if (aggregatedLabels.isEmpty()) return "{}"

        val entries = aggregatedLabels.entries.joinToString(", ") { (key, value) ->
            "\"$key\": $value"
        }
        return "{$entries}"
    }

    companion object {
        /**
         * Parses a JSON string of labels back into a Map.
         *
         * @param json JSON string like {"Food": 0.85, "Document": 0.72}
         * @return Map of label to confidence
         */
        fun fromJson(json: String?): Map<String, Float> {
            if (json.isNullOrBlank() || json == "{}") return emptyMap()

            return try {
                json.removeSurrounding("{", "}")
                    .split(", ")
                    .mapNotNull { pair ->
                        val parts = pair.split(": ")
                        if (parts.size == 2) {
                            val key = parts[0].trim().removeSurrounding("\"")
                            val value = parts[1].toFloatOrNull()
                            if (value != null) key to value else null
                        } else null
                    }
                    .toMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}

/**
 * Represents the status of a folder's learning process.
 */
enum class LearningStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
