package com.pawai.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val species: String,
    val breed: String,
    val ageYears: Float,
    val weightKg: Float,
    val dailyCalorieTarget: Int,
    val dietaryRestrictions: String,
    val createdAt: Long
)
