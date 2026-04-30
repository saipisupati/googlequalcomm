package com.pawai.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pawai.app.data.entities.MonthlySummary

@Dao
interface MonthlySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: MonthlySummary): Long

    @Query("SELECT * FROM monthly_summaries WHERE petId = :petId ORDER BY year ASC, month ASC")
    suspend fun getForPet(petId: Long): List<MonthlySummary>

    @Query(
        "SELECT * FROM monthly_summaries WHERE petId = :petId " +
            "AND (year * 100 + month) BETWEEN :startYearMonth AND :endYearMonth " +
            "ORDER BY year ASC, month ASC"
    )
    suspend fun getInRange(petId: Long, startYearMonth: Int, endYearMonth: Int): List<MonthlySummary>
}
