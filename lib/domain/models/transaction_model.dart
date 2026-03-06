class TransactionModel {
  final String id;
  final String sourceUri;
  final String destUri;
  final String operationType;
  final String status;
  final DateTime createdAt;
  final DateTime? completedAt;
  final int retryCount;
  final String? errorMessage;

  TransactionModel({
    required this.id,
    required this.sourceUri,
    required this.destUri,
    required this.operationType,
    required this.status,
    required this.createdAt,
    this.completedAt,
    this.retryCount = 0,
    this.errorMessage,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'source_uri': sourceUri,
      'dest_uri': destUri,
      'operation_type': operationType,
      'status': status,
      'created_at': createdAt.millisecondsSinceEpoch,
      'completed_at': completedAt?.millisecondsSinceEpoch,
      'retry_count': retryCount,
      'error_message': errorMessage,
    };
  }

  factory TransactionModel.fromMap(Map<String, dynamic> map) {
    return TransactionModel(
      id: map['id'] as String,
      sourceUri: map['source_uri'] as String,
      destUri: map['dest_uri'] as String,
      operationType: map['operation_type'] as String,
      status: map['status'] as String,
      createdAt: DateTime.fromMillisecondsSinceEpoch(map['created_at'] as int),
      completedAt: map['completed_at'] != null
          ? DateTime.fromMillisecondsSinceEpoch(map['completed_at'] as int)
          : null,
      retryCount: map['retry_count'] as int? ?? 0,
      errorMessage: map['error_message'] as String?,
    );
  }
}
