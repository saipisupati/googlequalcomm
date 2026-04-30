package com.pawai.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alerts",
    foreignKeys = [
        ForeignKey(
            entity = Pet::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId", "acknowledged"])]
)
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val petId: Long,
    val timestamp: Long,
    val alertType: String,
    val severity: Int,
    val title: String,
    val description: String,
    val relatedMealId: Long?,
    val acknowledged: Boolean = false
)
