package com.ayaan.mongofsterminal.presentation.splashscreen

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ayaan.mongofsterminal.navigation.Route
import com.ayaan.mongofsterminal.presentation.splashscreen.components.AnimatedVisibility
import com.ayaan.mongofsterminal.presentation.splashscreen.components.TerminalProgressBar
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val isBackendReady = viewModel.isBackendReady.value
    val connectionAttempts = viewModel.connectionAttempts.value
    val loadingDots by remember { mutableStateOf("") }
    var displayText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    val maxConnectionAttempts = 30

    // Blinking cursor animation
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    // Loading dots animation
    LaunchedEffect(Unit) {
        while (!isBackendReady) {
            for (i in 0..3) {
                viewModel.setLoadingDots(".".repeat(i))
                delay(300)
            }
        }
    }

    // Terminal text typing animation
    LaunchedEffect(Unit) {
        val bootSequence = listOf(
            "Initializing BetterMux Terminal v2.0",
            "Establishing secure connection",
            "Checking backend status",
            "Waiting for backend to respond",
            "Initializing file system",
            "Loading user preferences"
        )

        for (line in bootSequence) {
            displayText = ""
            for (i in line.indices) {
                displayText = line.substring(0, i + 1)
                delay(30) // typing speed
            }
            delay(500)
        }
    }

    // Handle navigation when backend is ready
    LaunchedEffect(isBackendReady) {
        if (isBackendReady) {
            delay(1000) // Give time to see the success message
            Log.d("SplashScreen", "User id: ${viewModel.isUserLoggedIn()}")
            // Navigate based on auth status
            val route = if (viewModel.isUserLoggedIn()) {
                Route.TerminalScreen.route
            } else {
                Route.SignInScreen.route
            }

            navController.navigate(route) {
                popUpTo(Route.SplashScreen.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Terminal header
            Text(
                text = "┌─[ BetterMux Terminal ]─[ System Boot ]",
                color = Color(0xFF4EB839),
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Main terminal animation area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFF121212), MaterialTheme.shapes.small)
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Display animated boot text
                    Text(
                        text = displayText + if (showCursor) "_" else "",
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Backend connection status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when {
                            isBackendReady -> Color(0xFF4EB839) // Green for success
                            connectionAttempts > maxConnectionAttempts / 2 -> Color.Yellow // Yellow for many attempts
                            else -> Color(0xFF64B5F6) // Blue for normal operation
                        }

                        Text(
                            text = "Backend status: ",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )

                        val statusText = when {
                            isBackendReady -> "CONNECTED"
                            else -> "CONNECTING${viewModel.loadingDots.value}"
                        }

                        Text(
                            text = statusText,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Connection attempts
                    Text(
                        text = "Connection attempts: $connectionAttempts",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Progress animation
                    val progress = connectionAttempts.toFloat() / maxConnectionAttempts
                    TerminalProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )

                    // Timeout warning message
                    AnimatedVisibility(connectionAttempts > maxConnectionAttempts / 2) {
                        Text(
                            text = "Note: Backend startup may take up to 30 seconds on first load.",
                            color = Color.Yellow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App title
            Text(
                text = "BETTERMUX",
                color = Color(0xFF4EB839),
                fontFamily = FontFamily.Monospace,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Terminal Emulator & File System",
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Terminal footer
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "└─[~ ]$ _",
                color = Color(0xFF4EB839),
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

