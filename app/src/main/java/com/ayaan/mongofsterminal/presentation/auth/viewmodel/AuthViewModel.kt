package com.ayaan.mongofsterminal.presentation.auth.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.ayaan.mongofsterminal.utils.GitHubSignInUtils
import com.ayaan.mongofsterminal.utils.GoogleSignInUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInUtils: GoogleSignInUtils,
    private val gitHubSignInUtils: GitHubSignInUtils,
    private val fileSystemRepository: FileSystemRepository
) : ViewModel() {

    // Common UI state
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    // Sign up specific state
    val confirmPassword = mutableStateOf("")
    val username = mutableStateOf("")

    // Password reset state
    val resetPasswordMessage = mutableStateOf<String?>(null)
    val isResetPasswordLoading = mutableStateOf(false)

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

    // Sign In functionality
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

    // Sign Up functionality
    private fun validateSignUpInputs(): Boolean {
        when {
            email.value.isBlank() || password.value.isBlank() ||
                    confirmPassword.value.isBlank() || username.value.isBlank() -> {
                errorMessage.value = "All fields are required"
                return false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email.value).matches() -> {
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

    fun signUp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                errorMessage.value = null

                // Validate inputs first
                if (!validateSignUpInputs()) {
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

    // Social Sign In
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

    // Password Reset functionality
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

    // Clear all fields and messages
    fun clearAll() {
        email.value = ""
        password.value = ""
        confirmPassword.value = ""
        username.value = ""
        errorMessage.value = null
        resetPasswordMessage.value = null
        isLoading.value = false
        isResetPasswordLoading.value = false
    }
}