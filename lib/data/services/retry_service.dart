import 'dart:math';

/// Configuration for retry behavior
class RetryConfig {
  final int maxRetries;
  final Duration initialDelay;
  final Duration maxDelay;

  const RetryConfig({
    this.maxRetries = 5,
    this.initialDelay = const Duration(seconds: 1),
    this.maxDelay = const Duration(minutes: 5),
  });

  /// Default configuration for standard operations
  static const defaultConfig = RetryConfig();

  /// Conservative configuration - fewer retries, shorter delays
  static const conservativeConfig = RetryConfig(
    maxRetries: 3,
    initialDelay: Duration(milliseconds: 500),
    maxDelay: Duration(minutes: 1),
  );

  /// Aggressive configuration - more retries, longer delays
  static const aggressiveConfig = RetryConfig(
    maxRetries: 10,
    initialDelay: Duration(seconds: 2),
    maxDelay: Duration(minutes: 10),
  );
}

/// Service implementing retry logic with exponential backoff
class RetryService {
  /// Execute an operation with retry logic
  /// Returns the result on success, throws after max retries exceeded
  Future<T> executeWithRetry<T>(
    Future<T> Function() operation, {
    RetryConfig? config,
    void Function(int attempt, Duration delay)? onRetry,
  }) async {
    final retryConfig = config ?? RetryConfig.defaultConfig;

    for (int attempt = 0; attempt <= retryConfig.maxRetries; attempt++) {
      try {
        return await operation();
      } catch (e) {
        if (attempt == retryConfig.maxRetries) {
          // Last attempt failed, rethrow
          rethrow;
        }

        final delay = calculateDelay(attempt, retryConfig);
        onRetry?.call(attempt + 1, delay);

        await Future.delayed(delay);
      }
    }

    throw StateError('Unreachable - should have returned or thrown');
  }

  /// Calculate delay using exponential backoff with jitter
  Duration calculateDelay(int attempt, RetryConfig config) {
    // Exponential backoff: initialDelay * 2^attempt
    final exponentialDelay = config.initialDelay.inMilliseconds * pow(2, attempt).toInt();
    
    // Cap at maxDelay
    final cappedDelay = min(exponentialDelay, config.maxDelay.inMilliseconds);
    
    // Add jitter (±25%)
    final jitter = (cappedDelay * 0.25 * (1 - random.nextDouble() * 2)).toInt();
    final finalDelay = cappedDelay + jitter;

    return Duration(milliseconds: max(finalDelay, 0));
  }

  final Random random = Random();

  /// Check if an error is retryable
  static bool isRetryableError(dynamic error, String? errorCode) {
    // Retry on transient errors
    if (errorCode == 'STORAGE_FULL') return false; // Storage full won't fix itself
    if (errorCode == 'PERMISSION_DENIED') return false; // Permission issues need user action
    
    // Network/IO errors are usually transient
    if (error is Exception || error is Error) return true;
    
    return true; // Default to retryable
  }
}
