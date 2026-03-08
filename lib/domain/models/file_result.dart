/// Result wrapper for file operations with type-safe error handling
class FileResult<T> {
  final bool success;
  final T? data;
  final String? error;
  final String? errorCode;

  bool get isSuccess => success;
  bool get isFailure => !success;

  FileResult._({
    required this.success,
    this.data,
    this.error,
    this.errorCode,
  });

  /// Create a successful result
  factory FileResult.success(T data) {
    return FileResult._(success: true, data: data);
  }

  /// Create a failed result
  factory FileResult.failure(String error, {String? code}) {
    return FileResult._(success: false, error: error, errorCode: code);
  }

  /// Create from raw map (from platform channel)
  factory FileResult.fromMap(Map<dynamic, dynamic> map, {String? defaultCode}) {
    final success = map['success'] as bool? ?? false;
    final data = map['data'] as T?;
    final error = map['error'] as String?;
    final errorCode = map['errorCode'] as String? ?? defaultCode;

    return FileResult._(
      success: success,
      data: data,
      error: error,
      errorCode: errorCode,
    );
  }

  @override
  String toString() {
    if (isSuccess) {
      return 'FileResult.success($data)';
    } else {
      return 'FileResult.failure($error, code: $errorCode)';
    }
  }
}

/// Error codes for file operations
class FileErrorCodes {
  static const String storageFull = 'STORAGE_FULL';
  static const String permissionDenied = 'PERMISSION_DENIED';
  static const String fileNotFound = 'FILE_NOT_FOUND';
  static const String ioError = 'IO_ERROR';
  static const String verificationFailed = 'VERIFICATION_FAILED';
}
