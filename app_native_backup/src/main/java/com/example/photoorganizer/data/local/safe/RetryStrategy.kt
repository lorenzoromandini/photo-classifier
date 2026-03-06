package com.example.photoorganizer.data.local.safe

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry strategy with exponential backoff for transient failures.
 *
 * Configuration:
 * - maxRetries: Maximum number of retry attempts (default: 5)
 * - initialDelayMs: Initial delay before first retry (default: 1000ms)
 * - maxDelayMs: Maximum delay between retries (default: 300000ms = 5 minutes)
 *
 * Delay calculation: initialDelayMs * 2^attempt, capped at maxDelayMs
 */
data class RetryConfig(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS
) {
    companion object {
        const val DEFAULT_MAX_RETRIES = 5
        const val DEFAULT_INITIAL_DELAY_MS = 1000L
        const val DEFAULT_MAX_DELAY_MS = 300000L // 5 minutes

        /**
         * Conservative config for critical operations
         */
        val CONSERVATIVE = RetryConfig(
            maxRetries = 3,
            initialDelayMs = 2000L,
            maxDelayMs = 60000L // 1 minute
        )

        /**
         * Aggressive config for quick recovery
         */
        val AGGRESSIVE = RetryConfig(
            maxRetries = 10,
            initialDelayMs = 500L,
            maxDelayMs = 60000L // 1 minute
        )
    }
}

/**
 * Retry strategy implementation with exponential backoff.
 */
class RetryStrategy(private val config: RetryConfig = RetryConfig()) {

    /**
     * Calculate the delay for a specific retry attempt.
     *
     * Uses exponential backoff: initialDelayMs * 2^attempt, capped at maxDelayMs
     *
     * @param attempt The attempt number (0-based)
     * @return Delay in milliseconds
     */
    fun calculateDelay(attempt: Int): Long {
        val delay = config.initialDelayMs * 2.0.pow(attempt.toDouble()).toLong()
        return min(delay, config.maxDelayMs)
    }

    /**
     * Execute an operation with retry logic.
     *
     * @param operation The operation to execute
     * @return Result containing the operation result or failure
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Throwable? = null

        for (attempt in 0..config.maxRetries) {
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e

                // Don't delay on last attempt
                if (attempt < config.maxRetries) {
                    val delayMs = calculateDelay(attempt)
                    delay(delayMs)
                }
            }
        }

        return Result.failure(
            lastException ?: RuntimeException("Operation failed after ${config.maxRetries} retries")
        )
    }

    /**
     * Execute an operation with retry and custom error handling.
     *
     * @param operation The operation to execute
     * @param onRetry Called before each retry attempt (after first failure)
     * @return Result containing the operation result or failure
     */
    suspend fun <T> executeWithRetry(
        operation: suspend () -> T,
        onRetry: (attempt: Int, exception: Throwable, nextDelayMs: Long) -> Unit
    ): Result<T> {
        var lastException: Throwable? = null

        for (attempt in 0..config.maxRetries) {
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e

                // Don't delay on last attempt
                if (attempt < config.maxRetries) {
                    val delayMs = calculateDelay(attempt)
                    onRetry(attempt, e, delayMs)
                    delay(delayMs)
                }
            }
        }

        return Result.failure(
            lastException ?: RuntimeException("Operation failed after ${config.maxRetries} retries")
        )
    }

    /**
     * Execute an operation with retry predicate.
     * Only retries if the predicate returns true for the exception.
     *
     * @param shouldRetry Predicate to determine if exception is retryable
     * @param operation The operation to execute
     * @return Result containing the operation result or failure
     */
    suspend fun <T> executeWithRetryIf(
        shouldRetry: (Throwable) -> Boolean,
        operation: suspend () -> T
    ): Result<T> {
        var lastException: Throwable? = null

        for (attempt in 0..config.maxRetries) {
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e

                // Check if we should retry this exception
                if (!shouldRetry(e) || attempt >= config.maxRetries) {
                    return Result.failure(e)
                }

                val delayMs = calculateDelay(attempt)
                delay(delayMs)
            }
        }

        return Result.failure(
            lastException ?: RuntimeException("Operation failed after ${config.maxRetries} retries")
        )
    }

    /**
     * Get the maximum delay for display/logging purposes.
     *
     * @return Max delay in milliseconds
     */
    fun getMaxDelayMs(): Long = config.maxDelayMs

    /**
     * Get the max retries count.
     *
     * @return Max retries
     */
    fun getMaxRetries(): Int = config.maxRetries
}

/**
 * Extension function to check if an exception is retryable.
 * IOExceptions and transient errors are typically retryable.
 */
fun Throwable.isRetryable(): Boolean {
    return when (this) {
        is java.io.IOException -> true
        is java.net.SocketTimeoutException -> true
        is java.net.UnknownHostException -> true
        else -> false
    }
}
