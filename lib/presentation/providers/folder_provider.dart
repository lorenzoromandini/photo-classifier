import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/database/database_service.dart';
import '../../data/repositories/folder_repository.dart';
import '../../data/platform/saf_service.dart';
import '../../domain/models/folder_model.dart';

// Provider for SafService
final safServiceProvider = Provider<SafService>((ref) {
  return SafService();
});

// Provider for FolderRepository
final folderRepositoryProvider = Provider<FolderRepository>((ref) {
  return FolderRepository(
    databaseService: DatabaseService(),
    safService: ref.watch(safServiceProvider),
  );
});

// Reactive stream provider for folders (reads from database)
final foldersProvider = StreamProvider.autoDispose<List<FolderModel>>((ref) {
  final repository = ref.watch(folderRepositoryProvider);
  
  // Poll database every 500ms for changes
  // In production, you might want to use a proper change notification system
  return Stream.periodic(const Duration(milliseconds: 500), (_) async {
    return await repository.getFolders();
  }).asyncMap((future) => future);
});

// Provider for folder count
final folderCountProvider = Provider.autoDispose<int>((ref) {
  final foldersAsync = ref.watch(foldersProvider);
  return foldersAsync.when(
    data: (folders) => folders.length,
    loading: () => 0,
    error: (_, __) => 0,
  );
});

// Provider for folder actions (mutations)
final folderActionsProvider = Provider.autoDispose<FolderActions>((ref) {
  final repository = ref.watch(folderRepositoryProvider);
  final notifier = ref.notifier<FolderStateNotifier>(folderStateProvider);
  
  return FolderActions(
    repository: repository,
    notifier: notifier,
  );
});

// State notifier for folder state management
class FolderStateNotifier extends StateNotifier<FolderState> {
  final FolderRepository repository;
  
  FolderStateNotifier(this.repository) : super(FolderState());
  
  /// Sync folders from SAF to database
  Future<Result<void>> syncFolders(String baseUri) async {
    state = state.copyWith(isLoading: true);
    
    final result = await repository.discoverAndSyncFolders(baseUri);
    
    state = state.copyWith(
      isLoading: false,
      lastSyncError: result.isFailure ? result.error : null,
    );
    
    return result;
  }
  
  /// Persist permission for a folder
  Future<Result<void>> persistPermission(String uri) async {
    return await repository.persistFolderPermission(uri);
  }
  
  /// Update learning status
  Future<void> updateLearningStatus(String uri, String status) async {
    await repository.updateLearningStatus(uri, status);
  }
}

// Provider for FolderStateNotifier
final folderStateProvider = StateNotifierProvider.autoDispose<FolderStateNotifier, FolderState>((ref) {
  final repository = ref.watch(folderRepositoryProvider);
  return FolderStateNotifier(repository);
});

// State class for folder state
class FolderState {
  final bool isLoading;
  final String? lastSyncError;
  
  FolderState({
    this.isLoading = false,
    this.lastSyncError,
  });
  
  FolderState copyWith({
    bool? isLoading,
    String? lastSyncError,
  }) {
    return FolderState(
      isLoading: isLoading ?? this.isLoading,
      lastSyncError: lastSyncError ?? this.lastSyncError,
    );
  }
}

// Actions for folder mutations
class FolderActions {
  final FolderRepository repository;
  final FolderStateNotifier notifier;
  
  FolderActions({
    required this.repository,
    required this.notifier,
  });
  
  /// Sync folders from SAF to database
  Future<Result<void>> syncFolders(String baseUri) async {
    return await notifier.syncFolders(baseUri);
  }
  
  /// Persist permission for a folder
  Future<Result<void>> persistPermission(String uri) async {
    return await notifier.persistPermission(uri);
  }
  
  /// Update learning status
  Future<void> updateLearningStatus(String uri, String status) async {
    await notifier.updateLearningStatus(uri, status);
  }
}
