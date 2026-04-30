package com.pawai.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "monthly_summaries",
    indices = [Index(value = ["petId", "year", "month"], unique = true)]
)
data class MonthlySummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val petId: Long,
    val year: Int,
    val month: Int,
    val totalMeals: Int,
    val avgPortionGrams: Float,
    val totalAlerts: Int,
    val avgEmbedding: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MonthlySummary) return false
        return id == other.id &&
            petId == other.petId &&
            year == other.year &&
            month == other.month &&
            totalMeals == other.totalMeals &&
            avgPortionGrams == other.avgPortionGrams &&
            totalAlerts == other.totalAlerts &&
            avgEmbedding.contentEquals(other.avgEmbedding)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + petId.hashCode()
        result = 31 * result + year
        result = 31 * result + month
        result = 31 * result + totalMeals
        result = 31 * result + avgPortionGrams.hashCode()
        result = 31 * result + totalAlerts
        result = 31 * result + avgEmbedding.contentHashCode()
        return result
    }
}
