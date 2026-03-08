import '../database/database_service.dart';
import '../../domain/models/trash_item.dart';

/// Repository for trash operations
/// 
/// Handles persistence and queries for files in the trash system.
/// Files in trash are retained for 7 days before automatic deletion.
class TrashRepository {
  final DatabaseService _database;

  TrashRepository(this._database);

  /// Add a trash item to the database
  Future<void> addToTrash(TrashItem item) async {
    await _database.insertTrashItem(item);
  }

  /// Get all trash items (including restored, for history)
  Future<List<TrashItem>> getAllTrashItems() async {
    return await _database.getAllTrashItems();
  }

  /// Get active (non-restored) trash items
  Future<List<TrashItem>> getTrashItems() async {
    return await _database.getActiveTrashItems();
  }

  /// Get trash items that have expired and should be deleted
  Future<List<TrashItem>> getExpiredItems() async {
    return await _database.getExpiredTrashItems();
  }

  /// Get a specific trash item by ID
  Future<TrashItem?> getTrashItem(String id) async {
    return await _database.getTrashItem(id);
  }

  /// Get trash item by original URI
  Future<TrashItem?> getTrashItemByOriginalUri(String uri) async {
    return await _database.getTrashItemByOriginalUri(uri);
  }

  /// Mark an item as restored (without deleting from database)
  Future<void> markRestored(String id) async {
    await _database.markRestored(id);
  }

  /// Remove an item from trash (permanent deletion from database)
  Future<void> removeFromTrash(String id) async {
    await _database.deleteTrashItem(id);
  }

  /// Clear all trash items from database
  Future<void> clearAll() async {
    await _database.clearAllTrash();
  }

  /// Get total size of trash items in bytes
  Future<int> getTrashSize() async {
    return await _database.getTrashSize();
  }

  /// Watch for trash items (reactive stream)
  /// 
  /// Note: SQLite doesn't have built-in reactive support,
  /// so this is a polling-based implementation.
  /// For production, consider using a proper reactive database.
  Stream<List<TrashItem>> watchTrashItems() async* {
    while (true) {
      yield await getTrashItems();
      await Future.delayed(const Duration(seconds: 2));
    }
  }
}
