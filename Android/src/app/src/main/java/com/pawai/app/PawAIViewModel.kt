package com.pawai.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PawAIUiState {
    data object Idle : PawAIUiState
    data object Scanning : PawAIUiState
    data class Confirming(val detectedFood: String) : PawAIUiState
    data class Assessing(val streamingText: String) : PawAIUiState
    data class Logged(val finalAssessment: String) : PawAIUiState
}

/**
 * Orchestrates the FastVLM -> Gemma pipeline with strict NPU memory scheduling:
 * the two large models never sit in memory at the same time. EmbeddingGemma stays resident.
 *
 * Pipeline:
 *   1. startScan(): load FastVLM, run on current camera frame, unload.
 *   2. confirmAndAssess(): pull feeding history from EmbeddingGemma, build Gemma prompt,
 *      load Gemma, stream, unload.
 *   3. logMeal(): embed the meal description and append to local history.
 */
class PawAIViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PawAIUiState>(PawAIUiState.Idle)
    val uiState: StateFlow<PawAIUiState> = _uiState.asStateFlow()

    private var lastFoodDescription: String = ""

    fun startScan() {
        viewModelScope.launch {
            _uiState.value = PawAIUiState.Scanning
            // TODO: load FastVLM, capture latest CameraX frame, run inference, unload.
            val detected = runFastVlm()
            lastFoodDescription = detected
            _uiState.value = PawAIUiState.Confirming(detected)
        }
    }

    fun confirmAndAssess() {
        viewModelScope.launch {
            _uiState.value = PawAIUiState.Assessing(streamingText = "")
            // TODO: PetProfileStore.getProfile() + recent feeding history.
            val prompt = NutritionPromptBuilder.build(
                foodDescription = lastFoodDescription,
                petProfile = null, // TODO
                feedingHistorySummary = "", // TODO from EmbeddingGemma
            )
            val finalText = streamGemma(prompt)
            _uiState.value = PawAIUiState.Logged(finalText)
            // TODO: PetProfileStore.logMeal(lastFoodDescription, finalText)
        }
    }

    fun reset() {
        _uiState.value = PawAIUiState.Idle
    }

    private suspend fun runFastVlm(): String {
        // TODO: real inference via LiteRT-LM on FastVLM-0.5B.qualcomm.sm8750.litertlm
        return "Royal Canin Adult kibble, ~1.5 cups, one Milk-Bone treat"
    }

    private suspend fun streamGemma(prompt: String): String {
        // TODO: real streaming via LiteRT-LM on gemma-4-E2B-it.litertlm.
        // Update _uiState as tokens arrive: PawAIUiState.Assessing(streamingText = accumulated).
        return "Stub assessment for prompt:\n$prompt"
    }
}
