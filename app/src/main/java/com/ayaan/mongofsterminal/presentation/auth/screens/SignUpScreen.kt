package com.ayaan.mongofsterminal.presentation.auth.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ayaan.mongofsterminal.navigation.Route
import com.ayaan.mongofsterminal.presentation.auth.viewmodel.AuthViewModel
import com.ayaan.mongofsterminal.presentation.auth.components.AuthScreenFooter
import com.ayaan.mongofsterminal.presentation.auth.components.GitHubSignInButton
import com.ayaan.mongofsterminal.presentation.auth.components.GoogleSignInButton
import com.ayaan.mongofsterminal.presentation.auth.components.TerminalTextField
import kotlinx.coroutines.delay

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // Access individual state properties directly
    val email = viewModel.email.value
    val password = viewModel.password.value
    val confirmPassword = viewModel.confirmPassword.value
    val username = viewModel.username.value
    val isLoading = viewModel.isLoading.value
    val errorMessage = viewModel.errorMessage.value

    val scrollState = rememberScrollState()
    var showCursor by remember { mutableStateOf(true) }
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(true) }
    val texts = listOf(
        "/* Welcome to BetterMux Terminal */",
        "/* Create a new account... */",
        "/* Enter your credentials below */",
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
            // Terminal header
            Text(
                text = "┌─[ BetterMux Terminal ]─[ Registration ]",
                color = Color(0xFF4EB839),
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Animated text with cursor
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayedText,
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
                Text(
                    text = if (showCursor) "_" else " ",
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Username field
            TerminalTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = "username:",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Email field
            TerminalTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = "email:",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Password field
            TerminalTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = "password:",
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Confirm Password field
            TerminalTextField(
                value = confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChange(it) },
                label = "confirm_password:",
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error message
            if (errorMessage != null) {
                val alpha by animateFloatAsState(targetValue = if (errorMessage != null) 1f else 0f,
                    label = "errorAlpha")
                Text(
                    text = "Error: $errorMessage",
                    color = Color.Red,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .alpha(alpha)
                )
            }
            val context= LocalContext.current

            val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                viewModel.handleGoogleLogin(
                    context = context,
                    launcher = null,
                    login = {
                        navController.navigate(Route.TerminalScreen.route) {
                            popUpTo(Route.SignInScreen.route) { inclusive = true }
                        }
                    }
                )
            }
            if(username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                Button(
                    onClick = {
                        viewModel.signUp {
                            navController.navigate(Route.TerminalScreen.route) {
                                popUpTo(Route.SignUpScreen.route) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C5A2E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !isLoading &&
                            email.isNotEmpty() &&
                            password.isNotEmpty() &&
                            confirmPassword.isNotEmpty() &&
                            username.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Text(
                            text = "REGISTER",
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GoogleSignInButton(
                    context = context,
                    modifier = Modifier,
                    launcher = launcher,
                    navController = navController
                )
                // GitHub Sign In Button
                GitHubSignInButton(
                    context = context,
                    modifier = Modifier.padding(vertical = 8.dp),
                    navController = navController
                )
            }


            // Login link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(onClick = { navController.navigate(Route.SignInScreen.route){
                    popUpTo(Route.SignUpScreen.route){ inclusive = true}
                } }) {
                    Text(
                        text = "Sign in",
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
