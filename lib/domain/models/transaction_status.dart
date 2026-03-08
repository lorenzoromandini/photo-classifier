/// Status states for transaction log
enum TransactionStatus {
  pending,
  copying,
  verifying,
  deleting,
  completed,
  failed,
}

/// Types of file operations
enum OperationType {
  copy,
  move,
  delete,
}

/// Extension to convert enum to string for database storage
extension TransactionStatusX on TransactionStatus {
  String get value {
    switch (this) {
      case TransactionStatus.pending:
        return 'pending';
      case TransactionStatus.copying:
        return 'copying';
      case TransactionStatus.verifying:
        return 'verifying';
      case TransactionStatus.deleting:
        return 'deleting';
      case TransactionStatus.completed:
        return 'completed';
      case TransactionStatus.failed:
        return 'failed';
    }
  }

  static TransactionStatus fromString(String value) {
    switch (value) {
      case 'pending':
        return TransactionStatus.pending;
      case 'copying':
        return TransactionStatus.copying;
      case 'verifying':
        return TransactionStatus.verifying;
      case 'deleting':
        return TransactionStatus.deleting;
      case 'completed':
        return TransactionStatus.completed;
      case 'failed':
        return TransactionStatus.failed;
      default:
        return TransactionStatus.pending;
    }
  }
}

/// Extension to convert enum to string for database storage
extension OperationTypeX on OperationType {
  String get value {
    switch (this) {
      case OperationType.copy:
        return 'copy';
      case OperationType.move:
        return 'move';
      case OperationType.delete:
        return 'delete';
    }
  }

  static OperationType fromString(String value) {
    switch (value) {
      case 'copy':
        return OperationType.copy;
      case 'move':
        return OperationType.move;
      case 'delete':
        return OperationType.delete;
      default:
        return OperationType.move;
    }
  }
}
