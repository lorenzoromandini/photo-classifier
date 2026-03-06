package com.example.photoorganizer.di

import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

/**
 * Hilt module providing WorkManager instance.
 *
 * WorkManager configuration is handled by PhotoOrganizerApplication
 * which implements Configuration.Provider.
 *
 * @see com.example.photoorganizer.PhotoOrganizerApplication
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    /**
     * Provides the WorkManager instance.
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
