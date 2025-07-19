package com.ayaan.mongofsterminal.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ayaan.mongofsterminal.presentation.auth.forgotpasswordscreen.ForgotPasswordScreen
import com.ayaan.mongofsterminal.presentation.auth.signinscreen.SignInScreen
import com.ayaan.mongofsterminal.presentation.auth.signupscreen.SignUpScreen
import com.ayaan.mongofsterminal.presentation.splashscreen.SplashScreen
import com.ayaan.mongofsterminal.presentation.terminalscreen.TerminalScreen

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(modifier: Modifier) {
    val navController = rememberNavController()

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
