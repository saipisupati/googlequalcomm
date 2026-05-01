package com.roost.app

import android.content.Context
import com.roost.app.data.AdviceRepository
import com.roost.app.data.BudgetRepository
import com.roost.app.data.RoostDatabase
import com.roost.app.data.DragonRepository
import com.roost.app.data.PurchaseRepository
import com.roost.app.data.Seeder
import com.roost.app.runtime.DemoMode
import com.roost.app.runtime.FallbackLLMEngine
import com.roost.app.runtime.FallbackVisionEngine
import com.roost.app.runtime.LiteRtGemmaEngine
import com.roost.app.runtime.LiteRtVisionEngine
import com.roost.app.runtime.LocalLLMEngine
import com.roost.app.runtime.MockLocalLLMEngine
import com.roost.app.runtime.MockReceiptVisionEngine
import com.roost.app.runtime.ReceiptVisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI container. Created once in RoostApplication, held for app lifetime.
 *
 * Why no Hilt: 18-hour hackathon, four files of plumbing isn't worth a 30-min Hilt setup.
 * Repos and engines are stateless / single-instance — manual wiring is fine.
 */
class AppContainer(context: Context) {

    private val db = RoostDatabase.get(context)

    val purchases = PurchaseRepository(db.purchaseDao())
    val budgets = BudgetRepository(db.budgetCategoryDao())
    val dragon = DragonRepository(db.dragonStateDao())
    val advice = AdviceRepository(db.aiAdviceDao())

    val llm: LocalLLMEngine = run {
        val mock = MockLocalLLMEngine()
        if (DemoMode.FORCE_MOCK_LLM) mock
        else FallbackLLMEngine(primary = LiteRtGemmaEngine(context), secondary = mock)
    }

    val vision: ReceiptVisionEngine = run {
        val mock = MockReceiptVisionEngine()
        if (DemoMode.FORCE_MOCK_VISION) mock
        else FallbackVisionEngine(primary = LiteRtVisionEngine(context), secondary = mock)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            budgets.seedIfEmpty(Seeder.defaultBudgets)
            dragon.seedIfEmpty(Seeder.initialDragon)
        }
    }
}
