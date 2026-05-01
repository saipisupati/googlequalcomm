package com.roost.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roost.app.AppContainer
import com.roost.app.data.Category
import com.roost.app.data.PurchaseEntity
import com.roost.app.data.Seeder
import com.roost.app.domain.BudgetEngine
import com.roost.app.domain.DragonStateEngine
import com.roost.app.runtime.PurchaseDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddPurchaseUiState(
    val merchant: String = "",
    val amount: String = "",
    val category: Category = Category.Food,
    val note: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val scanning: Boolean = false,
    val capturedImageUri: Uri? = null,
)

class AddPurchaseViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AddPurchaseUiState())
    val state: StateFlow<AddPurchaseUiState> = _state.asStateFlow()

    fun onMerchantChange(v: String) = _state.update { it.copy(merchant = v) }
    fun onAmountChange(v: String) = _state.update { it.copy(amount = v.filter { ch -> ch.isDigit() || ch == '.' }) }
    fun onCategoryChange(v: Category) = _state.update { it.copy(category = v) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }

    /**
     * Called by MainActivity after CameraScreen captures a photo.
     * Kicks off vision inference and fills the form fields with the result.
     */
    fun onPhotoCaptured(uri: Uri) {
        _state.update { it.copy(capturedImageUri = uri, scanning = true) }
        viewModelScope.launch {
            val draft: PurchaseDraft = runCatching {
                container.vision.extractPurchaseFromImage(imageUri = uri)
            }.getOrElse { err ->
                // Vision failed (model missing, parse error, etc.). Fall back to a sensible default
                // so the user can still edit and save. Same UX as a failed-to-recognize scan.
                PurchaseDraft(
                    merchant = "",
                    amount = 0.0,
                    suggestedCategory = Category.Other,
                    confidence = 0f,
                )
            }
            _state.update {
                it.copy(
                    merchant = draft.merchant,
                    amount = if (draft.amount > 0) "%.2f".format(draft.amount) else "",
                    category = draft.suggestedCategory,
                    scanning = false,
                )
            }
        }
    }

    fun onSave() {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: return
        if (s.merchant.isBlank() || amount <= 0.0) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }

            val purchase = PurchaseEntity(
                merchant = s.merchant.trim(),
                amount = amount,
                category = s.category.name,
                note = s.note.trim().takeIf { it.isNotEmpty() },
                timestamp = System.currentTimeMillis(),
            )
            container.purchases.insert(purchase)

            // Recompute this category's fraction-of-weekly-budget AFTER the new purchase,
            // then push that into the dragon state engine.
            val weekStart = BudgetEngine.startOfWeekMillis()
            val budget = container.budgets.get(s.category)?.weeklyLimit ?: 0.0
            val spent = container.purchases.sumForCategorySince(s.category, weekStart)
            val fraction = if (budget > 0) spent / budget else 0.0

            val current = container.dragon.get() ?: Seeder.initialDragon
            val updated = DragonStateEngine.applyPurchase(current, fraction)
            container.dragon.update(updated)

            _state.update { AddPurchaseUiState(saved = true) }
        }
    }

    fun acknowledgeSaved() = _state.update { it.copy(saved = false) }
}
