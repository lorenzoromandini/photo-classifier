import 'package:flutter/foundation.dart';
import 'package:workmanager/workmanager.dart';
import '../database/database_service.dart';
import '../repositories/trash_repository.dart';
import '../platform/file_operation_service.dart';
import '../services/trash_service.dart';
import '../../domain/models/trash_item.dart';

/// Background worker for daily trash cleanup
/// 
/// Runs once per day to permanently delete trash items that have expired
/// (older than 7 days). This implements the automatic cleanup requirement
/// for the trash system.
/// 
/// The worker is scheduled after onboarding completion and uses WorkManager's
/// periodic task scheduling with KEEP policy to prevent duplicate scheduling.
class TrashCleanupWorker {
  static const String taskName = 'trashCleanup';

  /// Callback dispatcher for WorkManager background execution
  /// 
  /// This must be a top-level function annotated with @pragma('vm:entry-point')
  /// to ensure it's not tree-shaken during release builds.
  @pragma('vm:entry-point')
  static void callbackDispatcher() {
    Workmanager().executeTask((task, inputData) async {
      if (task == taskName) {
        debugPrint('TrashCleanupWorker: Starting cleanup task');
        
        try {
          // Initialize services
          final db = DatabaseService();
          final fileService = FileOperationService();
          final trashRepo = TrashRepository(db);
          final trashService = TrashService(
            fileOperationService: fileService,
            trashRepository: trashRepo,
          );
          
          // Get expired items
          final expiredItems = await trashRepo.getExpiredItems();
          debugPrint('TrashCleanupWorker: Found ${expiredItems.length} expired items');
          
          // Delete each expired item permanently
          int deletedCount = 0;
          for (final item in expiredItems) {
            try {
              final result = await trashService.permanentlyDelete(item);
              if (result.isSuccess) {
                deletedCount++;
                debugPrint('TrashCleanupWorker: Deleted ${item.fileName}');
              } else {
                debugPrint('TrashCleanupWorker: Failed to delete ${item.fileName}: ${result.error}');
              }
            } catch (e) {
              debugPrint('TrashCleanupWorker: Error deleting ${item.fileName}: $e');
            }
          }
          
          debugPrint('Trash cleanup: deleted $deletedCount items');
          return true;
        } catch (e) {
          debugPrint('TrashCleanupWorker: Error during cleanup: $e');
          return false;
        }
      }
      
      return true;
    });
  }

  /// Schedule daily trash cleanup
  /// 
  /// Uses WorkManager's periodic task with 1-day frequency.
  /// The KEEP policy prevents duplicate scheduling if called multiple times.
  /// 
  /// Requires battery not low constraint to avoid draining user's device.
  static Future<void> scheduleDailyCleanup() async {
    debugPrint('TrashCleanupWorker: Scheduling daily cleanup');
    
    await Workmanager().registerPeriodicTask(
      taskName,
      taskName,
      frequency: const Duration(days: 1),
      constraints: Constraints(
        requiresBatteryNotLow: true,
        requiresStorageNotLow: false, // Can run even if storage is low (cleanup frees space)
      ),
      existingWorkPolicy: ExistingWorkPolicy.keep, // Prevent duplicates
      initialDelay: const Duration(hours: 1), // Wait 1 hour after app start
    );
    
    debugPrint('TrashCleanupWorker: Daily cleanup scheduled');
  }

  /// Cancel the scheduled cleanup worker
  static Future<void> cancelCleanup() async {
    debugPrint('TrashCleanupWorker: Cancelling scheduled cleanup');
    await Workmanager().cancelByUniqueName(taskName);
  }

  /// Manually trigger cleanup (for testing or user-initiated cleanup)
  static Future<int> runCleanupNow() async {
    debugPrint('TrashCleanupWorker: Running manual cleanup');
    
    try {
      final db = DatabaseService();
      final fileService = FileOperationService();
      final trashRepo = TrashRepository(db);
      final trashService = TrashService(
        fileOperationService: fileService,
        trashRepository: trashRepo,
      );
      
      final expiredItems = await trashRepo.getExpiredItems();
      int deletedCount = 0;
      
      for (final item in expiredItems) {
        final result = await trashService.permanentlyDelete(item);
        if (result.isSuccess) {
          deletedCount++;
        }
      }
      
      debugPrint('TrashCleanupWorker: Manual cleanup deleted $deletedCount items');
      return deletedCount;
    } catch (e) {
      debugPrint('TrashCleanupWorker: Manual cleanup failed: $e');
      return 0;
    }
  }
}
