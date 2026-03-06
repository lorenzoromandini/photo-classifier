package com.example.photoorganizer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.photoorganizer.data.local.database.dao.CategoryDao
import com.example.photoorganizer.data.local.database.dao.FileOperationDao
import com.example.photoorganizer.data.local.database.dao.FolderDao
import com.example.photoorganizer.data.local.database.dao.PhotoMetadataDao
import com.example.photoorganizer.data.local.database.entities.CategoryEntity
import com.example.photoorganizer.data.local.database.entities.FileOperationEntity
import com.example.photoorganizer.data.local.database.entities.FolderEntity
import com.example.photoorganizer.data.local.database.entities.PhotoMetadataEntity

/**
 * Room database for the Photo Organizer app.
 * Contains entities for categories, folders, photo metadata, and file operations.
 *
 * Schema version 1:
 * - categories: User-defined photo categories with ML labels
 * - folders: Discovered folders with learning status
 * - photo_metadata: Photo processing status and classification results
 * - file_operations: Transaction log for crash recovery
 *
 * @see CategoryEntity
 * @see FolderEntity
 * @see PhotoMetadataEntity
 * @see FileOperationEntity
 */
@Database(
    entities = [
        CategoryEntity::class,
        FolderEntity::class,
        PhotoMetadataEntity::class,
        FileOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun folderDao(): FolderDao
    abstract fun photoMetadataDao(): PhotoMetadataDao
    abstract fun fileOperationDao(): FileOperationDao

    companion object {
        const val DATABASE_NAME = "photo_organizer.db"
    }
}
