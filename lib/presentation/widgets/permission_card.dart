import 'package:flutter/material.dart';

/// A card widget that explains why permissions are needed and provides a grant button
class PermissionCard extends StatelessWidget {
  /// Whether permission has been denied
  final bool permissionDenied;

  /// Callback when user taps the grant button
  final VoidCallback onGrantPressed;

  /// Optional error message to display
  final String? errorMessage;

  const PermissionCard({
    super.key,
    this.permissionDenied = false,
    required this.onGrantPressed,
    this.errorMessage,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Icon and title
            Row(
              children: [
                Icon(
                  Icons.folder_open,
                  color: Theme.of(context).colorScheme.primary,
                  size: 28,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Access Your Photos',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Description explaining WHY permission is needed
            Text(
              'Photo Classifier needs access to your Pictures folder to discover existing folders and organize new photos as they arrive.',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 16),

            // Error state if permission denied
            if (permissionDenied || errorMessage != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.errorContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.error_outline,
                      color: Theme.of(context).colorScheme.onErrorContainer,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        errorMessage ?? 'Permission denied. Please grant access to continue.',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.onErrorContainer,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Grant Access button
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: onGrantPressed,
                icon: const Icon(Icons.lock_open),
                label: const Text('Grant Access'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
