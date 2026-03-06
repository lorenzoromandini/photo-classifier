package com.example.photoorganizer.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photoorganizer.data.local.database.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CategoryEntity.
 * Provides reactive queries using Kotlin Flow for UI observation.
 */
@Dao
interface CategoryDao {

    /**
     * Get all categories as a reactive Flow.
     * Emits new list whenever categories change.
     */
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CategoryEntity>>

    /**
     * Get a category by its ID (suspending).
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    /**
     * Insert or replace a category.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    /**
     * Delete a category.
     */
    @Delete
    suspend fun delete(category: CategoryEntity)

    /**
     * Get total count of categories.
     * Used to enforce 10 category limit.
     */
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    /**
     * Check if a category exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
