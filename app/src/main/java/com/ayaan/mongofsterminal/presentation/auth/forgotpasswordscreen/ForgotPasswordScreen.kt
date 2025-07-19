package com.ayaan.mongofsterminal.presentation.auth.forgotpasswordscreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ayaan.mongofsterminal.presentation.auth.components.AuthScreenFooter
import com.ayaan.mongofsterminal.presentation.auth.components.TerminalTextField
import com.ayaan.mongofsterminal.presentation.auth.signinscreen.SignInViewModel
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: SignInViewModel = hiltViewModel()
) {
    // State for local email input
    var localEmail by remember { mutableStateOf("") }
    val resetPasswordMessage = viewModel.resetPasswordMessage.value
    val isResetPasswordLoading = viewModel.isResetPasswordLoading.value

    val scrollState = rememberScrollState()
    var showCursor by remember { mutableStateOf(true) }
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }
    val texts = listOf(
        "/* Password Recovery */",
        "/* Enter your email address */",
        "/* We'll send you a reset link */",
    )
    var currentTextIndex by remember { mutableStateOf(0) }

    // Animation for blinking cursor
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    // Enhanced animation for typing and deleting text carousel
    LaunchedEffect(Unit) {
        while (true) {
            val currentFullText = texts[currentTextIndex]

            // Typing phase
            if (isTyping) {
                for (i in 1..currentFullText.length) {
                    displayedText = currentFullText.substring(0, i)
                    delay(60) // Typing speed
                }
                delay(1200) // Pause at the end of typing
                isTyping = false
            }
            // Deleting phase
            else {
                for (i in currentFullText.length downTo 0) {
                    displayedText = currentFullText.substring(0, i)
                    delay(20) // Deletion speed (faster than typing)
                }
                // Move to next text
                currentTextIndex = (currentTextIndex + 1) % texts.size
                isTyping = true
                delay(300) // Pause before starting next text
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start
        ) {
            // Terminal header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Text(
                        text = "←",
                        color = Color(0xFF4EB839),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp
                    )
                }
                Text(
                    text = "┌─[ BetterMux Terminal ]─[ Password Recovery ]",
                    color = Color(0xFF4EB839),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated text
            Text(
                text = displayedText + if (showCursor) "_" else "",
                color = Color.Cyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Text(
                text = "Enter the email address associated with your account and we'll send you a link to reset your password.",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email field
            TerminalTextField(
                value = localEmail,
                onValueChange = { localEmail = it },
                label = "email:",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reset password message
            if (resetPasswordMessage != null) {
                val alpha by animateFloatAsState(
                    targetValue = if (resetPasswordMessage != null) 1f else 0f,
                    label = "resetAlpha"
                )
                Text(
                    text = resetPasswordMessage,
                    color = if (resetPasswordMessage.contains("sent")) Color.Green else Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .alpha(alpha)
                )
            }

            // Send Reset Email button
            Button(
                onClick = {
                    viewModel.sendPasswordResetEmail(localEmail) {
                        // Optionally navigate back after successful email send
                        // navController.navigateUp()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C5A2E),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = !isResetPasswordLoading && localEmail.isNotEmpty()
            ) {
                if (isResetPasswordLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        text = "SEND RESET EMAIL",
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Sign In link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Remember your password? ",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(onClick = { navController.navigateUp() }) {
                    Text(
                        text = "Sign In",
                        color = Color(0xFF4EB839),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Terminal footer
            Spacer(modifier = Modifier.weight(1f))
            AuthScreenFooter()
        }
    }
}
