package com.pawai.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pawai.app.data.dao.AlertDao
import com.pawai.app.data.dao.MealDao
import com.pawai.app.data.dao.MonthlySummaryDao
import com.pawai.app.data.dao.PetDao
import com.pawai.app.data.entities.Alert
import com.pawai.app.data.entities.Meal
import com.pawai.app.data.entities.MonthlySummary
import com.pawai.app.data.entities.Pet

@Database(
    entities = [Pet::class, Meal::class, Alert::class, MonthlySummary::class],
    version = 1,
    exportSchema = false
)
abstract class PawDb : RoomDatabase() {

    abstract fun petDao(): PetDao
    abstract fun mealDao(): MealDao
    abstract fun alertDao(): AlertDao
    abstract fun monthlySummaryDao(): MonthlySummaryDao

    companion object {
        private const val DB_NAME = "paw_db"

        @Volatile
        private var instance: PawDb? = null

        fun getInstance(context: Context): PawDb {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PawDb::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
