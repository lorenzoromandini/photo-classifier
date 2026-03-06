import 'dart:io';
import 'package:file_picker/file_picker.dart';
import 'package:permission_handler/permission_handler.dart';
import '../domain/models/folder_model.dart';

class FolderDiscoveryService {
  Future<List<FolderModel>> discoverFolders(String basePath) async {
    final List<FolderModel> folders = [];
    
    try {
      final directory = Directory(basePath);
      if (!await directory.exists()) {
        return folders;
      }
      
      await for (final entity in directory.list(recursive: false)) {
        if (entity is Directory) {
          final name = entity.path.split(Platform.pathSeparator).last;
          
          // Skip system folders
          if (_isSystemFolder(name)) continue;
          
          // Count photos
          final photoCount = await _countPhotos(entity.path);
          
          folders.add(FolderModel(
            uri: entity.path,
            name: name,
            displayName: name,
            photoCount: photoCount,
            createdAt: DateTime.now(),
          ));
        }
      }
    } catch (e) {
      throw Exception('Failed to discover folders: $e');
    }
    
    return folders;
  }
  
  bool _isSystemFolder(String name) {
    final systemFolders = [
      'Android',
      '.thumbnails',
      '.trash',
      'lost+found',
      'LOST.DIR',
    ];
    return systemFolders.contains(name) || name.startsWith('.');
  }
  
  Future<int> _countPhotos(String folderPath) async {
    int count = 0;
    final extensions = ['.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif'];
    
    try {
      final directory = Directory(folderPath);
      if (!await directory.exists()) return 0;
      
      await for (final entity in directory.list(recursive: false)) {
        if (entity is File) {
          final ext = entity.path.toLowerCase().split('.').last;
          if (extensions.contains('.$ext')) {
            count++;
          }
        }
      }
    } catch (e) {
      // Ignore errors
    }
    
    return count;
  }
  
  Future<bool> requestStoragePermission() async {
    if (Platform.isAndroid) {
      final status = await Permission.storage.request();
      return status.isGranted;
    }
    return true;
  }
}
