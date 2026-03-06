package com.example.photoorganizer.data.local.safe

import android.net.Uri

/**
 * Sealed class representing the result of a file verification operation.
 * Used by SafeFileOperations to verify copied files match their sources.
 */
sealed class FileVerificationResult {
    /**
     * Verification succeeded - source and destination sizes match
     */
    data class Success(
        val sourceSize: Long,
        val destSize: Long
    ) : FileVerificationResult()

    /**
     * Size mismatch detected between source and destination
     */
    data class SizeMismatch(
        val sourceSize: Long,
        val destSize: Long
    ) : FileVerificationResult()

    /**
     * Checksum/SHA-256 mismatch detected between source and destination
     */
    data class ChecksumMismatch(
        val sourceHash: String,
        val destHash: String
    ) : FileVerificationResult()

    /**
     * Destination file was not found at the specified URI
     */
    data class DestNotFound(
        val uri: Uri
    ) : FileVerificationResult()

    /**
     * Verification could not be completed due to an error
     */
    data class Error(
        val exception: Throwable
    ) : FileVerificationResult()

    /**
     * Returns true if verification was successful
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if verification failed
     */
    val isFailure: Boolean
        get() = !isSuccess
}
