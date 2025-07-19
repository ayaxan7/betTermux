package com.ayaan.mongofsterminal.utils

import android.content.Context
import android.util.Log
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GitHubSignInUtils @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fileSystemRepository: FileSystemRepository
) {

    fun doGitHubSignIn(
        context: Context,
        scope: CoroutineScope,
        login: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            try {
                val provider = OAuthProvider.newBuilder("github.com")
                    .setScopes(listOf("user:email"))
                    .build()

                val authResult = firebaseAuth.startActivityForSignInWithProvider(
                    context as androidx.activity.ComponentActivity,
                    provider
                ).await()

                val user = authResult.user
                user?.let {
                    if (it.isAnonymous.not()) {
                        login.invoke()

                        // Initialize file system only for new users
                        if (authResult.additionalUserInfo?.isNewUser == true) {
                            Log.d("GitHubSignInUtils", "New user signed in: ${user.uid}")
                            user.uid.let { uid ->
                                val fsResult = fileSystemRepository.initializeFileSystem(uid)
                                if (!fsResult.success) {
                                    Log.d("GitHubSignInUtils", "Failed to initialize filesystem: ${fsResult.error}")
                                } else {
                                    Log.d("GitHubSignInUtils", "File system initialized successfully for user: $uid")
                                }
                            }
                        } else {
                            Log.d("GitHubSignInUtils", "Existing user signed in: ${user.uid}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError("GitHub sign-in failed: ${e.message}")
            }
        }
    }
}
