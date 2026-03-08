import 'package:flutter/foundation.dart';
import '../database/database_service.dart';
import '../platform/saf_service.dart';
import '../platform/result.dart';
import '../../domain/models/folder_model.dart';
import '../../domain/models/photo_model.dart';

/// Offline-first folder repository
/// Database is source of truth, SAF is data source
class FolderRepository {
  final DatabaseService databaseService;
  final SafService safService;

  FolderRepository({
    required this.databaseService,
    required this.safService,
  });

  /// Get folders as a reactive stream from database (source of truth)
  Future<List<FolderModel>> getFolders() async {
    return await databaseService.getFolders();
  }

  /// Discover folders via SAF and sync to database
  /// This is the offline-first sync operation:
  /// 1. Call SAF discovery
  /// 2. On success: delete existing folders, insert new ones
  /// 3. Update UI via database stream
  Future<Result<void>> discoverAndSyncFolders(String baseUri) async {
    try {
      // Step 1: Discover folders from SAF
      final discoverResult = await safService.discoverFolders(baseUri);
      
      if (discoverResult.isFailure) {
        return Result.failure(discoverResult.error ?? 'Unknown error');
      }

      final folders = discoverResult.data ?? [];
      debugPrint('Discovered ${folders.length} folders via SAF');

      // Step 2: Sync to database (delete old, insert new)
      final db = await databaseService.database;
      
      // Begin transaction for atomicity
      await db.transaction((txn) async {
        // Delete existing folders for this base URI
        // For now, we clear all and re-insert (simple approach)
        // In production, you might want to track which folders are from which base
        await txn.delete('folders');
        
        // Insert newly discovered folders
        for (final folder in folders) {
          await txn.insert(
            'folders',
            folder.toMap(),
            conflictAlgorithm: ConflictAlgorithm.replace,
          );
        }
      });

      debugPrint('Synced ${folders.length} folders to database');
      return Result.success(null);
    } catch (e) {
      debugPrint('Error syncing folders: $e');
      return Result.failure('Failed to sync folders: $e');
    }
  }

  /// Persist permission for a folder URI
  Future<Result<void>> persistFolderPermission(String uri) async {
    try {
      // First check if we already have permission
      final hasPermResult = await safService.hasPermission(uri);
      if (hasPermResult.isSuccess && (hasPermResult.data ?? false)) {
        debugPrint('Already have permission for $uri');
        return Result.success(null);
      }

      // Persist the permission
      final result = await safService.persistPermission(uri);
      if (result.isFailure) {
        return Result.failure(result.error ?? 'Failed to persist permission');
      }

      debugPrint('Persisted permission for $uri');
      return Result.success(null);
    } catch (e) {
      debugPrint('Error persisting permission: $e');
      return Result.failure('Failed to persist permission: $e');
    }
  }

  /// Check if we have permission for a folder URI
  Future<bool> hasPermission(String uri) async {
    try {
      final result = await safService.hasPermission(uri);
      return result.isSuccess && (result.data ?? false);
    } catch (e) {
      debugPrint('Error checking permission: $e');
      return false;
    }
  }

  /// Update learning status for a folder
  Future<void> updateLearningStatus(String uri, String status) async {
    await databaseService.updateFolderLearningStatus(uri, status);
    debugPrint('Updated learning status for $uri to $status');
  }

  /// Update learned labels for a folder
  Future<void> updateLearnedLabels(String uri, Map<String, double> labels) async {
    final db = await databaseService.database;
    await db.update(
      'folders',
      {
        'learned_labels': labels.toString(),
      },
      where: 'uri = ?',
      whereArgs: [uri],
    );
    debugPrint('Updated learned labels for $uri');
  }

  /// Get a specific folder by URI
  Future<FolderModel?> getFolderByUri(String uri) async {
    final folders = await getFolders();
    return folders.firstWhere((f) => f.uri == uri, orElse: () => null as FolderModel);
  }

  /// Count photos in a folder (uses cached count from database)
  Future<int> getPhotoCount(String uri) async {
    final folder = await getFolderByUri(uri);
    return folder?.photoCount ?? 0;
  }
}
