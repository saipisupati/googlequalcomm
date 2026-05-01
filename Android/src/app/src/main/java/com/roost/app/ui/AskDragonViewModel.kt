package com.roost.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roost.app.AppContainer
import com.roost.app.data.Seeder
import com.roost.app.domain.BudgetEngine
import com.roost.app.runtime.PromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AskDragonUiState(
    val question: String = "",
    val answer: String = "",
    val asking: Boolean = false,
)

class AskDragonViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AskDragonUiState())
    val state: StateFlow<AskDragonUiState> = _state.asStateFlow()

    fun onQuestionChange(v: String) = _state.update { it.copy(question = v) }

    fun onPresetSelected(preset: String) {
        _state.update { it.copy(question = preset) }
        ask(preset)
    }

    fun onAsk() {
        val q = _state.value.question.trim()
        if (q.isEmpty()) return
        ask(q)
    }

    private fun ask(question: String) {
        viewModelScope.launch {
            _state.update { it.copy(asking = true, answer = "") }

            val dragon = container.dragon.get() ?: Seeder.initialDragon
            // Read budgets synchronously by hitting the DAO once per category — cheap and
            // simpler than wiring a one-shot Flow collector here.
            val budgetEntities = com.roost.app.data.Category.entries.mapNotNull { cat ->
                container.budgets.get(cat)
            }
            val weekStart = BudgetEngine.startOfWeekMillis()
            val statuses = BudgetEngine.buildStatuses(budgetEntities) { cat ->
                container.purchases.sumForCategorySince(cat, weekStart)
            }

            val prompt = PromptBuilder.build(dragon, statuses, question)
            val response = container.llm.generateBudgetAdvice(prompt)
            container.advice.insert(prompt = prompt, response = response)

            _state.update { it.copy(answer = response, asking = false) }
        }
    }
}
