package com.example.roost.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Purchase::class, BudgetCategory::class, RoostState::class, AIAdvice::class],
    version = 1,
    exportSchema = false
)
abstract class RoostDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun roostStateDao(): RoostStateDao
    abstract fun aiAdviceDao(): AIAdviceDao

    companion object {
        @Volatile
        private var INSTANCE: RoostDatabase? = null

        fun getDatabase(context: Context): RoostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoostDatabase::class.java,
                    "roost.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
