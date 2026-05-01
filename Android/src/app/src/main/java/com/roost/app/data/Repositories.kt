package com.roost.app.data

import kotlinx.coroutines.flow.Flow

/**
 * Thin wrappers over DAOs. The repositories don't add logic — domain rules live in
 * BudgetEngine and DragonStateEngine. Repos exist so ViewModels can be unit-tested
 * by swapping in fakes.
 */

class PurchaseRepository(private val dao: PurchaseDao) {
    fun observeAll(): Flow<List<PurchaseEntity>> = dao.observeAll()
    fun observeRecent(limit: Int = 10): Flow<List<PurchaseEntity>> = dao.observeRecent(limit)
    suspend fun insert(p: PurchaseEntity): Long = dao.insert(p)
    suspend fun sumForCategorySince(category: Category, sinceMillis: Long): Double =
        dao.sumForCategorySince(category.name, sinceMillis)
    suspend fun sumAllSince(sinceMillis: Long): Double = dao.sumAllSince(sinceMillis)
}

class BudgetRepository(private val dao: BudgetCategoryDao) {
    fun observeAll(): Flow<List<BudgetCategoryEntity>> = dao.observeAll()
    suspend fun seedIfEmpty(defaults: List<BudgetCategoryEntity>) = dao.insertAllIfMissing(defaults)
    suspend fun get(category: Category): BudgetCategoryEntity? = dao.get(category.name)
}

class DragonRepository(private val dao: DragonStateDao) {
    fun observe(): Flow<DragonStateEntity?> = dao.observe()
    suspend fun seedIfEmpty(initial: DragonStateEntity) = dao.insertIfMissing(initial)
    suspend fun get(): DragonStateEntity? = dao.get()
    suspend fun update(state: DragonStateEntity) = dao.update(state)
}

class AdviceRepository(private val dao: AiAdviceDao) {
    fun observeRecent(limit: Int = 20): Flow<List<AiAdviceEntity>> = dao.observeRecent(limit)
    suspend fun insert(prompt: String, response: String): Long =
        dao.insert(AiAdviceEntity(prompt = prompt, response = response, timestamp = System.currentTimeMillis()))
}
