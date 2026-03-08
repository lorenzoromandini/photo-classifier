/// Result of crash recovery operation
class RecoveryResult {
  final int recovered;
  final int failed;
  final List<String> errors;
  final bool success;

  RecoveryResult({
    required this.recovered,
    required this.failed,
    required this.errors,
    required this.success,
  });

  /// Create a successful recovery result
  factory RecoveryResult.success({required int recovered, int failed = 0}) {
    return RecoveryResult(
      recovered: recovered,
      failed: failed,
      errors: [],
      success: failed == 0,
    );
  }

  /// Create a failed recovery result
  factory RecoveryResult.failure({required List<String> errors}) {
    return RecoveryResult(
      recovered: 0,
      failed: errors.length,
      errors: errors,
      success: false,
    );
  }

  @override
  String toString() {
    return 'RecoveryResult(recovered: $recovered, failed: $failed, success: $success)';
  }
}
