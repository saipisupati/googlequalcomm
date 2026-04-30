package com.dragonbudget.app

import android.content.Context
import com.dragonbudget.app.data.AdviceRepository
import com.dragonbudget.app.data.BudgetRepository
import com.dragonbudget.app.data.DragonBudgetDatabase
import com.dragonbudget.app.data.DragonRepository
import com.dragonbudget.app.data.PurchaseRepository
import com.dragonbudget.app.data.Seeder
import com.dragonbudget.app.runtime.DemoMode
import com.dragonbudget.app.runtime.LiteRtGemmaEngine
import com.dragonbudget.app.runtime.LiteRtVisionEngine
import com.dragonbudget.app.runtime.LocalLLMEngine
import com.dragonbudget.app.runtime.MockLocalLLMEngine
import com.dragonbudget.app.runtime.MockReceiptVisionEngine
import com.dragonbudget.app.runtime.ReceiptVisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI container. Created once in DragonBudgetApplication, held for app lifetime.
 *
 * Why no Hilt: 18-hour hackathon, four files of plumbing isn't worth a 30-min Hilt setup.
 * Repos and engines are stateless / single-instance — manual wiring is fine.
 */
class AppContainer(context: Context) {

    private val db = DragonBudgetDatabase.get(context)

    val purchases = PurchaseRepository(db.purchaseDao())
    val budgets = BudgetRepository(db.budgetCategoryDao())
    val dragon = DragonRepository(db.dragonStateDao())
    val advice = AdviceRepository(db.aiAdviceDao())

    val llm: LocalLLMEngine =
        if (DemoMode.ENABLED) MockLocalLLMEngine() else LiteRtGemmaEngine(context)

    val vision: ReceiptVisionEngine =
        if (DemoMode.ENABLED) MockReceiptVisionEngine() else LiteRtVisionEngine()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            budgets.seedIfEmpty(Seeder.defaultBudgets)
            dragon.seedIfEmpty(Seeder.initialDragon)
        }
    }
}
