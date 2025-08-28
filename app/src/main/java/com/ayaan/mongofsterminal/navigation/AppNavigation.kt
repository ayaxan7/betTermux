package com.ayaan.mongofsterminal.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ayaan.mongofsterminal.presentation.auth.screens.ForgotPasswordScreen
import com.ayaan.mongofsterminal.presentation.auth.screens.SignInScreen
import com.ayaan.mongofsterminal.presentation.auth.screens.SignUpScreen
import com.ayaan.mongofsterminal.presentation.splashscreen.SplashScreen
import com.ayaan.mongofsterminal.presentation.terminalscreen.TerminalScreen
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(modifier: Modifier) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // App opened
                    onAppEvent("App opened")
                }

                Lifecycle.Event.ON_STOP -> {
                    // App closed / moved to background
                    onAppEvent("App closed")
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Detects navigation changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            onAppEvent("Navigated to: ${destination.route}")
        }
    }
    // Always start with the splash screen regardless of auth status
    // The splash screen will handle navigation based on backend status and auth
    NavHost(startDestination = Route.SplashScreen.route, navController = navController){
        composable(Route.SplashScreen.route) {
            SplashScreen(navController = navController)
        }

        composable(Route.SignInScreen.route) {
            SignInScreen(navController = navController)
        }

        composable(Route.SignUpScreen.route) {
            SignUpScreen(navController = navController)
        }

        composable(Route.ForgotPasswordScreen.route) {
            ForgotPasswordScreen(navController = navController)
        }

        composable(Route.TerminalScreen.route) {
            TerminalScreen(navController = navController)
        }
    }
}
fun onAppEvent(message: String) {
    println("App Event: $message")
    val user= FirebaseAuth.getInstance().currentUser!=null
    Log.d("AppNavigation", "Screen Changed: $user")
}
@RequiresApi(Build.VERSION_CODES.P)
@Preview
@Composable
fun AppNavigationPreview() {
    AppNavigation(modifier = Modifier)
}
