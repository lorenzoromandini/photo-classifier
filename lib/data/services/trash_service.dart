import 'package:flutter/foundation.dart';
import '../platform/file_operation_service.dart';
import '../repositories/trash_repository.dart';
import '../../domain/models/trash_item.dart';
import '../../domain/models/file_result.dart';

/// Service for managing trash operations
/// 
/// Provides safe move-to-trash, restore, and permanent deletion functionality.
/// Files in trash are retained for 7 days before automatic cleanup.
class TrashService {
  final FileOperationService _fileOperationService;
  final TrashRepository _trashRepository;

  TrashService({
    required FileOperationService fileOperationService,
    required TrashRepository trashRepository,
  })  : _fileOperationService = fileOperationService,
        _trashRepository = trashRepository;

  /// Get or create the .trash folder in the Pictures directory
  /// Returns the trash folder URI
  Future<FileResult<String>> _getOrCreateTrashFolder(String baseUri) async {
    try {
      // Try to get existing trash folder
      final getResult = await _fileOperationService.invokePlatformMethod(
        'getTrashFolderUri',
        {'baseUri': baseUri},
      );

      if (getResult.isSuccess) {
        final data = getResult.data as Map<dynamic, dynamic>;
        if (data['success'] == true) {
          final trashUri = data['data'] as String?;
          if (trashUri != null && trashUri.isNotEmpty) {
            return FileResult.success(trashUri);
          }
        }
      }

      // Create new trash folder
      final createResult = await _fileOperationService.invokePlatformMethod(
        'createTrashFolder',
        {'baseUri': baseUri},
      );

      if (createResult.isSuccess) {
        final data = createResult.data as Map<dynamic, dynamic>;
        if (data['success'] == true) {
          final trashUri = data['data'] as String;
          debugPrint('TrashService: Created trash folder at $trashUri');
          return FileResult.success(trashUri);
        } else {
          return FileResult.failure(
            data['error'] as String? ?? 'Failed to create trash folder',
            code: data['errorCode'] as String?,
          );
        }
      } else {
        return FileResult.failure(
          createResult.error ?? 'Failed to create trash folder',
          code: createResult.errorCode,
        );
      }
    } catch (e) {
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Move a file to the trash
  /// 
  /// [sourceUri] - The URI of the file to move
  /// [baseUri] - The base folder URI (e.g., Pictures) where .trash will be created
  /// [fileName] - The original file name
  /// 
  /// Returns a [TrashItem] if successful, which includes the trash URI and expiration date
  Future<FileResult<TrashItem>> moveToTrash({
    required String sourceUri,
    required String baseUri,
    required String fileName,
  }) async {
    debugPrint('TrashService: Moving $fileName to trash');

    try {
      // Get file size before moving
      final sizeResult = await _fileOperationService.getFileSize(sourceUri);
      int fileSize = 0;
      if (sizeResult.isSuccess) {
        final sizeData = sizeResult.data;
        fileSize = sizeData ?? 0;
      }

      // Get or create trash folder
      final trashFolderResult = await _getOrCreateTrashFolder(baseUri);
      if (trashFolderResult.isFailure) {
        return FileResult.failure(
          trashFolderResult.error ?? 'Failed to access trash folder',
          code: trashFolderResult.errorCode,
        );
      }

      final trashFolderUri = trashFolderResult.data!;

      // Move file to trash via platform channel
      final moveResult = await _fileOperationService.invokePlatformMethod(
        'moveToTrash',
        {
          'sourceUri': sourceUri,
          'trashFolderUri': trashFolderUri,
          'fileName': fileName,
        },
      );

      if (moveResult.isFailure) {
        return FileResult.failure(
          moveResult.error ?? 'Failed to move to trash',
          code: moveResult.errorCode,
        );
      }

      final moveData = moveResult.data as Map<dynamic, dynamic>;
      if (moveData['success'] != true) {
        return FileResult.failure(
          moveData['error'] as String? ?? 'Failed to move to trash',
          code: moveData['errorCode'] as String?,
        );
      }

      final trashUri = moveData['data'] as String;

      // Create trash item and persist to database
      final trashItem = TrashItem.create(
        originalUri: sourceUri,
        trashUri: trashUri,
        fileName: fileName,
        fileSize: fileSize,
      );

      await _trashRepository.addToTrash(trashItem);
      debugPrint('TrashService: Moved to trash, expires at ${trashItem.expiresAt}');

      return FileResult.success(trashItem);
    } catch (e) {
      debugPrint('TrashService: Error moving to trash: $e');
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Restore a file from trash to its original location
  /// 
  /// [item] - The trash item to restore
  /// 
  /// Marks the item as restored in the database
  Future<FileResult<void>> restoreFromTrash(TrashItem item) async {
    debugPrint('TrashService: Restoring ${item.fileName} from trash');

    try {
      // Restore file via platform channel
      final result = await _fileOperationService.invokePlatformMethod(
        'restoreFromTrash',
        {
          'trashUri': item.trashUri,
          'destUri': item.originalUri,
        },
      );

      if (result.isFailure) {
        return FileResult.failure(
          result.error ?? 'Failed to restore from trash',
          code: result.errorCode,
        );
      }

      final data = result.data as Map<dynamic, dynamic>;
      if (data['success'] != true) {
        return FileResult.failure(
          data['error'] as String? ?? 'Failed to restore from trash',
          code: data['errorCode'] as String?,
        );
      }

      // Mark as restored in database
      await _trashRepository.markRestored(item.id);
      debugPrint('TrashService: Restored ${item.fileName} successfully');

      return FileResult.success(null);
    } catch (e) {
      debugPrint('TrashService: Error restoring from trash: $e');
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Permanently delete a file from trash
  /// 
  /// [item] - The trash item to delete permanently
  /// 
  /// Removes the file from storage and database
  Future<FileResult<void>> permanentlyDelete(TrashItem item) async {
    debugPrint('TrashService: Permanently deleting ${item.fileName}');

    try {
      // Delete from storage via platform channel
      final result = await _fileOperationService.invokePlatformMethod(
        'permanentlyDelete',
        {'trashUri': item.trashUri},
      );

      if (result.isFailure) {
        return FileResult.failure(
          result.error ?? 'Failed to permanently delete',
          code: result.errorCode,
        );
      }

      final data = result.data as Map<dynamic, dynamic>;
      if (data['success'] != true) {
        return FileResult.failure(
          data['error'] as String? ?? 'Failed to permanently delete',
          code: data['errorCode'] as String?,
        );
      }

      // Remove from database
      await _trashRepository.removeFromTrash(item.id);
      debugPrint('TrashService: Permanently deleted ${item.fileName}');

      return FileResult.success(null);
    } catch (e) {
      debugPrint('TrashService: Error permanently deleting: $e');
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Empty the entire trash
  /// 
  /// Permanently deletes all non-restored items
  /// 
  /// Returns count of deleted items
  Future<FileResult<int>> emptyTrash() async {
    debugPrint('TrashService: Emptying trash');

    try {
      final items = await _trashRepository.getTrashItems();
      int deletedCount = 0;

      for (final item in items) {
        final result = await permanentlyDelete(item);
        if (result.isSuccess) {
          deletedCount++;
        } else {
          debugPrint('TrashService: Failed to delete ${item.fileName}: ${result.error}');
        }
      }

      debugPrint('TrashService: Emptied trash, deleted $deletedCount items');
      return FileResult.success(deletedCount);
    } catch (e) {
      debugPrint('TrashService: Error emptying trash: $e');
      return FileResult.failure(e.toString(), code: FileErrorCodes.ioError);
    }
  }

  /// Get all active (non-restored) trash items
  Future<List<TrashItem>> getTrashItems() async {
    return await _trashRepository.getTrashItems();
  }

  /// Get the total size of trash items in bytes
  Future<int> getTrashSize() async {
    return await _trashRepository.getTrashSize();
  }
}
