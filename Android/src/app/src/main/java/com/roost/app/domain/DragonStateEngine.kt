package com.roost.app.domain

import com.roost.app.data.DragonStateEntity

/**
 * Pure rules for how the dragon's state changes in response to a single purchase.
 * Spec-faithful to the design doc: XP for under-budget spend, health hit for over-budget,
 * mood derived from health bucket.
 *
 * Keep deterministic. The LLM explains decisions; it never makes them.
 */
object DragonStateEngine {

    data class PurchaseImpact(
        val xpDelta: Int,
        val healthDelta: Int,
        val moodAfter: String,
        val leveledUp: Boolean,
    )

    /**
     * Apply a purchase to the dragon. Caller passes the post-purchase fraction-of-weekly-budget
     * for the affected category so this function stays free of DB access.
     */
    fun applyPurchase(
        current: DragonStateEntity,
        categoryFractionAfter: Double, // e.g. 0.62 means 62% of weekly limit used after this buy
    ): DragonStateEntity {
        val (xpDelta, healthDelta) = when {
            categoryFractionAfter <= 0.50 -> 5 to 0      // safe spend → small XP reward
            categoryFractionAfter <= 0.80 -> 1 to 0      // alert zone, tiny xp
            categoryFractionAfter <= 1.00 -> 0 to -5     // worried zone, small health hit
            else -> 0 to -15                              // over budget, real penalty
        }

        val newXp = (current.xp + xpDelta).coerceAtLeast(0)
        val newHealth = (current.health + healthDelta).coerceIn(0, 100)
        val newLevel = 1 + (newXp / 100)
        val leveledUp = newLevel > current.level
        val mood = moodFor(newHealth, categoryFractionAfter)

        return current.copy(
            xp = newXp,
            health = newHealth,
            level = newLevel,
            mood = mood,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    /**
     * Reward consistent tracking. Caller invokes this once per day after the user logs at least
     * one purchase. Logic: 3-day streak grants +10 health and +20 XP.
     */
    fun applyDailyStreak(current: DragonStateEntity, didLogToday: Boolean): DragonStateEntity {
        val newStreak = if (didLogToday) current.streakDays + 1 else 0
        val (healthBonus, xpBonus) = if (newStreak > 0 && newStreak % 3 == 0) 10 to 20 else 0 to 0
        val newHealth = (current.health + healthBonus).coerceIn(0, 100)
        val newXp = current.xp + xpBonus
        val newLevel = 1 + (newXp / 100)
        return current.copy(
            health = newHealth,
            xp = newXp,
            level = newLevel,
            streakDays = newStreak,
            mood = moodFor(newHealth, lastCategoryFraction = null),
            lastUpdated = System.currentTimeMillis(),
        )
    }

    /**
     * Mood is mostly a function of health bucket, but a fresh over-budget purchase trumps that
     * with "Tired" / "Worried" so the UI reflects the most recent action.
     */
    private fun moodFor(health: Int, lastCategoryFraction: Double?): String {
        if (lastCategoryFraction != null) {
            if (lastCategoryFraction > 1.00) return "Tired"
            if (lastCategoryFraction > 0.80) return "Worried"
        }
        return when {
            health >= 85 -> "Energized"
            health >= 60 -> "Stable"
            health >= 35 -> "Worried"
            else -> "Exhausted"
        }
    }
}
