package com.phoenixredwolf.home

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.phoenixredwolf.mongo.database.ImageToDeleteDao
import com.phoenixredwolf.mongo.repository.Diaries
import com.phoenixredwolf.mongo.repository.MongoDB.getAllDiaries
import com.phoenixredwolf.mongo.repository.MongoDB.getFilteredDiaries
import com.phoenixredwolf.util.connectivity.ConnectivityObserver
import com.phoenixredwolf.util.connectivity.NetworkConnectivityObserver
import com.phoenixredwolf.util.model.RequestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val connectivity: NetworkConnectivityObserver,
    private val imageToDeleteDao: ImageToDeleteDao
): ViewModel() {

    private lateinit var allDiariesJob: Job
    private lateinit var filteredDiariesJob: Job

    private var network by mutableStateOf(ConnectivityObserver.Status.Unavailable)
    
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    var dateIsSelected by mutableStateOf(false)
        private set

    init {
        getDiaries()
        viewModelScope.launch {
            connectivity.observe().collect { network = it }
        }
    }

    fun getDiaries(zonedDateTime: ZonedDateTime? = null) {
        dateIsSelected = zonedDateTime != null
        diaries.value = RequestState.Loading
        if (dateIsSelected && zonedDateTime != null) {
            observeFilteredDiaries(zonedDateTime)
        } else {
            observeAllDiaries()
        }
    }

    private fun observeFilteredDiaries(zonedDateTime: ZonedDateTime) {
        filteredDiariesJob = viewModelScope.launch {
            if (::allDiariesJob.isInitialized) {
                allDiariesJob.cancelAndJoin()
            }
            getFilteredDiaries(zonedDateTime = zonedDateTime).collect { result ->
                diaries.value = result
            }
        }
    }
    
    private fun observeAllDiaries() {
        allDiariesJob = viewModelScope.launch {
            if(::filteredDiariesJob.isInitialized) {
                filteredDiariesJob.cancelAndJoin()
            }
            getAllDiaries().collect { result ->
                diaries.value = result
                Log.d("Diary data", "From HomeViewModel observeAllDiaries() -> $result")
            }
        }
    }

    fun deleteAllDiaries(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (network == ConnectivityObserver.Status.Available) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val imagesDirectory = "images/${userId}"
            val storage = FirebaseStorage.getInstance().reference
            storage.child(imagesDirectory).listAll().addOnSuccessListener {
                it.items.forEach { ref ->
                    val imagePath = "images/$userId/${ref.name}"
                    storage.child(imagePath).delete()
                        .addOnFailureListener {
                            viewModelScope.launch(Dispatchers.IO) {
                                imageToDeleteDao.addImageToDelete(
                                    com.phoenixredwolf.mongo.database.entity.ImageToDelete(
                                        remoteImagePath = imagePath
                                    )
                                )
                            }
                        }

                }
                viewModelScope.launch(Dispatchers.IO) {
                    val result = com.phoenixredwolf.mongo.repository.MongoDB.deleteAllDiaries()
                    if (result is RequestState.Success) {
                        withContext(Dispatchers.Main) {
                            onSuccess
                        }
                    } else if (result is RequestState.Error) {
                        withContext(Dispatchers.Main) {
                            onError(result.error)
                        }
                    }
                }
            }
                .addOnFailureListener {  onError(it) }
        } else {
            onError(Exception("No Internet Connection."))
        }
    }
}