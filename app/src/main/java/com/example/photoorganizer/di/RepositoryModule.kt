package com.example.photoorganizer.di

import android.content.Context
import androidx.room.Room
import com.example.photoorganizer.data.local.database.AppDatabase
import com.example.photoorganizer.data.local.database.dao.CategoryDao
import com.example.photoorganizer.data.local.database.dao.FileOperationDao
import com.example.photoorganizer.data.local.database.dao.FolderDao
import com.example.photoorganizer.data.local.database.dao.PhotoMetadataDao
import com.example.photoorganizer.data.local.saf.SafDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository and database dependencies.
 * SingletonComponent ensures dependencies live for app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides the FolderDao from the database.
     */
    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao {
        return database.folderDao()
    }

    /**
     * Provides the CategoryDao from the database.
     */
    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides the PhotoMetadataDao from the database.
     */
    @Provides
    fun providePhotoMetadataDao(database: AppDatabase): PhotoMetadataDao {
        return database.photoMetadataDao()
    }

    /**
     * Provides the FileOperationDao from the database.
     */
    @Provides
    fun provideFileOperationDao(database: AppDatabase): FileOperationDao {
        return database.fileOperationDao()
    }

    /**
     * Provides the SafDataSource.
     * Constructor injection is used, but this is here for explicit binding.
     */
    @Provides
    @Singleton
    fun provideSafDataSource(
        @ApplicationContext context: Context
    ): SafDataSource {
        return SafDataSource(context)
    }
}