package com.calora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MealRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealRecordDao(): MealRecordDao
}
