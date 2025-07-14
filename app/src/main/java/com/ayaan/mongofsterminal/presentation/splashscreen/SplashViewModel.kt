package com.ayaan.mongofsterminal.presentation.splashscreen

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fileSystemApi: FileSystemApi
) : ViewModel() {

    val isBackendReady = mutableStateOf(false)
    val connectionAttempts = mutableStateOf(0)
    val loadingDots = mutableStateOf("")

    init {
        checkBackendHealth()
        startProgressTimer()
    }

    fun setLoadingDots(dots: String) {
        loadingDots.value = dots
    }

    private fun startProgressTimer() {
        viewModelScope.launch {
            while (!isBackendReady.value && connectionAttempts.value < 30) {
                delay(2000)
                if (!isBackendReady.value) {
                    connectionAttempts.value++
                    Log.d("SplashViewModel", "Progress timer - Connection attempts: ${connectionAttempts.value}")
                }
            }
        }
    }

    private fun checkBackendHealth() {
        viewModelScope.launch {
            while (!isBackendReady.value) {
                try {
                    Log.d("SplashViewModel", "Checking backend health - Attempt #${connectionAttempts.value}")
                    val response = fileSystemApi.checkHealth()
                    Log.d("SplashViewModel", "Response from backend: ${response.status}")
                    if (response.status == "OK") {
                        Log.d("SplashViewModel", "Backend is ready! Status: ${response.status}")
                        isBackendReady.value = true
                        break
                    } else {
                        Log.d("SplashViewModel", "Backend not ready yet. Status: ${response.status}")
                    }
                } catch (e: Exception) {
                    Log.e("SplashViewModel", "Error checking backend health: ${e.message}")
                }
                val delayTime = minOf(1000L * (1 shl minOf(connectionAttempts.value / 2, 2)), 3000L)
                delay(delayTime)
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
