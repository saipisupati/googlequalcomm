package com.pawai.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pawai.app.data.entities.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(alert: Alert): Long

    @Query("SELECT * FROM alerts WHERE petId = :petId AND acknowledged = 0 ORDER BY timestamp DESC")
    suspend fun unacknowledged(petId: Long): List<Alert>

    @Query("SELECT * FROM alerts WHERE petId = :petId AND acknowledged = 0 ORDER BY timestamp DESC")
    fun observeUnacknowledged(petId: Long): Flow<List<Alert>>

    @Query("UPDATE alerts SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledge(id: Long)

    @Query("SELECT * FROM alerts WHERE petId = :petId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(petId: Long, limit: Int): List<Alert>

    @Query("SELECT COUNT(*) FROM alerts WHERE petId = :petId AND timestamp BETWEEN :startMs AND :endMs")
    suspend fun countInRange(petId: Long, startMs: Long, endMs: Long): Int
}
