package com.example.photoorganizer.data.local.ml

import android.content.Context
import android.net.Uri
import com.example.photoorganizer.data.model.MlLabel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Data source for ML Kit Image Labeling operations.
 *
 * Wraps ML Kit's ImageLabeling API to provide coroutine-friendly,
 * type-safe access to image label detection. Uses a bundled model
 * (adds ~5.7MB to app size) that works offline.
 *
 * Configuration:
 * - Confidence threshold: 0.5f for learning (captures more labels)
 * - Organization threshold: 0.9f (high confidence for actual organization)
 * - Handles corrupted images gracefully
 *
 * @property context Application context for image loading
 */
@Singleton
class MlLabelingDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Image labeler configured for learning phase.
     * Lower threshold (0.5) to capture more potential labels during folder learning.
     */
    private val learningLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(LEARNING_CONFIDENCE_THRESHOLD)
            .build()
        ImageLabeling.getClient(options)
    }

    /**
     * Image labeler configured for organization phase.
     * Higher threshold (0.9) to only organize photos with high confidence.
     */
    private val organizationLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(ORGANIZATION_CONFIDENCE_THRESHOLD)
            .build()
        ImageLabeling.getClient(options)
    }

    /**
     * Analyzes an image and returns detected labels using the learning threshold.
     *
     * This is used during folder learning to collect diverse samples.
     * Uses 0.5f confidence threshold to capture more potential labels.
     *
     * @param uri The image URI to analyze
     * @return Result containing list of labels or error
     */
    suspend fun analyzeImageForLearning(uri: Uri): Result<List<MlLabel>> {
        return analyzeImageInternal(uri, learningLabeler)
    }

    /**
     * Analyzes an image and returns detected labels using the organization threshold.
     *
     * This is used during actual photo organization.
     * Uses 0.9f confidence threshold to only organize high-confidence matches.
     *
     * @param uri The image URI to analyze
     * @return Result containing list of labels or error
     */
    suspend fun analyzeImageForOrganization(uri: Uri): Result<List<MlLabel>> {
        return analyzeImageInternal(uri, organizationLabeler)
    }

    /**
     * Internal method to analyze an image with the specified labeler.
     *
     * @param uri The image URI to analyze
     * @param labeler The configured ML Kit labeler to use
     * @return Result containing list of labels or error
     */
    private suspend fun analyzeImageInternal(
        uri: Uri,
        labeler: com.google.mlkit.vision.label.ImageLabeler
    ): Result<List<MlLabel>> = withContext(Dispatchers.IO) {
        try {
            // Load image using ML Kit's InputImage
            val inputImage = try {
                InputImage.fromFilePath(context, uri)
            } catch (e: IOException) {
                return@withContext Result.failure(
                    MlLabelingException.ImageLoadFailed(uri.toString(), e.message)
                )
            }

            // Process image using ML Kit
            val labels = suspendCancellableCoroutine { continuation ->
                val task = labeler.process(inputImage)
                    .addOnSuccessListener { labels ->
                        val mlLabels = labels.map { label ->
                            MlLabel(
                                text = label.text,
                                confidence = label.confidence,
                                index = label.index
                            )
                        }
                        continuation.resume(mlLabels)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(
                            MlLabelingException.LabelingFailed(uri.toString(), e.message)
                        )
                    }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    task.continueWith { }
                }
            }

            Result.success(labels)
        } catch (e: MlLabelingException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                MlLabelingException.LabelingFailed(uri.toString(), e.message)
            )
        }
    }

    /**
     * Closes the labelers to free resources.
     * Should be called when the data source is no longer needed.
     */
    fun close() {
        learningLabeler.close()
        organizationLabeler.close()
    }

    companion object {
        /**
         * Confidence threshold for learning phase.
         * Lower threshold captures more labels to build a comprehensive
         * understanding of folder contents.
         */
        const val LEARNING_CONFIDENCE_THRESHOLD = 0.5f

        /**
         * Confidence threshold for organization phase.
         * Higher threshold ensures only high-confidence labels trigger
         * photo organization to avoid misclassification.
         */
        const val ORGANIZATION_CONFIDENCE_THRESHOLD = 0.9f
    }
}

/**
 * Sealed class representing ML labeling errors.
 */
sealed class MlLabelingException : Exception() {
    abstract val uri: String

    data class ImageLoadFailed(
        override val uri: String,
        val reason: String?
    ) : MlLabelingException() {
        override val message: String
            get() = "Failed to load image from $uri: $reason"
    }

    data class LabelingFailed(
        override val uri: String,
        val reason: String?
    ) : MlLabelingException() {
        override val message: String
            get() = "ML labeling failed for $uri: $reason"
    }
}
