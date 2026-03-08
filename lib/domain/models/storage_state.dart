/// Storage state enumeration
enum StorageState {
  ok,
  low,
  critical,
}

/// Storage information with available/total bytes and state
class StorageInfo {
  final int availableBytes;
  final int totalBytes;
  final StorageState state;

  StorageInfo({
    required this.availableBytes,
    required this.totalBytes,
    required this.state,
  });

  /// Check if there's at least 100MB available (with safety buffer)
  bool get hasSpaceFor100MB => availableBytes > 100 * 1024 * 1024;

  /// Check if there's enough space for a given file size
  bool hasSpaceFor(int fileSizeBytes, {int bufferBytes = 100 * 1024 * 1024}) {
    return availableBytes > (fileSizeBytes + bufferBytes);
  }

  @override
  String toString() {
    return 'StorageInfo(available: ${availableBytes ~/ 1024 ~/ 1024}MB, total: ${totalBytes ~/ 1024 ~/ 1024}MB, state: $state)';
  }
}

/// Helper to determine storage state based on available bytes
class StorageThresholds {
  static const int criticalThreshold = 50 * 1024 * 1024; // 50MB
  static const int lowThreshold = 500 * 1024 * 1024; // 500MB
  static const int minBuffer = 100 * 1024 * 1024; // 100MB

  static StorageState determineState(int availableBytes) {
    if (availableBytes < criticalThreshold) {
      return StorageState.critical;
    } else if (availableBytes < lowThreshold) {
      return StorageState.low;
    }
    return StorageState.ok;
  }
}
