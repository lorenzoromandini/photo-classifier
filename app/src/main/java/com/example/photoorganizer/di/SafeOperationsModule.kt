package com.example.photoorganizer.di

import android.content.Context
import com.example.photoorganizer.data.local.safe.RetryConfig
import com.example.photoorganizer.data.local.safe.RetryStrategy
import com.example.photoorganizer.data.local.safe.StorageChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module providing safe file operations dependencies.
 * Includes StorageChecker, RetryStrategy, and SafeFileOperations.
 *
 * @see StorageChecker
 * @see RetryStrategy
 */
@Module
@InstallIn(SingletonComponent::class)
object SafeOperationsModule {

    /**
     * Provides StorageChecker for storage space monitoring.
     */
    @Provides
    @Singleton
    fun provideStorageChecker(
        @ApplicationContext context: Context
    ): StorageChecker {
        return StorageChecker(context)
    }

    /**
     * Provides RetryStrategy with default configuration.
     */
    @Provides
    @Singleton
    fun provideRetryStrategy(): RetryStrategy {
        return RetryStrategy(RetryConfig())
    }

    /**
     * Provides RetryStrategy with conservative configuration.
     * Use for critical operations that need slower, more careful retries.
     */
    @Provides
    @Singleton
    @ConservativeRetry
    fun provideConservativeRetryStrategy(): RetryStrategy {
        return RetryStrategy(RetryConfig.CONSERVATIVE)
    }

    /**
     * Provides RetryStrategy with aggressive configuration.
     * Use for operations that should recover quickly.
     */
    @Provides
    @Singleton
    @AggressiveRetry
    fun provideAggressiveRetryStrategy(): RetryStrategy {
        return RetryStrategy(RetryConfig.AGGRESSIVE)
    }
}

/**
 * Qualifier for conservative retry strategy.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ConservativeRetry

/**
 * Qualifier for aggressive retry strategy.
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class AggressiveRetry
