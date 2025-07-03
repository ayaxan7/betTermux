package com.ayaan.mongofsterminal.navigation

sealed class Route(val route: String) {
    object TerminalScreen : Route("terminal_screen")
    object FingerPrintScreen : Route("fingerprint_screen")
}