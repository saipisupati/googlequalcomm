package com.pawai.app.data

import androidx.room.withTransaction
import com.pawai.app.data.entities.Alert
import com.pawai.app.data.entities.Meal
import com.pawai.app.data.entities.MonthlySummary
import com.pawai.app.data.entities.Pet
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.max

class PawRepository(private val db: PawDb) {

    private val petDao = db.petDao()
    private val mealDao = db.mealDao()
    private val alertDao = db.alertDao()
    private val monthlySummaryDao = db.monthlySummaryDao()

    suspend fun createPet(pet: Pet): Long = petDao.insert(pet)

    suspend fun getPet(id: Long): Pet? = petDao.getById(id)

    suspend fun getAllPets(): List<Pet> = petDao.getAll()

    fun observePets(): Flow<List<Pet>> = petDao.observeAll()

    suspend fun logMeal(meal: Meal): Long = mealDao.insert(meal)

    suspend fun getRecentMeals(petId: Long, days: Int): List<Meal> {
        val sinceMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return mealDao.inDateRange(petId, sinceMs, System.currentTimeMillis())
    }

    fun getMealHistoryFlow(petId: Long): Flow<List<Meal>> =
        mealDao.observeRecent(petId, MEAL_HISTORY_LIMIT)

    fun getActiveAlerts(petId: Long): Flow<List<Alert>> = alertDao.observeUnacknowledged(petId)

    suspend fun raiseAlert(alert: Alert): Long = alertDao.insert(alert)

    suspend fun acknowledgeAlert(id: Long) = alertDao.acknowledge(id)

    suspend fun get14DayBaseline(petId: Long): BaselineStats {
        val windowDays = 14
        val now = System.currentTimeMillis()
        val sinceMs = now - TimeUnit.DAYS.toMillis(windowDays.toLong())

        val totalMeals = mealDao.countSince(petId, sinceMs)
        val mealsInWindow = mealDao.inDateRange(petId, sinceMs, now)
        val totalGrams = mealsInWindow.sumOf { it.portionGrams.toDouble() }.toFloat()
        val avgDailyPortionGrams = totalGrams / windowDays.toFloat()
        val avgMealsPerDay = totalMeals.toFloat() / windowDays.toFloat()

        return BaselineStats(
            avgDailyPortionGrams = avgDailyPortionGrams,
            avgMealsPerDay = avgMealsPerDay,
            totalMealsInWindow = totalMeals,
            windowDays = windowDays
        )
    }

    /**
     * Roll up meals older than [olderThanDays] into per-(pet,year,month) summaries, then delete the
     * source rows. Wrapped in a single transaction so the rollup and the delete either both happen
     * or neither does — the originals are never lost without a summary in their place.
     */
    suspend fun pruneOldData(olderThanDays: Int = 90): PruneResult {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
        return db.withTransaction {
            val old = mealDao.olderThan(cutoff)
            if (old.isEmpty()) {
                return@withTransaction PruneResult(0, 0)
            }

            val grouped = old.groupBy { Triple(it.petId, it.yearOf(), it.monthOf()) }
            for ((key, meals) in grouped) {
                val (petId, year, month) = key
                val totalMeals = meals.size
                val avgPortion = meals.map { it.portionGrams }.average().toFloat()

                val (startMs, endMs) = monthBounds(year, month)
                val totalAlerts = alertDao.countInRange(petId, startMs, endMs)

                monthlySummaryDao.insert(
                    MonthlySummary(
                        petId = petId,
                        year = year,
                        month = month,
                        totalMeals = totalMeals,
                        avgPortionGrams = avgPortion,
                        totalAlerts = totalAlerts,
                        avgEmbedding = ByteArray(EMBEDDING_BYTES)
                    )
                )
            }

            val deleted = mealDao.deleteOlderThan(cutoff)
            PruneResult(rolledUpMonths = grouped.size, mealsDeleted = max(deleted, 0))
        }
    }

    data class PruneResult(val rolledUpMonths: Int, val mealsDeleted: Int)

    private fun Meal.yearOf(): Int = calendar(timestamp).get(Calendar.YEAR)
    private fun Meal.monthOf(): Int = calendar(timestamp).get(Calendar.MONTH) + 1

    private fun calendar(ms: Long): Calendar =
        Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = ms }

    private fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.clear()
        cal.set(year, month - 1, 1, 0, 0, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        return start to end
    }

    companion object {
        private const val MEAL_HISTORY_LIMIT = 200
        private const val EMBEDDING_BYTES = 768 * 4
    }
}
