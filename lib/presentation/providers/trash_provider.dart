import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/database/database_service.dart';
import '../../data/platform/file_operation_service.dart';
import '../../data/repositories/trash_repository.dart';
import '../../data/services/trash_service.dart';
import '../../data/workers/trash_cleanup_worker.dart';
import '../../domain/models/trash_item.dart';

/// Helper to format bytes to human-readable size
String _formatBytes(int bytes) {
  if (bytes <= 0) return '0 B';
  
  const suffixes = ['B', 'KB', 'MB', 'GB', 'TB'];
  var size = bytes.toDouble();
  var suffixIndex = 0;
  
  while (size >= 1024 && suffixIndex < suffixes.length - 1) {
    size /= 1024;
    suffixIndex++;
  }
  
  return '${size.toStringAsFixed(size < 10 && suffixIndex > 0 ? 1 : 0)} ${suffixes[suffixIndex]}';
}

// Provider for TrashRepository
final trashRepositoryProvider = Provider<TrashRepository>((ref) {
  return TrashRepository(DatabaseService());
});

// Provider for TrashService
final trashServiceProvider = Provider<TrashService>((ref) {
  return TrashService(
    fileOperationService: FileOperationService(),
    trashRepository: ref.watch(trashRepositoryProvider),
  );
});

// Reactive stream provider for trash items
final trashItemsProvider = StreamProvider.autoDispose<List<TrashItem>>((ref) {
  final repository = ref.watch(trashRepositoryProvider);
  
  // Poll database every 500ms for changes
  return Stream.periodic(const Duration(milliseconds: 500), (_) async {
    return await repository.getTrashItems();
  }).asyncMap((future) => future);
});

// Provider for trash count
final trashCountProvider = Provider.autoDispose<int>((ref) {
  final trashAsync = ref.watch(trashItemsProvider);
  return trashAsync.when(
    data: (items) => items.length,
    loading: () => 0,
    error: (_, __) => 0,
  );
});

// Provider for trash size (formatted)
final trashSizeProvider = Provider.autoDispose<String>((ref) {
  final repository = ref.watch(trashRepositoryProvider);
  
  // Poll every 500ms
  return Stream.periodic(const Duration(milliseconds: 500), (_) async {
    final bytes = await repository.getTrashSize();
    return _formatBytes(bytes);
  }).asyncMap((future) => future).future.then((value) => value).then((value) => value);
});

// Provider for trash actions (mutations)
final trashActionsProvider = Provider.autoDispose<TrashActions>((ref) {
  final service = ref.watch(trashServiceProvider);
  final repository = ref.watch(trashRepositoryProvider);
  
  return TrashActions(
    service: service,
    repository: repository,
  );
});

/// Actions for trash operations
class TrashActions {
  final TrashService _service;
  final TrashRepository _repository;

  TrashActions({
    required TrashService service,
    required TrashRepository repository,
  })  : _service = service,
        _repository = repository;

  /// Move a file to trash
  Future<bool> moveToTrash({
    required String sourceUri,
    required String baseUri,
    required String fileName,
  }) async {
    final result = await _service.moveToTrash(
      sourceUri: sourceUri,
      baseUri: baseUri,
      fileName: fileName,
    );
    
    if (result.isFailure) {
      // Handle error (in production, show snackbar or dialog)
      return false;
    }
    
    return true;
  }

  /// Restore an item from trash
  Future<bool> restoreItem(TrashItem item) async {
    final result = await _service.restoreFromTrash(item);
    return result.isSuccess;
  }

  /// Permanently delete an item from trash
  Future<bool> deleteItem(TrashItem item) async {
    final result = await _service.permanentlyDelete(item);
    return result.isSuccess;
  }

  /// Empty the entire trash
  Future<int> emptyTrash() async {
    final result = await _service.emptyTrash();
    return result.isSuccess ? (result.data ?? 0) : 0;
  }

  /// Get all trash items
  Future<List<TrashItem>> getTrashItems() async {
    return await _repository.getTrashItems();
  }

  /// Get trash size in bytes
  Future<int> getTrashSize() async {
    return await _repository.getTrashSize();
  }

  /// Trigger manual cleanup (for testing)
  Future<int> runCleanupNow() async {
    return await TrashCleanupWorker.runCleanupNow();
  }
}
