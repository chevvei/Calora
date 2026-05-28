package com.calora.core

import android.content.Context
import androidx.room.Room
import com.calora.data.local.AppDatabase
import com.calora.data.local.MealRecordDao
import com.calora.data.repository.MealRepository
import com.calora.nutrition.NutritionEstimator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME).build()

    @Provides
    fun provideMealRecordDao(db: AppDatabase): MealRecordDao = db.mealRecordDao()

    @Provides
    @Singleton
    fun provideMealRepository(dao: MealRecordDao): MealRepository = MealRepository(dao)

    @Provides
    @Singleton
    fun provideNutritionEstimator(): NutritionEstimator = NutritionEstimator()
}
