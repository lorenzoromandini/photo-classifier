import 'dart:math';
import 'package:uuid/uuid.dart';
import '../database/database_service.dart';
import '../domain/models/transaction_model.dart';
import '../domain/models/transaction_status.dart';

/// Repository for managing transaction log and crash recovery
class TransactionRepository {
  final DatabaseService _databaseService;
  final Uuid _uuid = const Uuid();

  TransactionRepository(this._databaseService);

  /// Create a new transaction log entry
  /// Returns the transaction ID
  Future<String> createTransaction({
    required String sourceUri,
    required String destUri,
    required OperationType operationType,
  }) async {
    final id = _uuid.v4();
    final transaction = TransactionModel(
      id: id,
      sourceUri: sourceUri,
      destUri: destUri,
      operationType: operationType.value,
      status: TransactionStatus.pending.value,
      createdAt: DateTime.now(),
      retryCount: 0,
    );

    await _databaseService.insertTransaction(transaction);
    return id;
  }

  /// Update transaction status
  Future<void> updateStatus(String id, TransactionStatus status, {String? errorMessage}) async {
    final db = await _databaseService.database;
    final updates = <String, dynamic>{
      'status': status.value,
    };

    if (status == TransactionStatus.completed || status == TransactionStatus.failed) {
      updates['completed_at'] = DateTime.now().millisecondsSinceEpoch;
    }

    if (errorMessage != null) {
      updates['error_message'] = errorMessage;
    }

    await db.update(
      'transactions',
      updates,
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  /// Increment retry count for a transaction
  Future<void> incrementRetryCount(String id) async {
    final db = await _databaseService.database;
    await db.rawUpdate(
      'UPDATE transactions SET retry_count = retry_count + 1 WHERE id = ?',
      [id],
    );
  }

  /// Get retry count for a transaction
  Future<int> getRetryCount(String id) async {
    final db = await _databaseService.database;
    final result = await db.rawQuery(
      'SELECT retry_count FROM transactions WHERE id = ?',
      [id],
    );

    if (result.isEmpty) return 0;
    return result.first['retry_count'] as int? ?? 0;
  }

  /// Get all pending transactions (not completed or failed)
  Future<List<TransactionModel>> getPendingTransactions() async {
    return await _databaseService.getPendingTransactions();
  }

  /// Get all failed transactions
  Future<List<TransactionModel>> getFailedTransactions() async {
    final db = await _databaseService.database;
    final maps = await db.query(
      'transactions',
      where: 'status = ?',
      whereArgs: ['failed'],
    );
    return maps.map((map) => TransactionModel.fromMap(map)).toList();
  }

  /// Clean up old completed transactions
  Future<void> cleanupOldTransactions(Duration age) async {
    final db = await _databaseService.database;
    final cutoff = DateTime.now().subtract(age).millisecondsSinceEpoch;

    await db.delete(
      'transactions',
      where: 'status = ? AND completed_at < ?',
      whereArgs: ['completed', cutoff],
    );
  }

  /// Get transaction by ID
  Future<TransactionModel?> getTransaction(String id) async {
    final db = await _databaseService.database;
    final maps = await db.query(
      'transactions',
      where: 'id = ?',
      whereArgs: [id],
      limit: 1,
    );

    if (maps.isEmpty) return null;
    return TransactionModel.fromMap(maps.first);
  }

  /// Delete transaction by ID
  Future<void> deleteTransaction(String id) async {
    final db = await _databaseService.database;
    await db.delete(
      'transactions',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  /// Update retry count and error message on failure
  Future<void> markFailed(String id, String errorMessage, {int? retryCount}) async {
    final db = await _databaseService.database;
    await db.update(
      'transactions',
      {
        'status': TransactionStatus.failed.value,
        'error_message': errorMessage,
        'completed_at': DateTime.now().millisecondsSinceEpoch,
        if (retryCount != null) 'retry_count': retryCount,
      },
      where: 'id = ?',
      whereArgs: [id],
    );
  }
}
