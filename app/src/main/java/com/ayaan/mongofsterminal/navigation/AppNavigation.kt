package com.ayaan.mongofsterminal.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ayaan.mongofsterminal.presentation.auth.signinscreen.SignInScreen
import com.ayaan.mongofsterminal.presentation.auth.signupscreen.SignUpScreen
import com.ayaan.mongofsterminal.presentation.fingerPrintScreen.FingerprintScreen
import com.ayaan.mongofsterminal.presentation.terminalscreen.TerminalScreen
import com.google.firebase.auth.FirebaseAuth

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AppNavigation(modifier: Modifier) {
    val navController = rememberNavController()
    val auth= FirebaseAuth.getInstance()
    val user = auth.currentUser
    val uid= user?.uid ?: ""
    Log.d("AppNavigation", "Current User ID: $uid")
    val startDestination = if (user != null) Route.TerminalScreen.route else Route.SignInScreen.route
    NavHost(startDestination = startDestination, navController = navController){
        composable(Route.SignInScreen.route) {
            SignInScreen(navController = navController)
        }

        composable(Route.SignUpScreen.route) {
            SignUpScreen(navController = navController)
        }

//        composable(Route.FingerPrintScreen.route) {
//            FingerprintScreen(onAuthenticationSuccess = {navController.navigate(Route.TerminalScreen.route)}, modifier =modifier, navController = navController)
//        }

        composable(Route.TerminalScreen.route) {
            TerminalScreen(navController = navController)
        }
    }
}
