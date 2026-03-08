import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/onboarding_provider.dart';
import '../widgets/permission_card.dart';
import '../widgets/folder_discovery_progress.dart';
import '../../domain/models/onboarding_state.dart';

/// Single-screen onboarding flow with SAF permission picker and folder discovery
/// Per user decision: no multi-step wizard, tabs, or pages
class OnboardingScreen extends ConsumerWidget {
  const OnboardingScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(onboardingProvider);

    return Scaffold(
      body: SafeArea(
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 300),
          transitionBuilder: (Widget child, Animation<double> animation) {
            return FadeTransition(
              opacity: animation,
              child: SlideTransition(
                position: Tween<Offset>(
                  begin: const Offset(0.0, 0.05),
                  end: Offset.zero,
                ).animate(CurvedAnimation(
                  parent: animation,
                  curve: Curves.easeOutCubic,
                )),
                child: child,
              ),
            );
          },
          child: _buildContent(context, ref, state),
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context, WidgetRef ref, OnboardingState state) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Spacer(flex: 2),

          // App logo and value proposition
          _buildHeader(context),

          const Spacer(flex: 3),

          // Stage-specific content
          switch (state.stage) {
            OnboardingStage.welcome => _buildWelcomeStage(context, ref),
            OnboardingStage.requestingPermission => _buildRequestingPermissionStage(context),
            OnboardingStage.discovering => _buildDiscoveringStage(context, state),
            OnboardingStage.complete => _buildCompleteStage(context, ref),
            OnboardingStage.error => _buildErrorStage(context, ref, state),
          },

          const Spacer(),
        ],
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Column(
      children: [
        // App icon/logo
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primaryContainer,
            borderRadius: BorderRadius.circular(30),
          ),
          child: Icon(
            Icons.photo_library,
            size: 64,
            color: Theme.of(context).colorScheme.onPrimaryContainer,
          ),
        ),
        const SizedBox(height: 24),

        // Title
        Text(
          'Photo Classifier',
          style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 12),

        // Value proposition description
        Text(
          'Automatically organize your photos using on-device machine learning. '
          'Your photos stay private and never leave your device.',
          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                height: 1.5,
              ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildWelcomeStage(BuildContext context, WidgetRef ref) {
    return Column(
      children: [
        // Permission request card with context explanation
        PermissionCard(
          onGrantPressed: () {
            ref.read(onboardingProvider.notifier).requestPermission();
          },
        ),
        const SizedBox(height: 16),

        // Privacy note
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.security,
              size: 16,
              color: Theme.of(context).colorScheme.outline,
            ),
            const SizedBox(width: 8),
            Text(
              'No internet connection required - works offline',
              style: Theme.of(context).textTheme.labelSmall?.copyWith(
                    color: Theme.of(context).colorScheme.outline,
                  ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildRequestingPermissionStage(BuildContext context) {
    // Spinner while waiting for permission response (per user decision)
    return Column(
      children: [
        SizedBox(
          width: 64,
          height: 64,
          child: CircularProgressIndicator(
            color: Theme.of(context).colorScheme.primary,
            strokeWidth: 4,
          ),
        ),
        const SizedBox(height: 24),
        Text(
          'Opening folder picker...',
          style: Theme.of(context).textTheme.bodyLarge,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 8),
        Text(
          'Select your Pictures folder to continue',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: Theme.of(context).hintColor,
              ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildDiscoveringStage(BuildContext context, OnboardingState state) {
    // Show folder discovery progress
    return FolderDiscoveryProgress(
      discoveredFolders: state.discoveredFolders,
      totalPhotos: state.totalPhotos > 0 ? state.totalPhotos : null,
      currentFolder: state.currentFolder,
      isComplete: false,
    );
  }

  Widget _buildCompleteStage(BuildContext context, WidgetRef ref) {
    // Success confirmation before main screen (per user decision)
    final state = ref.watch(onboardingProvider);

    // Auto-navigate after short delay
    Future.delayed(const Duration(milliseconds: 1500), () async {
      await ref.read(onboardingProvider.notifier).completeOnboarding();
      // Navigation handled by listener in main.dart or here
    });

    return Column(
      children: [
        // Success indicator
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.primaryContainer,
            shape: BoxShape.circle,
          ),
          child: Icon(
            Icons.check_circle,
            size: 64,
            color: Theme.of(context).colorScheme.onPrimaryContainer,
          ),
        ),
        const SizedBox(height: 24),

        // Success message
        Text(
          'Ready to organize!',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
              ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 12),

        Text(
          'Found ${state.discoveredFolders} folders with ${state.totalPhotos} photos',
          style: Theme.of(context).textTheme.bodyLarge,
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 32),

        // Manual continue button (in case auto-nav doesn't trigger)
        FilledButton.icon(
          onPressed: () async {
            await ref.read(onboardingProvider.notifier).completeOnboarding();
            if (context.mounted) {
              Navigator.of(context).pushReplacementNamed('/main');
            }
          },
          icon: const Icon(Icons.arrow_forward),
          label: const Text('Continue'),
        ),
      ],
    );
  }

  Widget _buildErrorStage(BuildContext context, WidgetRef ref, OnboardingState state) {
    return Column(
      children: [
        // Error icon
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.errorContainer,
            shape: BoxShape.circle,
          ),
          child: Icon(
            Icons.error_outline,
            size: 64,
            color: Theme.of(context).colorScheme.onErrorContainer,
          ),
        ),
        const SizedBox(height: 24),

        // Error title
        Text(
          'Something went wrong',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
              ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 12),

        // Error message
        if (state.errorMessage != null)
          Text(
            state.errorMessage!,
            style: Theme.of(context).textTheme.bodyMedium,
            textAlign: TextAlign.center,
          ),
        const SizedBox(height: 32),

        // Retry button
        FilledButton.icon(
          onPressed: () {
            ref.read(onboardingProvider.notifier).retry();
          },
          icon: const Icon(Icons.refresh),
          label: const Text('Try Again'),
        ),
      ],
    );
  }
}
