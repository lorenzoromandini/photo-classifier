import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/database/database_service.dart';
import '../../domain/models/folder_model.dart';

final foldersProvider = FutureProvider<List<FolderModel>>((ref) async {
  final db = DatabaseService();
  return await db.getFolders();
});

final folderCountProvider = Provider<int>((ref) {
  final foldersAsync = ref.watch(foldersProvider);
  return foldersAsync.when(
    data: (folders) => folders.length,
    loading: () => 0,
    error: (_, __) => 0,
  );
});
