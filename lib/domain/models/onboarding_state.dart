/// Onboarding stage enum representing the current state of the onboarding flow
enum OnboardingStage {
  /// Initial welcome screen with value proposition
  welcome,

  /// Opening SAF folder picker
  requestingPermission,

  /// Scanning folders in Pictures/
  discovering,

  /// Success - ready to navigate to main screen
  complete,

  /// Error occurred - user can retry
  error,
}

/// State object for onboarding screen
class OnboardingState {
  /// Current stage in the onboarding flow
  final OnboardingStage stage;

  /// Number of folders discovered so far
  final int discoveredFolders;

  /// Total photos found across all discovered folders
  final int totalPhotos;

  /// Error message if stage is error
  final String? errorMessage;

  /// Whether a loading indicator should be shown
  final bool isLoading;

  /// Name of current folder being scanned (during discovery)
  final String? currentFolder;

  const OnboardingState({
    this.stage = OnboardingStage.welcome,
    this.discoveredFolders = 0,
    this.totalPhotos = 0,
    this.errorMessage,
    this.isLoading = false,
    this.currentFolder,
  });

  OnboardingState copyWith({
    OnboardingStage? stage,
    int? discoveredFolders,
    int? totalPhotos,
    String? errorMessage,
    bool? isLoading,
    String? currentFolder,
  }) {
    return OnboardingState(
      stage: stage ?? this.stage,
      discoveredFolders: discoveredFolders ?? this.discoveredFolders,
      totalPhotos: totalPhotos ?? this.totalPhotos,
      errorMessage: errorMessage ?? this.errorMessage,
      isLoading: isLoading ?? this.isLoading,
      currentFolder: currentFolder ?? this.currentFolder,
    );
  }

  @override
  String toString() {
    return 'OnboardingState(stage: $stage, discoveredFolders: $discoveredFolders, '
        'totalPhotos: $totalPhotos, isLoading: $isLoading)';
  }
}
