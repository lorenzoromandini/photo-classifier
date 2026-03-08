import 'package:flutter/services.dart';
import '../domain/models/file_result.dart';

/// Platform channel service for file operations via SAF
class FileOperationService {
  static const MethodChannel _channel =
      MethodChannel('com.photo_classifier/platform');

  /// Copy file from source to destination via SAF
  /// Returns FileResult with bytes copied on success
  Future<FileResult<int>> copyFile(String sourceUri, String destUri) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'copyFile',
        {
          'sourceUri': sourceUri,
          'destUri': destUri,
        },
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<int>(result, defaultCode: FileErrorCodes.ioError);
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error during copy',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }

  /// Verify file copy by comparing sizes and checksums
  /// Returns FileResult with boolean indicating if files match
  Future<FileResult<bool>> verifyFile(String sourceUri, String destUri) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'verifyFile',
        {
          'sourceUri': sourceUri,
          'destUri': destUri,
        },
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<bool>(
        result,
        defaultCode: FileErrorCodes.verificationFailed,
      );
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error during verification',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.verificationFailed,
      );
    }
  }

  /// Delete file via SAF
  Future<FileResult<void>> deleteFile(String uri) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'deleteFile',
        {
          'uri': uri,
        },
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<void>(
        result,
        defaultCode: FileErrorCodes.ioError,
      );
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error during delete',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }

  /// Get file size in bytes
  Future<FileResult<int>> getFileSize(String uri) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'getFileSize',
        {
          'uri': uri,
        },
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<int>(result, defaultCode: FileErrorCodes.fileNotFound);
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error getting file size',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }

  /// Get available storage in bytes
  Future<FileResult<int>> getAvailableStorage() async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'getAvailableStorage',
        {},
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<int>(result, defaultCode: FileErrorCodes.ioError);
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error getting storage',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }

  /// Check if file exists
  Future<FileResult<bool>> exists(String uri) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        'exists',
        {
          'uri': uri,
        },
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<bool>(result, defaultCode: FileErrorCodes.fileNotFound);
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error checking existence',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }

  /// Invoke a platform method directly (for custom operations)
  Future<FileResult<Map>> invokePlatformMethod(
    String method,
    Map<String, dynamic> arguments,
  ) async {
    try {
      final result = await _channel.invokeMethod<Map<dynamic, dynamic>>(
        method,
        arguments,
      );

      if (result == null) {
        return FileResult.failure('Null result from platform channel');
      }

      return FileResult.fromMap<Map>(result, defaultCode: FileErrorCodes.ioError);
    } on PlatformException catch (e) {
      return FileResult.failure(
        e.message ?? 'Platform error invoking $method',
        code: e.code,
      );
    } catch (e) {
      return FileResult.failure(
        e.toString(),
        code: FileErrorCodes.ioError,
      );
    }
  }
}
