import 'package:uuid/uuid.dart';

/// Domain model representing a file in the trash system
/// 
/// Files moved to trash are retained for 7 days before automatic deletion.
/// This provides a safety net for users to recover accidentally deleted photos.
class TrashItem {
  /// Unique identifier for the trash item
  final String id;
  
  /// Original URI where the file was located
  final String originalUri;
  
  /// Current URI in the .trash folder
  final String trashUri;
  
  /// File name (without path)
  final String fileName;
  
  /// File size in bytes
  final int fileSize;
  
  /// Timestamp when file was moved to trash
  final DateTime movedAt;
  
  /// Timestamp when file should be permanently deleted (movedAt + 7 days)
  final DateTime expiresAt;
  
  /// Whether the file has been restored to original location
  final bool restored;
  
  const TrashItem({
    required this.id,
    required this.originalUri,
    required this.trashUri,
    required this.fileName,
    required this.fileSize,
    required this.movedAt,
    required this.expiresAt,
    this.restored = false,
  });
  
  /// Factory constructor for creating a new trash item
  /// Automatically sets movedAt to now and expiresAt to 7 days from now
  factory TrashItem.create({
    required String originalUri,
    required String trashUri,
    required String fileName,
    required int fileSize,
  }) {
    final now = DateTime.now();
    return TrashItem(
      id: const Uuid().v4(),
      originalUri: originalUri,
      trashUri: trashUri,
      fileName: fileName,
      fileSize: fileSize,
      movedAt: now,
      expiresAt: now.add(const Duration(days: 7)),
    );
  }
  
  /// Check if this item has expired and should be deleted
  bool get isExpired => DateTime.now().isAfter(expiresAt);
  
  /// Days remaining until permanent deletion
  int get daysRemaining {
    final now = DateTime.now();
    if (now.isAfter(expiresAt)) return 0;
    return expiresAt.difference(now).inDays;
  }
  
  /// Convert to Map for database storage
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'original_uri': originalUri,
      'trash_uri': trashUri,
      'file_name': fileName,
      'file_size': fileSize,
      'moved_at': movedAt.millisecondsSinceEpoch,
      'expires_at': expiresAt.millisecondsSinceEpoch,
      'restored': restored ? 1 : 0,
    };
  }
  
  /// Create from database map
  factory TrashItem.fromMap(Map<String, dynamic> map) {
    return TrashItem(
      id: map['id'] as String,
      originalUri: map['original_uri'] as String,
      trashUri: map['trash_uri'] as String,
      fileName: map['file_name'] as String,
      fileSize: map['file_size'] as int,
      movedAt: DateTime.fromMillisecondsSinceEpoch(map['moved_at'] as int),
      expiresAt: DateTime.fromMillisecondsSinceEpoch(map['expires_at'] as int),
      restored: (map['restored'] as int?) == 1,
    );
  }
  
  @override
  String toString() => 'TrashItem(id: $id, fileName: $fileName, expiresAt: $expiresAt)';
  
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TrashItem && runtimeType == other.runtimeType && id == other.id;
  
  @override
  int get hashCode => id.hashCode;
}
