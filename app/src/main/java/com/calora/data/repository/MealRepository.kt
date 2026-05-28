package com.calora.data.repository

import com.calora.data.local.MealRecordDao
import com.calora.data.local.MealRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val dao: MealRecordDao
) {
    fun getRecordsByDate(date: String): Flow<List<MealRecordEntity>> = dao.getByDate(date)

    fun getAllRecords(): Flow<List<MealRecordEntity>> = dao.getAll()

    suspend fun insert(record: MealRecordEntity) = dao.insert(record)

    suspend fun delete(record: MealRecordEntity) = dao.delete(record)
}
