package com.example.roost.viewmodel

import androidx.lifecycle.*
import com.example.roost.AppContainer
import com.example.roost.data.AIAdvice
import com.example.roost.engine.PromptBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AskRoostViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val recentAdvice: StateFlow<List<AIAdvice>> = repo.getRecentAdvice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun askRoost(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _response.value = ""

            try {
                val dragon = repo.getRoostStateOnce()
                val categories = repo.getCategoriesWithSpent()

                // Build the prompt
                val prompt = PromptBuilder.buildAdvicePrompt(dragon, categories, question)

                // Generate response via LiteRT-LM Gemma on NPU
                val answer = container.llmEngine.generateBudgetAdvice(prompt)

                _response.value = answer

                // Save to history
                repo.saveAdvice(question, answer)
            } catch (e: Exception) {
                _response.value = "SnapDragon encountered an error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AskRoostViewModel(container) as T
        }
    }
}
