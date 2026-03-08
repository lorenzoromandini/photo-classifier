import 'package:flutter/services.dart';
import '../../domain/models/folder_model.dart';
import '../../domain/models/photo_model.dart';
import 'result.dart';

/// Service for Storage Access Framework (SAF) operations
class SafService {
  static const MethodChannel _channel =
      MethodChannel('com.photo_classifier/platform');

  /// Pick a folder using SAF picker
  /// Returns the tree URI string or null if cancelled
  Future<Result<String>> pickFolder() async {
    try {
      final uri = await _channel.invokeMethod<String>('pickFolder');
      if (uri == null) {
        return Result.failure('Folder selection cancelled');
      }
      return Result.success(uri);
    } on PlatformException catch (e) {
      return Result.failure('Failed to pick folder: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }

  /// Discover folders in the given tree URI
  /// Returns a list of FolderModel objects
  Future<Result<List<FolderModel>>> discoverFolders(String uri) async {
    try {
      final foldersList = await _channel.invokeMethod<List<dynamic>>(
        'discoverFolders',
        {'uri': uri},
      );

      final folders = foldersList
          .whereType<Map<dynamic, dynamic>>()
          .map((map) => FolderModel.fromMap(
                Map<String, dynamic>.from(
                  map.map((k, v) => MapEntry(k.toString(), v)),
                ),
              ))
          .toList();

      return Result.success(folders);
    } on PlatformException catch (e) {
      return Result.failure('Failed to discover folders: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }

  /// Persist read/write permission for the given URI
  Future<Result<void>> persistPermission(String uri) async {
    try {
      await _channel.invokeMethod<bool>(
        'persistPermission',
        {'uri': uri},
      );
      return Result.success(null);
    } on PlatformException catch (e) {
      return Result.failure('Failed to persist permission: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }

  /// Check if we have persistable permission for the given URI
  Future<Result<bool>> hasPermission(String uri) async {
    try {
      final hasPermission = await _channel.invokeMethod<bool>(
        'hasPermission',
        {'uri': uri},
      );
      return Result.success(hasPermission ?? false);
    } on PlatformException catch (e) {
      return Result.failure('Failed to check permission: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }

  /// List photos in the given folder
  Future<Result<List<PhotoModel>>> listPhotos(String folderUri) async {
    try {
      final photosList = await _channel.invokeMethod<List<dynamic>>(
        'listPhotos',
        {'uri': folderUri},
      );

      final photos = photosList
          .whereType<Map<dynamic, dynamic>>()
          .map((map) {
            final data = Map<String, dynamic>.from(
              map.map((k, v) => MapEntry(k.toString(), v)),
            );
            // Convert from native format to domain model
            return PhotoModel(
              uri: data['uri'] as String,
              folderUri: folderUri,
              fileName: data['fileName'] as String,
              fileSize: (data['fileSize'] as num).toInt(),
              status: 'pending',
            );
          })
          .toList();

      return Result.success(photos);
    } on PlatformException catch (e) {
      return Result.failure('Failed to list photos: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }

  /// Count photos in the given folder
  Future<Result<int>> countPhotos(String uri) async {
    try {
      final count = await _channel.invokeMethod<int>(
        'countPhotos',
        {'uri': uri},
      );
      return Result.success(count ?? 0);
    } on PlatformException catch (e) {
      return Result.failure('Failed to count photos: ${e.message}');
    } catch (e) {
      return Result.failure('Unexpected error: $e');
    }
  }
}
