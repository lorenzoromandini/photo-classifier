package com.example.photoorganizer.data.local.saf

/**
 * Sealed class representing SAF-related exceptions.
 * Provides type-safe error handling for Storage Access Framework operations.
 */
sealed class SafException : Exception() {

    /**
     * Thrown when an invalid or malformed URI is encountered.
     */
    data class InvalidUriException(
        val uri: String,
        override val message: String = "Invalid SAF URI: $uri"
    ) : SafException()

    /**
     * Thrown when permission is denied for a SAF operation.
     * Usually indicates the user revoked permissions or the URI is no longer valid.
     */
    data class PermissionDeniedException(
        val uri: String,
        override val message: String = "Permission denied for URI: $uri"
    ) : SafException()

    /**
     * Thrown when a folder cannot be found at the specified path.
     */
    data class FolderNotFoundException(
        val path: String,
        override val message: String = "Folder not found: $path"
    ) : SafException()

    /**
     * Thrown when SAF content resolution fails.
     */
    data class ContentResolutionException(
        val uri: String,
        override val cause: Throwable? = null,
        override val message: String = "Failed to resolve content for URI: $uri"
    ) : SafException()
}