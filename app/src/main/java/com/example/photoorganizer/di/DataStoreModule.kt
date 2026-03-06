package com.example.photoorganizer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.example.photoorganizer.data.local.datastore.UserPreferences
import com.example.photoorganizer.data.local.datastore.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Proto DataStore dependencies.
 * Configures the DataStore with custom serializer for UserPreferences.
 *
 * @see UserPreferences
 * @see UserPreferencesSerializer
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides the singleton DataStore for UserPreferences.
     * Uses DataStoreFactory for custom configuration.
     *
     * @param context Application context for file storage
     * @return DataStore instance backed by protobuf file
     */
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<UserPreferences> {
        return DataStoreFactory.create(
            serializer = UserPreferencesSerializer,
            produceFile = { context.dataStoreFile("user_prefs.pb") }
        )
    }
}
