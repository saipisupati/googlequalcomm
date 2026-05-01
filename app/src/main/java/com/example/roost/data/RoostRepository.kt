package com.example.roost.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository — Single source of truth for all Roost data.
 * All operations are offline / local via Room.
 */
class RoostRepository(private val db: RoostDatabase) {

    private val purchaseDao = db.purchaseDao()
    private val categoryDao = db.budgetCategoryDao()
    private val roostDao = db.roostStateDao()
    private val adviceDao = db.aiAdviceDao()

    // ── Purchases ───────────────────────────────

    suspend fun addPurchase(purchase: Purchase): Long = purchaseDao.insert(purchase)

    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    fun getRecentPurchases(limit: Int = 5): Flow<List<Purchase>> = purchaseDao.getRecentPurchases(limit)

    fun getPurchasesByCategory(category: String): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByCategory(category)

    suspend fun deletePurchase(purchase: Purchase) = purchaseDao.delete(purchase)

    suspend fun clearAllPurchases() = purchaseDao.deleteAll()

    suspend fun resetRoostState() {
        roostDao.insertOrUpdate(RoostState())
    }

    suspend fun getSpentInCategory(category: String): Double {
        return purchaseDao.getSpentInCategory(category, getWeekStart())
    }

    suspend fun getTotalSpentThisWeek(): Double {
        return purchaseDao.getTotalSpentSince(getWeekStart())
    }

    // ── Budget Categories ───────────────────────

    fun getAllCategories(): Flow<List<BudgetCategory>> = categoryDao.getAllCategories()

    suspend fun updateCategoryLimit(name: String, limit: Double) {
        categoryDao.updateLimit(name, limit)
    }

    suspend fun getCategoriesWithSpent(): List<BudgetCategoryWithSpent> {
        val weekStart = getWeekStart()
        val categories = mutableListOf<BudgetCategoryWithSpent>()
        for (cat in Categories.ALL) {
            val budgetCat = categoryDao.getCategory(cat)
            val spent = purchaseDao.getSpentInCategory(cat, weekStart)
            categories.add(
                BudgetCategoryWithSpent(
                    name = cat,
                    weeklyLimit = budgetCat?.weeklyLimit ?: Categories.DEFAULT_LIMITS[cat] ?: 50.0,
                    iconEmoji = budgetCat?.iconEmoji ?: Categories.EMOJIS[cat] ?: "📦",
                    spentAmount = spent
                )
            )
        }
        return categories
    }

    // ── Dragon State ────────────────────────────

    fun getRoostState(): Flow<RoostState?> = roostDao.getRoostState()

    suspend fun getRoostStateOnce(): RoostState =
        roostDao.getRoostStateOnce() ?: RoostState()

    suspend fun updateRoostState(state: RoostState) = roostDao.insertOrUpdate(state)

    // ── AI Advice ───────────────────────────────

    suspend fun saveAdvice(prompt: String, response: String) {
        adviceDao.insert(AIAdvice(prompt = prompt, response = response))
    }

    fun getRecentAdvice(): Flow<List<AIAdvice>> = adviceDao.getRecentAdvice()

    // ── Seed Data ───────────────────────────────

    suspend fun seedIfNeeded() {
        // Seed dragon
        if (roostDao.getRoostStateOnce() == null) {
            roostDao.insertOrUpdate(RoostState())
        }
        // Seed budget categories
        val existing = categoryDao.getCategory(Categories.FOOD)
        if (existing == null) {
            val defaults = Categories.ALL.map { name ->
                BudgetCategory(
                    name = name,
                    weeklyLimit = Categories.DEFAULT_LIMITS[name] ?: 50.0,
                    iconEmoji = Categories.EMOJIS[name] ?: "📦"
                )
            }
            categoryDao.insertAll(defaults)
        }
    }

    // ── Helpers ─────────────────────────────────

    private fun getWeekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
