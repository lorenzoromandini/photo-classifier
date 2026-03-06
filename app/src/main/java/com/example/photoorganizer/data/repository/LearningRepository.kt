package com.example.photoorganizer.data.repository

import android.net.Uri
import com.example.photoorganizer.data.local.database.dao.FolderDao
import com.example.photoorganizer.data.local.database.entities.FolderEntity
import com.example.photoorganizer.data.local.database.entities.LearningStatus
import com.example.photoorganizer.data.local.ml.MlLabelingDataSource
import com.example.photoorganizer.data.local.saf.SafDataSource
import com.example.photoorganizer.data.model.LearningResult
import com.example.photoorganizer.data.model.MlLabelingDataSource.Companion.LEARNING_CONFIDENCE_THRESHOLD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing folder learning using ML Kit Image Labeling.
 *
 * Learns from sample photos in each folder to understand what type of
 * content the folder contains. Uses a combined approach:
 * 1. Sample up to 50 photos per folder
 * 2. Analyze each photo with ML Kit Image Labeling
 * 3. Aggregate labels across all samples
 * 4. Boost labels that match folder name
 * 5. Store top 10 labels for future classification
 *
 * @property folderDao DAO for folder database operations
 * @property mlLabelingDataSource Data source for ML Kit image labeling
 * @property safDataSource Data source for SAF photo access
 */
