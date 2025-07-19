package com.ayaan.mongofsterminal.navigation

sealed class Route(val route: String) {
    object SplashScreen : Route("splash_screen")
    object TerminalScreen : Route("terminal_screen")
    object FingerPrintScreen : Route("fingerprint_screen")
    object SignInScreen : Route("signin_screen")
    object SignUpScreen : Route("signup_screen")
    object ForgotPasswordScreen : Route("forgot_password_screen")
}