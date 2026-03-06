import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import '../domain/models/folder_model.dart';
import '../domain/models/photo_model.dart';
import '../domain/models/transaction_model.dart';

class DatabaseService {
  static Database? _database;
  
  Future<Database> get database async {
    _database ??= await _initDatabase();
    return _database!;
  }
  
  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'photo_classifier.db');
    
    return await openDatabase(
      path,
      version: 1,
      onCreate: _createTables,
    );
  }
  
  Future<void> _createTables(Database db, int version) async {
    // Folders table
    await db.execute('''
      CREATE TABLE folders (
        uri TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        display_name TEXT NOT NULL,
        photo_count INTEGER DEFAULT 0,
        is_active INTEGER DEFAULT 1,
        learned_labels TEXT,
        learning_status TEXT DEFAULT 'pending',
        created_at INTEGER NOT NULL
      )
    ''');
    
    // Photos table
    await db.execute('''
      CREATE TABLE photos (
        uri TEXT PRIMARY KEY,
        folder_uri TEXT NOT NULL,
        file_name TEXT NOT NULL,
        file_size INTEGER NOT NULL,
        processed_at INTEGER,
        detected_labels TEXT,
        status TEXT DEFAULT 'pending',
        target_category_id TEXT,
        FOREIGN KEY (folder_uri) REFERENCES folders (uri)
      )
    ''');
    
    // Transactions table for crash recovery
    await db.execute('''
      CREATE TABLE transactions (
        id TEXT PRIMARY KEY,
        source_uri TEXT NOT NULL,
        dest_uri TEXT NOT NULL,
        operation_type TEXT NOT NULL,
        status TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        completed_at INTEGER,
        retry_count INTEGER DEFAULT 0,
        error_message TEXT
      )
    ''');
    
    // Categories table
    await db.execute('''
      CREATE TABLE categories (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        folder_uri TEXT NOT NULL,
        ml_labels TEXT,
        confidence_threshold REAL DEFAULT 0.9,
        created_at INTEGER NOT NULL
      )
    ''');
    
    // Trash table
    await db.execute('''
      CREATE TABLE trash (
        original_uri TEXT PRIMARY KEY,
        trash_uri TEXT NOT NULL,
        file_name TEXT NOT NULL,
        file_size INTEGER NOT NULL,
        moved_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        restored INTEGER DEFAULT 0
      )
    ''');
  }
  
  // Folder operations
  Future<List<FolderModel>> getFolders() async {
    final db = await database;
    final maps = await db.query('folders');
    return maps.map((map) => FolderModel.fromMap(map)).toList();
  }
  
  Future<void> insertFolder(FolderModel folder) async {
    final db = await database;
    await db.insert('folders', folder.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace);
  }
  
  Future<void> updateFolderLearningStatus(String uri, String status) async {
    final db = await database;
    await db.update(
      'folders',
      {'learning_status': status},
      where: 'uri = ?',
      whereArgs: [uri],
    );
  }
  
  // Transaction operations for crash recovery
  Future<void> insertTransaction(TransactionModel transaction) async {
    final db = await database;
    await db.insert('transactions', transaction.toMap());
  }
  
  Future<void> updateTransactionStatus(String id, String status) async {
    final db = await database;
    await db.update(
      'transactions',
      {
        'status': status,
        'completed_at': DateTime.now().millisecondsSinceEpoch,
      },
      where: 'id = ?',
      whereArgs: [id],
    );
  }
  
  Future<List<TransactionModel>> getPendingTransactions() async {
    final db = await database;
    final maps = await db.query(
      'transactions',
      where: 'status IN (?, ?, ?, ?)',
      whereArgs: ['pending', 'copying', 'verifying', 'deleting'],
    );
    return maps.map((map) => TransactionModel.fromMap(map)).toList();
  }
  
  // Trash operations
  Future<void> insertTrashItem(Map<String, dynamic> item) async {
    final db = await database;
    await db.insert('trash', item);
  }
  
  Future<List<Map<String, dynamic>>> getExpiredTrashItems() async {
    final db = await database;
    final now = DateTime.now().millisecondsSinceEpoch;
    return await db.query(
      'trash',
      where: 'expires_at < ? AND restored = 0',
      whereArgs: [now],
    );
  }
  
  Future<void> deleteTrashItem(String uri) async {
    final db = await database;
    await db.delete('trash', where: 'original_uri = ?', whereArgs: [uri]);
  }
  
  // Photo operations
  Future<void> insertPhoto(PhotoModel photo) async {
    final db = await database;
    await db.insert('photos', photo.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace);
  }
  
  Future<void> updatePhotoStatus(String uri, String status) async {
    final db = await database;
    await db.update(
      'photos',
      {'status': status, 'processed_at': DateTime.now().millisecondsSinceEpoch},
      where: 'uri = ?',
      whereArgs: [uri],
    );
  }
}
