package com.ayaan.mongofsterminal.presentation.fingerPrintScreen
import android.content.Context
import androidx.biometric.BiometricManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun FingerprintScreen(
    onAuthenticationSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val context = LocalContext.current

    // Create a delegated property that logs whenever the state changes
    var authenticationState by remember {
        mutableStateOf(AuthenticationState.IDLE).also {
            Log.d("FingerPrintScreen", "Initial authentication state: ${AuthenticationState.IDLE}")
        }
    }

    // Custom delegate to track state changes
    var previousState = AuthenticationState.IDLE
    authenticationState = authenticationState.also {
        if (it != previousState) {
            Log.d("FingerPrintScreen", "Authentication state changed: $previousState -> $it")
            previousState = it
        }
    }

    var errorMessage by remember { mutableStateOf("") }

    val biometricManager = BiometricManager.from(context)
    val biometricPrompt = instanceOfBiometricPrompt(context, onAuthenticationSuccess)

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Use your fingerprint to access the terminal")
        .setNegativeButtonText("Cancel")
        .build()

    LaunchedEffect(Unit) {
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric authentication is available
                Toast.makeText(context, "Biometric authentication is available", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                authenticationState = AuthenticationState.ERROR
                errorMessage = "No biometric hardware available"
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                authenticationState = AuthenticationState.ERROR
                errorMessage = "Biometric hardware is currently unavailable"
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                authenticationState = AuthenticationState.ERROR
                errorMessage = "No biometric credentials enrolled"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Fingerprint",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Terminal Access",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Please authenticate to access the terminal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (authenticationState) {
            AuthenticationState.IDLE -> {
                Button(
                    onClick = {
                        authenticationState = AuthenticationState.AUTHENTICATING
                        biometricPrompt.authenticate(promptInfo)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authenticate")
                }
            }

            AuthenticationState.AUTHENTICATING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Authenticating...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            AuthenticationState.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Authentication successful!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AuthenticationState.ERROR, AuthenticationState.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        authenticationState = AuthenticationState.IDLE
                        errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}
private fun instanceOfBiometricPrompt(context: Context, onAuthenticationSuccess: () -> Unit): BiometricPrompt {
    val executor = ContextCompat.getMainExecutor(context)
    val activity: FragmentActivity = context as FragmentActivity
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            showMessage(context, "$errorCode :: $errString")
        }
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            showMessage(context, "Authentication failed for an unknown reason")
        }
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            showMessage(context, "Authentication was successful")
            onAuthenticationSuccess()
        }
    }

    return BiometricPrompt(activity, executor, callback)
}
private fun showMessage(context: Context, message: String) {
    // You can implement this with Toast, Snackbar, or update your UI state
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}