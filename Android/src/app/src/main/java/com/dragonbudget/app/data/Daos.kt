package com.dragonbudget.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PurchaseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM purchases WHERE category = :category AND timestamp >= :sinceMillis")
    suspend fun sumForCategorySince(category: String, sinceMillis: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0) FROM purchases WHERE timestamp >= :sinceMillis")
    suspend fun sumAllSince(sinceMillis: Long): Double

    @Query("DELETE FROM purchases")
    suspend fun deleteAll()
}

@Dao
interface BudgetCategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfMissing(categories: List<BudgetCategoryEntity>)

    @Query("SELECT * FROM budget_categories ORDER BY name")
    fun observeAll(): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories WHERE name = :name")
    suspend fun get(name: String): BudgetCategoryEntity?

    @Update
    suspend fun update(entity: BudgetCategoryEntity)
}

@Dao
interface DragonStateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(state: DragonStateEntity)

    @Query("SELECT * FROM dragon_state WHERE id = 1")
    fun observe(): Flow<DragonStateEntity?>

    @Query("SELECT * FROM dragon_state WHERE id = 1")
    suspend fun get(): DragonStateEntity?

    @Update
    suspend fun update(state: DragonStateEntity)
}

@Dao
interface AiAdviceDao {
    @Insert
    suspend fun insert(advice: AiAdviceEntity): Long

    @Query("SELECT * FROM ai_advice ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AiAdviceEntity>>
}
