package com.pawai.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pawai.app.data.entities.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(pet: Pet): Long

    @Update
    suspend fun update(pet: Pet)

    @Delete
    suspend fun delete(pet: Pet)

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Pet?

    @Query("SELECT * FROM pets ORDER BY createdAt ASC")
    suspend fun getAll(): List<Pet>

    @Query("SELECT * FROM pets ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Pet>>
}