@Singleton
class LearningRepository @Inject constructor(
    private val folderDao: FolderDao,
    private val mlLabelingDataSource: MlLabelingDataSource,
    private val safDataSource: SafDataSource
) {

    /**
     * Gets all folders with PENDING learning status.
     * These folders need to be learned before they can be used for organization.
     */
    suspend fun getPendingFolders(): List<FolderEntity> {
        return withContext(Dispatchers.IO) {
            folderDao.getPendingFolders()
        }
    }

    /**
     * Gets learning progress as a reactive Flow.
     * Emits whenever folder learning status changes.
     */
    val learningProgress: Flow<LearningProgress> = folderDao.getAll()
        .map { folders ->
            val total = folders.size
            val completed = folders.count { 
                it.learningStatus == LearningStatus.COMPLETED.name 
            }
            val pending = folders.count { 
                it.learningStatus == LearningStatus.PENDING.name 
            }
            val inProgress = folders.count { 
                it.learningStatus == LearningStatus.IN_PROGRESS.name 
            }
            LearningProgress(total, completed, pending, inProgress)
        }

    /**
     * Starts the learning process for a specific folder.
     *
     * This method:
     * 1. Updates folder status to IN_PROGRESS
     * 2. Samples up to 50 photos from the folder
     * 3. Analyzes each photo with ML Kit Image Labeling
     * 4. Aggregates labels across all samples
     * 5. Boosts folder-name-matching labels
     * 6. Stores results and updates status to COMPLETED
     *
     * @param folderUri The URI of the folder to learn from
     * @return Result containing LearningResult on success, or error
     */
    suspend fun startLearning(folderUri: String): Result<LearningResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Update status to IN_PROGRESS
                folderDao.updateLearningStatus(folderUri, LearningStatus.IN_PROGRESS.name)

                // 2. Get folder entity for metadata
                val folder = folderDao.getByUri(folderUri)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Folder not found: $folderUri")
                    )

                // 3. Sample up to 50 photos
                val photos = samplePhotos(folderUri)
                val sampleCount = photos.size

                if (sampleCount == 0) {
                    // No photos to learn from, mark as completed with empty results
                    val emptyResult = LearningResult(
                        folderUri = folderUri,
                        folderName = folder.name,
                        sampleCount = 0,
                        aggregatedLabels = emptyMap(),
                        topLabels = emptyList(),
                        completedAt = System.currentTimeMillis()
                    )
                    saveLearningResult(emptyResult)
                    return@withContext Result.success(emptyResult)
                }

                // 4. Analyze each photo and collect labels
                val allLabels = mutableListOf<String>()
                val labelConfidences = mutableMapOf<String, MutableList<Float>>()

                photos.forEach { photoUri ->
                    mlLabelingDataSource.analyzeImageForLearning(photoUri)
                        .onSuccess { labels ->
                            labels.forEach { label ->
                                allLabels.add(label.text)
                                labelConfidences.getOrPut(label.text) { mutableListOf() }
                                    .add(label.confidence)
                            }
                        }
                        .onFailure { /* Skip failed images, continue with others */ }
                }

                // 5. Aggregate labels
                val aggregatedLabels = aggregateLabels(allLabels, labelConfidences)

                // 6. Boost folder-name-matching labels
                val boostedLabels = boostFolderNameMatches(aggregatedLabels, folder.name)

                // 7. Get top 10 labels
                val topLabels = boostedLabels
                    .entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .map { it.key }

                // 8. Create and save result
                val learningResult = LearningResult(
                    folderUri = folderUri,
                    folderName = folder.name,
                    sampleCount = sampleCount,
                    aggregatedLabels = boostedLabels,
                    topLabels = topLabels,
                    completedAt = System.currentTimeMillis()
                )

                saveLearningResult(learningResult)
                Result.success(learningResult)

            } catch (e: Exception) {
                // Mark as pending on failure (can retry later)
                folderDao.updateLearningStatus(folderUri, LearningStatus.PENDING.name)
                Result.failure(e)
            }
        }
    }

    /**
     * Saves a learning result to the database.
     *
     * @param result The learning result to save
     */
    suspend fun saveLearningResult(result: LearningResult) {
        withContext(Dispatchers.IO) {
            // Save labels as JSON
            val labelsJson = result.toJson()
            folderDao.updateLearnedLabels(result.folderUri, labelsJson)

            // Mark as completed
            folderDao.updateLearningStatus(result.folderUri, LearningStatus.COMPLETED.name)
        }
    }

    /**
     * Updates the learning status for a folder.
     *
     * @param uri The folder URI
     * @param status The new learning status
     */
    suspend fun updateLearningStatus(uri: String, status: LearningStatus) {
        withContext(Dispatchers.IO) {
            folderDao.updateLearningStatus(uri, status.name)
        }
    }

    /**
     * Samples up to 50 photos from a folder.
     *
     * @param folderUri The folder URI
     * @return List of photo URIs (up to 50, randomly selected)
     */
    private suspend fun samplePhotos(folderUri: String): List<Uri> {
        return withContext(Dispatchers.IO) {
            val uri = Uri.parse(folderUri)
            val allPhotos = safDataSource.listPhotos(uri)

            // Shuffle and take up to 50
            allPhotos
                .shuffled()
                .take(MAX_SAMPLE_SIZE)
                .map { it.uri }
        }
    }

    /**
     * Aggregates labels across all samples.
     * Calculates average confidence for each label.
     *
     * @param allLabels All labels found across samples
     * @param labelConfidences Map of label to list of confidence scores
     * @return Map of label to average confidence
     */
    private fun aggregateLabels(
        allLabels: List<String>,
        labelConfidences: Map<String, List<Float>>
    ): Map<String, Float> {
        // Count frequency of each label
        val labelCounts = allLabels.groupingBy { it }.eachCount()

        // Calculate average confidence for each label
        return labelConfidences.map { (label, confidences) ->
            val avgConfidence = confidences.average().toFloat()
            val frequency = labelCounts[label] ?: 0
            // Combine frequency and confidence (both normalized)
            val frequencyScore = frequency.toFloat() / allLabels.size.coerceAtLeast(1)
            val combinedScore = (avgConfidence * 0.7f) + (frequencyScore * 0.3f)
            label to combinedScore
        }.toMap()
    }

    /**
     * Boosts labels that match the folder name.
     * If a label text appears in the folder name, its confidence is boosted.
     *
     * @param labels Current label scores
     * @param folderName Folder name to match against
     * @return Updated label scores with boosts applied
     */
    private fun boostFolderNameMatches(
        labels: Map<String, Float>,
        folderName: String
    ): Map<String, Float> {
        val normalizedFolderName = folderName.lowercase()
        val wordsInFolderName = normalizedFolderName
            .split(" ", "_", "-", ".")
            .filter { it.length > 2 }
            .toSet()

        return labels.map { (label, confidence) ->
            val normalizedLabel = label.lowercase()
            val boost = when {
                // Exact match with folder name
                normalizedFolderName.contains(normalizedLabel) -> FOLDER_NAME_EXACT_BOOST
                // Label word matches folder word
                wordsInFolderName.any { normalizedLabel.contains(it) } -> FOLDER_NAME_PARTIAL_BOOST
                else -> 0f
            }
            label to (confidence + boost).coerceAtMost(1.0f)
        }.toMap()
    }

    /**
     * Data class representing learning progress across all folders.
     */
    data class LearningProgress(
        val total: Int,
        val completed: Int,
        val pending: Int,
        val inProgress: Int
    ) {
        /**
         * Returns the percentage of folders that have been learned (0-100).
         */
        val percentComplete: Int
            get() = if (total > 0) (completed * 100) / total else 0

        /**
         * Returns true if all folders have been learned.
         */
        val isComplete: Boolean
            get() = completed == total && total > 0
    }

    companion object {
        /**
         * Maximum number of photos to sample for learning.
         * User decision: sample 50 photos per folder for analysis.
         */
        const val MAX_SAMPLE_SIZE = 50

        /**
         * Boost amount for exact folder name match.
         * e.g., folder "Vacation" with label "Vacation" gets this boost.
         */
        const val FOLDER_NAME_EXACT_BOOST = 0.15f

        /**
         * Boost amount for partial folder name match.
         * e.g., folder "Dog Photos" with label "Dog" gets this boost.
         */
        const val FOLDER_NAME_PARTIAL_BOOST = 0.08f
    }
}
