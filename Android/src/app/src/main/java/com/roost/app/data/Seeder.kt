package com.roost.app.data

/**
 * Default budgets seeded on first launch. Tuned to feel realistic for a college-age user
 * so the demo numbers look plausible. Per-week limits in USD.
 */
object Seeder {
    val defaultBudgets: List<BudgetCategoryEntity> = listOf(
        BudgetCategoryEntity(name = Category.Food.name, weeklyLimit = 60.0),
        BudgetCategoryEntity(name = Category.Groceries.name, weeklyLimit = 80.0),
        BudgetCategoryEntity(name = Category.Gas.name, weeklyLimit = 40.0),
        BudgetCategoryEntity(name = Category.School.name, weeklyLimit = 50.0),
        BudgetCategoryEntity(name = Category.Entertainment.name, weeklyLimit = 50.0),
        BudgetCategoryEntity(name = Category.Shopping.name, weeklyLimit = 60.0),
        BudgetCategoryEntity(name = Category.Household.name, weeklyLimit = 30.0),
        BudgetCategoryEntity(name = Category.Other.name, weeklyLimit = 30.0),
    )

    val initialDragon = DragonStateEntity(
        id = 1,
        name = "SnapDragon",
        health = 80,
        xp = 0,
        level = 1,
        mood = "Curious",
        streakDays = 0,
        lastUpdated = System.currentTimeMillis(),
    )
}
