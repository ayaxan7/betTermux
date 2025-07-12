package com.ayaan.mongofsterminal.presentation.auth.signupscreen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fileSystemRepository: FileSystemRepository
) : ViewModel() {

    // UI state
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")
    val username = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // Update methods
    fun onEmailChange(value: String) {
        email.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

    fun onConfirmPasswordChange(value: String) {
        confirmPassword.value = value
    }

    fun onUsernameChange(value: String) {
        username.value = value
    }

    // Validation
    private fun validateInputs(): Boolean {
        when {
            email.value.isBlank() || password.value.isBlank() ||
                    confirmPassword.value.isBlank() || username.value.isBlank() -> {
                errorMessage.value = "All fields are required"
                return false
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches() -> {
                errorMessage.value = "Please enter a valid email address"
                return false
            }

            password.value != confirmPassword.value -> {
                errorMessage.value = "Passwords don't match"
                return false
            }

            password.value.length < 6 -> {
                errorMessage.value = "Password must be at least 6 characters long"
                return false
            }

            else -> return true
        }
    }

    // Registration
    fun signUp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                errorMessage.value = null

                // Validate inputs first
                if (!validateInputs()) {
                    return@launch
                }

                isLoading.value = true

                // Create user with email and password
                val authResult = firebaseAuth.createUserWithEmailAndPassword(
                    email.value.trim(),
                    password.value.trim()
                ).await()

                // Add display name to user profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(username.value.trim())
                    .build()

                authResult.user?.updateProfile(profileUpdates)?.await()

                // Initialize file system for the new user
                authResult.user?.uid?.let { uid ->
                    val fsResult = fileSystemRepository.initializeFileSystem(uid)
                    if (!fsResult.success) {
                        // Log error but don't block user from proceeding
                        println("Failed to initialize filesystem: ${fsResult.error}")
                    }
                }

                // Success
                isLoading.value = false
                onSuccess()
            } catch (e: Exception) {
                isLoading.value = false
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        errorMessage.value = "This email is already registered"
                    }

                    is FirebaseAuthWeakPasswordException -> {
                        errorMessage.value = "Password is too weak"
                    }

                    else -> {
                        errorMessage.value = e.message ?: "Registration failed"
                    }
                }
            }
        }
    }
}