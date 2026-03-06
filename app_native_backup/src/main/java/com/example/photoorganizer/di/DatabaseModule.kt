package com.example.photoorganizer.di

import android.content.Context
import androidx.room.Room
import com.example.photoorganizer.data.local.database.AppDatabase
import com.example.photoorganizer.data.local.database.dao.CategoryDao
import com.example.photoorganizer.data.local.database.dao.FileOperationDao
import com.example.photoorganizer.data.local.database.dao.FolderDao
import com.example.photoorganizer.data.local.database.dao.PhotoMetadataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database dependencies.
 * Configures the database with proper migrations and DAO injections.
 *
 * @see AppDatabase
 * @see CategoryDao
 * @see FolderDao
 * @see PhotoMetadataDao
 * @see FileOperationDao
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton Room database instance.
     * Configured with fallback to destructive migration for development.
     * In production, proper migrations should be defined.
     *
     * @param context Application context for database file
     * @return AppDatabase singleton instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            // For development only - in production use proper migrations
            .fallbackToDestructiveMigration()
            // Export schema for version control
            .createFromAsset("database/photo_organizer.db")
            .build()
    }

    /**
     * Provides CategoryDao from the database.
     */
    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides FolderDao from the database.
     */
    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao {
        return database.folderDao()
    }

    /**
     * Provides PhotoMetadataDao from the database.
     */
    @Provides
    fun providePhotoMetadataDao(database: AppDatabase): PhotoMetadataDao {
        return database.photoMetadataDao()
    }

    /**
     * Provides FileOperationDao from the database.
     */
    @Provides
    fun provideFileOperationDao(database: AppDatabase): FileOperationDao {
        return database.fileOperationDao()
    }
}
