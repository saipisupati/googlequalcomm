package com.roost.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PurchaseEntity::class,
        BudgetCategoryEntity::class,
        DragonStateEntity::class,
        AiAdviceEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RoostDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun dragonStateDao(): DragonStateDao
    abstract fun aiAdviceDao(): AiAdviceDao

    companion object {
        @Volatile private var instance: RoostDatabase? = null

        fun get(context: Context): RoostDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoostDatabase::class.java,
                    "roost.db",
                ).build().also { instance = it }
            }
        }
    }
}
