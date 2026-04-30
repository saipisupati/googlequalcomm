package com.pawai.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId", "timestamp"])]
)
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val petId: Long,
    val timestamp: Long,
    val foodName: String,
    val brand: String?,
    val portionGrams: Float,
    val confidence: Float,
    val assessment: String,
    val flagSeverity: Int,
    val wasLogged: Boolean
)
