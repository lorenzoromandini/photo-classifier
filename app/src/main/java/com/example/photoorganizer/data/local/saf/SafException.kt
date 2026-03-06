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

    /**
     * Thrown when permission cannot be persisted for a URI.
     */
    data class PermissionPersistenceException(
        override val message: String = "Failed to persist permission"
    ) : SafException()

    /**
     * Thrown when folder discovery fails.
     */
    data class DiscoveryException(
        override val message: String = "Failed to discover folders"
    ) : SafException()

    /**
     * User-friendly message for this exception.
     * Can be displayed directly in UI.
     */
    val userMessage: String
        get() = when (this) {
            is InvalidUriException -> "The selected folder is not valid. Please try again."
            is PermissionDeniedException -> "Permission denied. Please allow access to your photos."
            is PermissionPersistenceException -> "Could not save your permission settings. Please try again."
            is FolderNotFoundException -> "Could not find the selected folder. Please select another folder."
            is DiscoveryException -> "Could not scan your photos. Please check your folder and try again."
            is ContentResolutionException -> "Could not read photo information. Please try again."
        }
}