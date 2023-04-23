package com.phoenixredwolf.diaryapp.presentation.screens.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenixredwolf.diaryapp.data.repository.Diaries
import com.phoenixredwolf.diaryapp.data.repository.MongoDB
import com.phoenixredwolf.diaryapp.model.RequestState
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    init {
        observeAllDiaries()
    }
    
    private fun observeAllDiaries() {
        viewModelScope.launch { 
            MongoDB.getAllDiaries().collect { result ->
                diaries.value = result
                Log.d("Diary data", "From HomeViewModel observeAllDiaries() -> $result")
            }
        }
    }
}