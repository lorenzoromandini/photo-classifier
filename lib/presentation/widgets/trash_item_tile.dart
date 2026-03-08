import 'package:flutter/material.dart';
import '../../domain/models/trash_item.dart';

/// Widget displaying a single trash item with actions
/// 
/// Shows file name, size, and days remaining until permanent deletion.
/// Supports restore (tap) and permanent delete (long-press) actions.
class TrashItemTile extends StatelessWidget {
  final TrashItem item;
  final VoidCallback? onRestore;
  final VoidCallback? onDelete;

  const TrashItemTile({
    super.key,
    required this.item,
    this.onRestore,
    this.onDelete,
  });

  /// Helper to format bytes to human-readable size
  static String _formatBytes(int bytes) {
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

  @override
  Widget build(BuildContext context) {
    final daysRemaining = item.daysRemaining;
    final isExpiringSoon = daysRemaining <= 2 && daysRemaining > 0;
    final isExpired = daysRemaining <= 0;
    
    return GestureDetector(
      onLongPress: onDelete,
      child: ListTile(
        leading: Icon(
          isExpired ? Icons.delete_forever : Icons.delete_outline,
          color: isExpired 
              ? Colors.red 
              : isExpiringSoon 
                  ? Colors.orange 
                  : Colors.grey,
        ),
        title: Text(
          item.fileName,
          style: TextStyle(
            decoration: isExpired ? TextDecoration.lineThrough : null,
            color: isExpired ? Colors.red : null,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _formatBytes(item.fileSize),
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 2),
            Text(
              isExpired
                  ? 'Ready for deletion'
                  : '$daysRemaining days remaining',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: isExpired
                    ? Colors.red
                    : isExpiringSoon
                        ? Colors.orange
                        : Colors.grey,
              ),
            ),
          ],
        ),
        trailing: IconButton(
          icon: const Icon(Icons.restore),
          tooltip: 'Restore',
          onPressed: onRestore,
        ),
        dense: false,
      ),
    );
  }
}

/// Empty state widget for trash when there are no items
class TrashEmptyState extends StatelessWidget {
  const TrashEmptyState({super.key});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.delete_outline,
            size: 64,
            color: Colors.grey.shade400,
          ),
          const SizedBox(height: 16),
          Text(
            'Trash is empty',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 8),
          Text(
            'Deleted photos will appear here for 7 days',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Colors.grey,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
