import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/folder_provider.dart';
import '../providers/preference_provider.dart';

class MainScreen extends ConsumerWidget {
  const MainScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final foldersAsync = ref.watch(foldersProvider);
    final confidence = ref.watch(confidenceThresholdProvider);
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('Photo Classifier'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => _showSettings(context, ref),
          ),
        ],
      ),
      body: foldersAsync.when(
        data: (folders) {
          if (folders.isEmpty) {
            return const Center(
              child: Text('No folders discovered yet'),
            );
          }
          return ListView.builder(
            itemCount: folders.length,
            itemBuilder: (context, index) {
              final folder = folders[index];
              return ListTile(
                leading: const Icon(Icons.folder),
                title: Text(folder.displayName),
                subtitle: Text('${folder.photoCount} photos'),
                trailing: folder.learningStatus == 'completed'
                    ? const Icon(Icons.check_circle, color: Colors.green)
                    : folder.learningStatus == 'in_progress'
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.pending),
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, stack) => Center(
          child: Text('Error: $error'),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddFolderDialog(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  void _showSettings(BuildContext context, WidgetRef ref) {
    final confidence = ref.read(confidenceThresholdProvider);
    
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Settings',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 16),
              Text(
                'Confidence Threshold',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              Text(
                'Photos with confidence below this threshold will be left in place for manual review.',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _buildConfidenceButton(
                    context, ref, 'Low', 0.6, confidence),
                  _buildConfidenceButton(
                    context, ref, 'Medium', 0.75, confidence),
                  _buildConfidenceButton(
                    context, ref, 'High', 0.9, confidence),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                'Current: ${(confidence * 100).toInt()}%',
                style: Theme.of(context).textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildConfidenceButton(
    BuildContext context,
    WidgetRef ref,
    String label,
    double value,
    double current,
  ) {
    final isSelected = (current - value).abs() < 0.01;
    
    return ElevatedButton(
      onPressed: () {
        ref.read(confidenceThresholdProvider.notifier).setValue(value);
      },
      style: ElevatedButton.styleFrom(
        backgroundColor: isSelected
            ? Theme.of(context).colorScheme.primary
            : Theme.of(context).colorScheme.surface,
        foregroundColor: isSelected
            ? Theme.of(context).colorScheme.onPrimary
            : Theme.of(context).colorScheme.onSurface,
      ),
      child: Text(label),
    );
  }

  void _showAddFolderDialog(BuildContext context, WidgetRef ref) {
    // TODO: Implement folder picker
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Add Folder'),
        content: const Text('Folder picker will open here'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
        ],
      ),
    );
  }
}
