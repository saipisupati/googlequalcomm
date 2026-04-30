package com.dragonbudget.app.domain

import com.dragonbudget.app.data.BudgetCategoryEntity
import com.dragonbudget.app.data.Category
import java.util.Calendar

/**
 * Pure budget math. Given the user's purchases and budget limits,
 * compute spending against limits.
 *
 * "This week" is Monday 00:00 in the device's local time zone — matches how a college
 * student thinks about a weekly allowance.
 */
object BudgetEngine {

    data class CategoryStatus(
        val category: Category,
        val weeklyLimit: Double,
        val spent: Double,
    ) {
        val remaining: Double get() = (weeklyLimit - spent).coerceAtLeast(0.0)
        val fractionUsed: Double get() = if (weeklyLimit > 0) (spent / weeklyLimit).coerceAtLeast(0.0) else 0.0
        val isOverBudget: Boolean get() = spent > weeklyLimit
    }

    fun startOfWeekMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Build a status row for each known category given the current budget limits and
     * a function that returns this-week spend per category.
     */
    suspend fun buildStatuses(
        budgets: List<BudgetCategoryEntity>,
        spentThisWeek: suspend (Category) -> Double,
    ): List<CategoryStatus> {
        return budgets.map { budget ->
            val cat = Category.fromName(budget.name)
            CategoryStatus(
                category = cat,
                weeklyLimit = budget.weeklyLimit,
                spent = spentThisWeek(cat),
            )
        }
    }

    fun totalRemaining(statuses: List<CategoryStatus>): Double =
        statuses.sumOf { it.remaining }

    fun overspentCategories(statuses: List<CategoryStatus>): List<CategoryStatus> =
        statuses.filter { it.isOverBudget }
}
