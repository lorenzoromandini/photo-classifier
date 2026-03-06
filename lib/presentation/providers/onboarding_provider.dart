import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:file_picker/file_picker.dart';
import '../../data/database/database_service.dart';
import '../../data/services/preference_service.dart';
import '../../domain/models/folder_model.dart';

// State class for onboarding
class OnboardingState {
  final bool isLoading;
  final int foldersDiscovered;
  final String? error;

  OnboardingState({
    this.isLoading = false,
    this.foldersDiscovered = 0,
    this.error,
  });

  OnboardingState copyWith({
    bool? isLoading,
    int? foldersDiscovered,
    String? error,
  }) {
    return OnboardingState(
      isLoading: isLoading ?? this.isLoading,
      foldersDiscovered: foldersDiscovered ?? this.foldersDiscovered,
      error: error ?? this.error,
    );
  }
}

// Onboarding provider
final onboardingProvider = StateNotifierProvider<OnboardingNotifier, OnboardingState>((ref) {
  return OnboardingNotifier();
});

class OnboardingNotifier extends StateNotifier<OnboardingState> {
  final DatabaseService _db = DatabaseService();
  final PreferenceService _prefs = PreferenceService();

  OnboardingNotifier() : super(OnboardingState());

  Future<void> requestPermission() async {
    state = state.copyWith(isLoading: true, error: null);
    
    try {
      final result = await FilePicker.platform.getDirectoryPath();
      
      if (result != null) {
        // Save folder URI
        await _prefs.addFolderUri(result);
        
        // Create folder entry in database
        final folder = FolderModel(
          uri: result,
          name: result.split('/').last,
          displayName: result.split('/').last,
          createdAt: DateTime.now(),
        );
        
        await _db.insertFolder(folder);
        
        // Count photos in folder
        // In real implementation, would scan for image files
        state = state.copyWith(
          isLoading: false,
          foldersDiscovered: 1,
        );
      } else {
        state = state.copyWith(
          isLoading: false,
          error: 'Permission denied. Please grant access to continue.',
        );
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: 'Error: $e',
      );
    }
  }

  Future<void> completeOnboarding() async {
    await _prefs.init();
    await _prefs.setOnboardingComplete(true);
  }
}
