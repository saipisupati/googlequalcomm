package com.roost.app.runtime

import kotlinx.coroutines.delay

/**
 * Demo-mode LLM. Inspects the prompt for context and returns a plausible-looking
 * SnapDragon response. This is not "fake" in a deceptive sense — it's the
 * deterministic fallback the rule engine already encodes, dressed in dragon voice.
 */
class MockLocalLLMEngine : LocalLLMEngine {

    override suspend fun generateBudgetAdvice(prompt: String): String {
        // Tiny synthetic latency so the UI shows a brief "thinking..." state.
        delay(450)

        val lower = prompt.lowercase()
        return when {
            "tired" in lower || "exhausted" in lower ->
                "SnapDragon is tired because you spent past your budget in one category this week. Try a no-spend day tomorrow to recover energy."

            "afford" in lower || "can i" in lower ->
                "You still have room in your weekly budget for a small spend, but watch the category — once it's past 80%, SnapDragon starts losing health."

            "overspend" in lower || "over" in lower ->
                "Your highest spend this week is in Food. Cooking once at home would put SnapDragon back in the green zone."

            "level" in lower || "grow" in lower ->
                "SnapDragon levels up every 100 XP. Logging your spending three days in a row gives a +20 XP streak bonus."

            else ->
                "SnapDragon is paying attention. Keep logging purchases — small steady tracking is what makes the dragon grow."
        }
    }
}
