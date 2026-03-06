class FolderModel {
  final String uri;
  final String name;
  final String displayName;
  final int photoCount;
  final bool isActive;
  final Map<String, double>? learnedLabels;
  final String learningStatus;
  final DateTime createdAt;

  FolderModel({
    required this.uri,
    required this.name,
    required this.displayName,
    this.photoCount = 0,
    this.isActive = true,
    this.learnedLabels,
    this.learningStatus = 'pending',
    required this.createdAt,
  });

  Map<String, dynamic> toMap() {
    return {
      'uri': uri,
      'name': name,
      'display_name': displayName,
      'photo_count': photoCount,
      'is_active': isActive ? 1 : 0,
      'learned_labels': learnedLabels?.toString(),
      'learning_status': learningStatus,
      'created_at': createdAt.millisecondsSinceEpoch,
    };
  }

  factory FolderModel.fromMap(Map<String, dynamic> map) {
    return FolderModel(
      uri: map['uri'] as String,
      name: map['name'] as String,
      displayName: map['display_name'] as String,
      photoCount: map['photo_count'] as int? ?? 0,
      isActive: (map['is_active'] as int? ?? 1) == 1,
      learningStatus: map['learning_status'] as String? ?? 'pending',
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['created_at'] as int),
    );
  }
}
