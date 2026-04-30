package com.pawai.app.data

data class BaselineStats(
    val avgDailyPortionGrams: Float,
    val avgMealsPerDay: Float,
    val totalMealsInWindow: Int,
    val windowDays: Int
)
