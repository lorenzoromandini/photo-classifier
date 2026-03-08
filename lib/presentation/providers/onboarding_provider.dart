import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import '../../domain/models/onboarding_state.dart';
import '../../data/database/database_service.dart';
import '../../data/services/preference_service.dart';
import '../../data/services/folder_discovery_service.dart';
import '../../domain/models/folder_model.dart';

// Provider for onboarding state
final onboardingProvider = StateNotifierProvider<OnboardingNotifier, OnboardingState>((ref) {
  return OnboardingNotifier();
});

// Provider for folder discovery service
final folderDiscoveryServiceProvider = Provider<FolderDiscoveryService>((ref) {
  return FolderDiscoveryService(
    databaseService: DatabaseService(),
  );
});

/// State notifier for onboarding flow management
class OnboardingNotifier extends StateNotifier<OnboardingState> {
  final DatabaseService _db = DatabaseService();
  final PreferenceService _prefs = PreferenceService();
  String? _selectedFolderUri;
  String? _selectedFolderName;

  OnboardingNotifier() : super(const OnboardingState());

  /// Request SAF permission to access a folder
  /// Shows spinner while picker is opening (per user decision)
  Future<void> requestPermission() async {
    // Set stage to requesting permission (shows spinner)
    state = state.copyWith(
      stage: OnboardingStage.requestingPermission,
      isLoading: true,
      errorMessage: null,
    );

    try {
      // Show SAF folder picker
      // Note: file_picker uses ACTION_OPEN_DOCUMENT_TREE on Android
      final result = await FilePicker.platform.getDirectoryPath();

      if (result == null) {
        // User cancelled or denied
        state = state.copyWith(
          stage: OnboardingStage.error,
          isLoading: false,
          errorMessage: 'Permission denied. Please grant access to continue.',
        );
        return;
      }

      // Permission granted - extract folder name from URI
      _selectedFolderUri = result;
      _selectedFolderName = result.split('/').last;

      // Persist URI for future access
      await _prefs.addFolderUri(result);

      // Start folder discovery
      await startDiscovery();
    } catch (e) {
      state = state.copyWith(
        stage: OnboardingStage.error,
        isLoading: false,
        errorMessage: 'Error requesting permission: $e',
      );
    }
  }

  /// Start discovering folders in the selected Pictures/ directory
  Future<void> startDiscovery() async {
    if (_selectedFolderUri == null) {
      state = state.copyWith(
        stage: OnboardingStage.error,
        errorMessage: 'No folder selected',
      );
      return;
    }

    // Set discovering stage
    state = state.copyWith(
      stage: OnboardingStage.discovering,
      isLoading: true,
      discoveredFolders: 0,
      totalPhotos: 0,
      currentFolder: null,
    );

    try {
      // Get folder discovery service
      final discoveryService = FolderDiscoveryService(databaseService: _db);

      // Discover folders and update progress
      // Note: In a full implementation, this would stream progress updates
      final folders = await discoveryService.discoverFoldersInDirectory(
        _selectedFolderUri!,
        onProgress: (count, total) {
          // Update state with progress
          state = state.copyWith(
            discoveredFolders: count,
            isLoading: count < total, // Still loading while discovering
          );
        },
      );

      // Create folder entries in database if not already present
      for (final folderInfo in folders) {
        final folder = FolderModel(
          uri: folderInfo.uri,
          name: folderInfo.name,
          displayName: folderInfo.displayName ?? folderInfo.name,
          photoCount: folderInfo.photoCount,
          createdAt: DateTime.now(),
        );

        // Insert or update folder in database
        await _db.insertFolder(folder);
      }

      // Calculate totals
      final totalFolders = folders.length;
      final totalPhotos = folders.fold<int>(0, (sum, f) => sum + f.photoCount);

      // Discovery complete - show success state
      state = state.copyWith(
        stage: OnboardingStage.complete,
        isLoading: false,
        discoveredFolders: totalFolders,
        totalPhotos: totalPhotos,
      );
    } catch (e) {
      state = state.copyWith(
        stage: OnboardingStage.error,
        isLoading: false,
        errorMessage: 'Failed to discover folders: $e',
      );
    }
  }

  /// Complete onboarding and save state
  Future<void> completeOnboarding() async {
    await _prefs.init();
    await _prefs.setOnboardingComplete(true);
  }

  /// Retry permission request after error
  Future<void> retry() async {
    state = state.copyWith(
      stage: OnboardingStage.welcome,
      isLoading: false,
      errorMessage: null,
    );
  }

  /// Check if onboarding has been completed previously
  Future<bool> hasCompletedOnboarding() async {
    await _prefs.init();
    return await _prefs.isOnboardingComplete();
  }
}
