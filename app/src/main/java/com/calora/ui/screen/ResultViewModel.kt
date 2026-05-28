package com.calora.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calora.data.local.MealRecordEntity
import com.calora.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: MealRepository
) : ViewModel() {

    fun saveRecord(foodName: String, calories: Float, protein: Float, carbs: Float, fat: Float) {
        viewModelScope.launch {
            repository.insert(
                MealRecordEntity(
                    foodName = foodName,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    date = LocalDate.now().toString(),
                    time = LocalTime.now().toString()
                )
            )
        }
    }
}
