package com.roost.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roost.app.AppContainer
import com.roost.app.data.DragonStateEntity
import com.roost.app.data.PurchaseEntity
import com.roost.app.data.Seeder
import com.roost.app.domain.BudgetEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val dragon: DragonStateEntity = Seeder.initialDragon,
    val statuses: List<BudgetEngine.CategoryStatus> = emptyList(),
    val recentPurchases: List<PurchaseEntity> = emptyList(),
) {
    val totalRemaining: Double get() = BudgetEngine.totalRemaining(statuses)
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            container.dragon.observe(),
            container.budgets.observeAll(),
            container.purchases.observeRecent(8),
        ) { dragon, budgets, recent ->
            val weekStart = BudgetEngine.startOfWeekMillis()
            val statuses = BudgetEngine.buildStatuses(budgets) { cat ->
                container.purchases.sumForCategorySince(cat, weekStart)
            }
            HomeUiState(
                dragon = dragon ?: Seeder.initialDragon,
                statuses = statuses,
                recentPurchases = recent,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}
