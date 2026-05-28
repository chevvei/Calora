package com.calora.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MealRecordEntity)

    @Delete
    suspend fun delete(record: MealRecordEntity)

    @Query("SELECT * FROM meal_records WHERE date = :date ORDER BY time DESC")
    fun getByDate(date: String): Flow<List<MealRecordEntity>>

    @Query("SELECT * FROM meal_records ORDER BY date DESC, time DESC")
    fun getAll(): Flow<List<MealRecordEntity>>
}
