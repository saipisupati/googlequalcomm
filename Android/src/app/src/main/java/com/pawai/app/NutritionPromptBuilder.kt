package com.pawai.app

/**
 * Builds the prompt sent to Gemma 4 E2B from the FastVLM food description,
 * the user's pet profile, and a feeding-history summary from EmbeddingGemma.
 *
 * Output is a single instruction-tuned prompt string. Keep it short — Gemma E2B
 * runs on the NPU and we want first-token latency under a second on SM8750.
 */
object NutritionPromptBuilder {

    fun build(
        foodDescription: String,
        petProfile: PetProfile?,
        feedingHistorySummary: String,
    ): String = buildString {
        appendLine("You are a calm, plain-spoken pet nutrition assistant.")
        appendLine("Give the owner a short (3-4 sentence) assessment of this meal.")
        appendLine("End with one of: GOOD / SLIGHTLY_OVER / SIGNIFICANTLY_OVER / UNDER.")
        appendLine()
        appendLine("Pet:")
        if (petProfile == null) {
            appendLine("  (no profile on file)")
        } else {
            appendLine("  name: ${petProfile.name}")
            appendLine("  species: ${petProfile.species}")
            appendLine("  breed: ${petProfile.breed}")
            appendLine("  age: ${petProfile.ageYears}y")
            appendLine("  weight: ${petProfile.weightLbs} lbs")
            appendLine("  daily calorie target: ${petProfile.dailyCalorieTarget}")
            if (petProfile.dietaryRestrictions.isNotEmpty()) {
                appendLine("  restrictions: ${petProfile.dietaryRestrictions.joinToString()}")
            }
        }
        appendLine()
        appendLine("Bowl right now: $foodDescription")
        if (feedingHistorySummary.isNotBlank()) {
            appendLine("Recent feeding context: $feedingHistorySummary")
        }
    }
}
