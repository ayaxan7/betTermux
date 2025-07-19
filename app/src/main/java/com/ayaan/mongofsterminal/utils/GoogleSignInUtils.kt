package com.ayaan.mongofsterminal.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.ayaan.mongofsterminal.BuildConfig
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoogleSignInUtils @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val fileSystemRepository: FileSystemRepository
){
        fun doGoogleSignIn(
            context: Context,
            scope: CoroutineScope,
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
            login: () -> Unit,
        ) {
            val credentialManager = CredentialManager.create(context)

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptions(context))
                .build()
            scope.launch {
                try {
                    val result = credentialManager.getCredential(context,request)
                    when(result.credential){
                        is CustomCredential ->{
                            if(result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL){
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                val googleTokenId = googleIdTokenCredential.idToken
                                val authCredential =
                                    GoogleAuthProvider.getCredential(googleTokenId, null)
                               val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                               val user = authResult.user
                               user?.let {
                                   if(it.isAnonymous.not()){
                                       login.invoke()
                                       // Initialize file system only for new users
                                       if (authResult.additionalUserInfo?.isNewUser == true) {
                                           Log.d("GoogleSignInUtils", "New user signed in: ${user.uid}")
                                           user.uid.let { uid ->
                                               val fsResult = fileSystemRepository.initializeFileSystem(uid)
                                               if (!fsResult.success) {
                                                   // Log error but don't block user from proceeding
                                                   Log.d("GoogleSignInUtils", "Failed to initialize filesystem: ${fsResult.error}")
                                               }else{
                                                   Log.d("GoogleSignInUtils", "File system initialized successfully for user: $uid")
                                               }
                                           }
                                       }else{
                                             Log.d("GoogleSignInUtils", "Existing user signed in: ${user.uid}")
                                       }
                                   }
                               }
                            }
                        }
                        else ->{
                        }
                    }
                }catch (e:NoCredentialException){
                    launcher?.launch(getIntent())
                }catch (e:GetCredentialException){
                    e.printStackTrace()
                }
            }
        }

        private fun getIntent():Intent{
            return Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
            }
        }
        private fun getCredentialOptions(context: Context):CredentialOption{
            val oauthClientId = BuildConfig.OAUTH_CLIENT_ID
            return GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(oauthClientId)
                .build()
        }

}