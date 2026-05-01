package com.roost.app.runtime

/**
 * Interface for a local on-device LLM that produces short budgeting advice.
 * Implementations: MockLocalLLMEngine (canned), LiteRtGemmaEngine (real).
 */
interface LocalLLMEngine {
    suspend fun generateBudgetAdvice(prompt: String): String
}
