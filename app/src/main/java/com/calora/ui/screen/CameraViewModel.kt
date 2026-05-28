package com.calora.ui.screen

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calora.inference.FoodClassifier
import com.calora.inference.FoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val foodClassifier: FoodClassifier
) : ViewModel() {

    private val _result = MutableStateFlow<FoodResult?>(null)
    val result: StateFlow<FoodResult?> = _result.asStateFlow()

    fun classifyFood(imageUri: Uri, onResult: (FoodResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                foodClassifier.classify(imageUri)
            }
            _result.value = result
            onResult(result)
        }
    }
}
