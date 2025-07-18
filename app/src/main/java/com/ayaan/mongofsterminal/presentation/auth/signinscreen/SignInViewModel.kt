package com.ayaan.mongofsterminal.presentation.auth.signinscreen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.utils.GoogleSignInUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInUtils: GoogleSignInUtils
) : ViewModel() {

    // UI state
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // Update methods
    fun onEmailChange(value: String) {
        email.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

    // Authentication
    fun signIn(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                isLoading.value = true
                errorMessage.value = null

                // Validate inputs
                if (email.value.isBlank() || password.value.isBlank()) {
                    errorMessage.value = "Email and password cannot be empty"
                    isLoading.value = false
                    return@launch
                }

                // Attempt to sign in
                firebaseAuth.signInWithEmailAndPassword(
                    email.value.trim(),
                    password.value.trim()
                ).await()

                // Success
                isLoading.value = false
                onSuccess()

            } catch (e: FirebaseAuthInvalidUserException) {
                isLoading.value = false
                errorMessage.value = "User not found. Please check your email or sign up."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                isLoading.value = false
                errorMessage.value = "Invalid credentials. Please check your email and password."
            } catch (e: Exception) {
                isLoading.value = false
                errorMessage.value = e.message ?: "Sign in failed. Please try again."
            }
        }
    }

    fun handleGoogleLogin(
        context: Context,
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
        login: () -> Unit
    ) {
        googleSignInUtils.doGoogleSignIn(
            context = context,
            scope = viewModelScope,
            launcher = launcher,
            login = login
        )
    }
}