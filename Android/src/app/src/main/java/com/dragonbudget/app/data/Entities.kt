package com.dragonbudget.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val category: String,        // stored as Category.name
    val note: String? = null,
    val timestamp: Long,         // epoch millis
)

@Entity(tableName = "budget_categories")
data class BudgetCategoryEntity(
    @PrimaryKey val name: String,    // matches Category.name
    val weeklyLimit: Double,
)

/**
 * Single-row table holding the dragon's current state.
 * id is hardcoded to 1 — the engine reads/writes row 1 only.
 */
@Entity(tableName = "dragon_state")
data class DragonStateEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "SnapDragon",
    val health: Int = 80,
    val xp: Int = 0,
    val level: Int = 1,
    val mood: String = "Curious",
    val streakDays: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
)

/**
 * Persisted advice generations. Lets the UI replay the last response on cold start
 * and lets us build a "things SnapDragon told you" history later.
 */
@Entity(tableName = "ai_advice")
data class AiAdviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val response: String,
    val timestamp: Long,
)
