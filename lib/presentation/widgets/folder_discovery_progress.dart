import 'package:flutter/material.dart';

/// A widget that shows progress during folder discovery
class FolderDiscoveryProgress extends StatelessWidget {
  /// Number of folders discovered so far
  final int discoveredFolders;

  /// Total photos found (optional)
  final int? totalPhotos;

  /// Name of current folder being scanned (optional)
  final String? currentFolder;

  /// Whether discovery is complete
  final bool isComplete;

  const FolderDiscoveryProgress({
    super.key,
    required this.discoveredFolders,
    this.totalPhotos,
    this.currentFolder,
    this.isComplete = false,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Progress indicator
            if (isComplete)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.check_circle,
                  color: Theme.of(context).colorScheme.onPrimaryContainer,
                  size: 48,
                ),
              )
            else
              SizedBox(
                width: 48,
                height: 48,
                child: CircularProgressIndicator(
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),

            const SizedBox(height: 16),

            // Progress text
            Text(
              isComplete
                  ? 'Ready to organize!'
                  : 'Found $discoveredFolders ${_pluralize('folder', discoveredFolders)}',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
              textAlign: TextAlign.center,
            ),

            const SizedBox(height: 8),

            // Subtitle with scanning info or photo count
            if (currentFolder != null && !isComplete)
              Text(
                'Scanning "$currentFolder"...',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).hintColor,
                    ),
                textAlign: TextAlign.center,
              )
            else if (totalPhotos != null && totalPhotos! > 0)
              Text(
                '${totalPhotos} photos total',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).hintColor,
                    ),
                textAlign: TextAlign.center,
              )
            else if (!isComplete)
              Text(
                'Discovering folders...',
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).hintColor,
                    ),
                textAlign: TextAlign.center,
              ),
          ],
        ),
      ),
    );
  }

  String _pluralize(String word, int count) {
    return count == 1 ? word : '${word}s';
  }
}
