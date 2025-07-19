package com.ayaan.mongofsterminal.presentation.auth.signinscreen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.utils.GoogleSignInUtils
import com.ayaan.mongofsterminal.utils.GitHubSignInUtils
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
    private val googleSignInUtils: GoogleSignInUtils,
    private val gitHubSignInUtils: GitHubSignInUtils
) : ViewModel() {

    // UI state
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val resetPasswordMessage = mutableStateOf<String?>(null)
    val isResetPasswordLoading = mutableStateOf(false)

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
                if (email.value.isEmpty() || password.value.isBlank()) {
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

    fun handleGitHubLogin(
        context: Context,
        login: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        gitHubSignInUtils.doGitHubSignIn(
            context = context,
            scope = viewModelScope,
            login = login,
            onError = onError
        )
    }

    // Forgot Password functionality
    fun sendPasswordResetEmail(emailAddress: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                isResetPasswordLoading.value = true
                resetPasswordMessage.value = null
                errorMessage.value = null

                val resetEmail = emailAddress?.takeIf { it.isNotBlank() } ?: email.value

                if (resetEmail.isEmpty()) {
                    resetPasswordMessage.value = "Please enter your email address"
                    isResetPasswordLoading.value = false
                    return@launch
                }

                firebaseAuth.sendPasswordResetEmail(resetEmail.trim()).await()

                resetPasswordMessage.value = "Password reset email sent to $resetEmail"
                isResetPasswordLoading.value = false
                onSuccess()

            } catch (e: FirebaseAuthInvalidUserException) {
                isResetPasswordLoading.value = false
                resetPasswordMessage.value = "No account found with this email address"
            } catch (e: Exception) {
                isResetPasswordLoading.value = false
                resetPasswordMessage.value = e.message ?: "Failed to send password reset email"
            }
        }
    }

    fun clearResetPasswordMessage() {
        resetPasswordMessage.value = null
    }
}