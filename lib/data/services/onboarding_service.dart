import 'package:file_picker/file_picker.dart';
import '../database/database_service.dart';
import 'preference_service.dart';
import '../models/folder_model.dart';
import 'folder_discovery_service.dart';

/// Represents the result of an operation
sealed class Result<T> {
  const Result();
  
  T? get valueOrNull;
  String? get errorOrNull;
}

class Success<T> extends Result<T> {
  final T _value;

  const Success(this._value);

  @override
  T? get valueOrNull => _value;

  @override
  String? get errorOrNull => null;
}

class Failure<T> extends Result<T> {
  final String _error;

  const Failure(this._error);

  @override
  T? get valueOrNull => null;

  @override
  String? get errorOrNull => _error;

  @override
  String toString() => 'Failure($_error)';
}

/// Extension methods for Result
extension ResultExtension<T> on Result<T> {
  void when({
    required void Function(T value) success,
    required void Function(String error) failure,
  }) {
    if (this is Success<T>) {
      success((this as Success<T>).valueOrNull!);
    } else if (this is Failure<T>) {
      failure((this as Failure<T>).errorOrNull!);
    }
  }
}

/// Business logic for onboarding operations
/// Handles SAF permission requests, folder discovery, and state persistence
class OnboardingService {
  final PreferenceService _preferenceService;
  final DatabaseService _databaseService;
  final FolderDiscoveryService _folderDiscoveryService;

  OnboardingService({
    required PreferenceService preferenceService,
    required DatabaseService databaseService,
    FolderDiscoveryService? folderDiscoveryService,
  })  : _preferenceService = preferenceService,
        _databaseService = databaseService,
        _folderDiscoveryService =
            folderDiscoveryService ?? FolderDiscoveryService(databaseService: databaseService);

  /// Request SAF permission to access Pictures folder
  /// Returns folder URI on success, error message on failure
  Future<Result<String>> requestPicturesFolder() async {
    try {
      // Show SAF folder picker using file_picker
      // This uses ACTION_OPEN_DOCUMENT_TREE on Android
      final result = await FilePicker.platform.getDirectoryPath();

      if (result == null) {
        return const Failure('Permission denied. Please grant access to continue.');
      }

      return Success(result);
    } catch (e) {
      return Failure('Error requesting permission: $e');
    }
  }

  /// Persist the SAF URI permission for long-term access
  Future<Result<void>> persistPermission(String uri) async {
    try {
      await _preferenceService.addFolderUri(uri);
      return const Success(null);
    } catch (e) {
      return Failure('Failed to persist permission: $e');
    }
  }

  /// Discover folders in the selected directory
  /// Returns list of discovered folders on success
  Future<Result<List<FolderModel>>> discoverFolders(String baseUri,
      {void Function(int count, int total)? onProgress}) async {
    try {
      // Use folder discovery service to scan directory
      final folderInfos = await _folderDiscoveryService.discoverFoldersInDirectory(
        baseUri,
        onProgress: onProgress,
      );

      // Convert to FolderModel and persist to database
      final folderModels = <FolderModel>[];
      for (final folderInfo in folderInfos) {
        final folder = FolderModel(
          uri: folderInfo.uri,
          name: folderInfo.name,
          displayName: folderInfo.displayName ?? folderInfo.name,
          photoCount: folderInfo.photoCount,
          createdAt: DateTime.now(),
        );

        await _databaseService.insertFolder(folder);
        folderModels.add(folder);
      }

      return Success(folderModels);
    } catch (e) {
      return Failure('Failed to discover folders: $e');
    }
  }

  /// Mark onboarding as complete in preferences
  Future<void> completeOnboarding() async {
    await _preferenceService.init();
    await _preferenceService.setOnboardingComplete(true);
  }

  /// Check if onboarding has been completed
  Future<bool> hasCompletedOnboarding() async {
    await _preferenceService.init();
    return await _preferenceService.isOnboardingComplete();
  }

  /// Create folder entry in database
  Future<Result<FolderModel>> createFolderEntry(String uri, String name) async {
    try {
      final folder = FolderModel(
        uri: uri,
        name: name,
        displayName: name,
        createdAt: DateTime.now(),
      );

      await _databaseService.insertFolder(folder);
      return Success(folder);
    } catch (e) {
      return Failure('Failed to create folder entry: $e');
    }
  }

  /// Revoke permission (for cleanup or testing)
  Future<Result<void>> revokePermission(String uri) async {
    try {
      // Note: Actual revocation would need platform channel
      // For now, just remove from preferences
      await _preferenceService.removeFolderUri(uri);
      return const Success(null);
    } catch (e) {
      return Failure('Failed to revoke permission: $e');
    }
  }
}
