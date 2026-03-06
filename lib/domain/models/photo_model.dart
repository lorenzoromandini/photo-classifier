class PhotoModel {
  final String uri;
  final String folderUri;
  final String fileName;
  final int fileSize;
  final DateTime? processedAt;
  final List<Map<String, dynamic>>? detectedLabels;
  final String status;
  final String? targetCategoryId;

  PhotoModel({
    required this.uri,
    required this.folderUri,
    required this.fileName,
    required this.fileSize,
    this.processedAt,
    this.detectedLabels,
    this.status = 'pending',
    this.targetCategoryId,
  });

  Map<String, dynamic> toMap() {
    return {
      'uri': uri,
      'folder_uri': folderUri,
      'file_name': fileName,
      'file_size': fileSize,
      'processed_at': processedAt?.millisecondsSinceEpoch,
      'detected_labels': detectedLabels?.toString(),
      'status': status,
      'target_category_id': targetCategoryId,
    };
  }

  factory PhotoModel.fromMap(Map<String, dynamic> map) {
    return PhotoModel(
      uri: map['uri'] as String,
      folderUri: map['folder_uri'] as String,
      fileName: map['file_name'] as String,
      fileSize: map['file_size'] as int,
      status: map['status'] as String? ?? 'pending',
      targetCategoryId: map['target_category_id'] as String?,
    );
  }
}
