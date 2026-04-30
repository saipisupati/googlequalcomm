package com.pawai.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pawai.app.data.entities.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(meal: Meal): Long

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Meal?

    @Query("SELECT * FROM meals WHERE petId = :petId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(petId: Long, limit: Int): List<Meal>

    @Query(
        "SELECT * FROM meals WHERE petId = :petId AND timestamp BETWEEN :startMs AND :endMs " +
            "ORDER BY timestamp ASC"
    )
    suspend fun inDateRange(petId: Long, startMs: Long, endMs: Long): List<Meal>

    @Query("SELECT * FROM meals WHERE petId = :petId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(petId: Long, limit: Int): Flow<List<Meal>>

    @Query("SELECT AVG(portionGrams) FROM meals WHERE petId = :petId AND timestamp >= :sinceMs")
    suspend fun averagePortionSince(petId: Long, sinceMs: Long): Float?

    @Query("SELECT COUNT(*) FROM meals WHERE petId = :petId AND timestamp >= :sinceMs")
    suspend fun countSince(petId: Long, sinceMs: Long): Int

    @Query("DELETE FROM meals WHERE timestamp < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT * FROM meals WHERE timestamp < :cutoffMs ORDER BY petId, timestamp ASC")
    suspend fun olderThan(cutoffMs: Long): List<Meal>
}
