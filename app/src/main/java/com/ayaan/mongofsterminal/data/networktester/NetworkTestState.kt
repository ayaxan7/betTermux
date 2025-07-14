package com.ayaan.mongofsterminal.data.networktester

data class NetworkTestState(
    val isRunning: Boolean = false,
    val currentPhase: String = "",
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val ping: Int = 0,
    val jitter: Int = 0,
    val progress: Int = 0,
    val progressMax: Int = 240,
    val error: String? = null,
    val isCompleted: Boolean = false,
    val serverDomain: String = "",
    val connectionType: String = "",
    val packetLoss: Double? = null,
    val currentTestValue: String = "",
    val trafficTestValue: String = "",
    val logMessages: List<String> = emptyList()
)