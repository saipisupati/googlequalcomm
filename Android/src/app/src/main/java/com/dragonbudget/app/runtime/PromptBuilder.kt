package com.dragonbudget.app.runtime

import com.dragonbudget.app.data.DragonStateEntity
import com.dragonbudget.app.domain.BudgetEngine

/**
 * Builds Gemma prompts grounded in the user's local data. The prompt is the only thing
 * that varies — the model itself is generic. Keep it short so first-token latency is low.
 */
object PromptBuilder {

    fun build(
        dragon: DragonStateEntity,
        statuses: List<BudgetEngine.CategoryStatus>,
        userQuestion: String,
    ): String = buildString {
        appendLine("You are SnapDragon, an offline budgeting companion.")
        appendLine("Use only the local spending data below. Do not invent transactions.")
        appendLine("Keep response under 40 words. Friendly, short, action-oriented.")
        appendLine()
        appendLine("Dragon state:")
        appendLine("  Health: ${dragon.health}/100")
        appendLine("  Mood: ${dragon.mood}")
        appendLine("  Level: ${dragon.level}")
        appendLine("  XP: ${dragon.xp}")
        appendLine()
        appendLine("Budget summary (this week):")
        statuses.forEach { s ->
            val pct = (s.fractionUsed * 100).toInt()
            appendLine("  ${s.category.displayName}: \$${"%.2f".format(s.spent)} / \$${"%.2f".format(s.weeklyLimit)} (${pct}%)")
        }
        appendLine()
        appendLine("User question: $userQuestion")
        appendLine()
        append("Answer:")
    }
}
