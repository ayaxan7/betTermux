package com.ayaan.mongofsterminal.network

import android.util.Log
import com.ayaan.mongofsterminal.data.networktester.NetworkTestState
import com.speedchecker.android.sdk.Public.SpeedTestListener
import com.speedchecker.android.sdk.Public.SpeedTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class NetworkQualityManager: SpeedTestListener {
    private val _testState = MutableStateFlow(NetworkTestState())
    val testState: StateFlow<NetworkTestState> = _testState.asStateFlow()

    private val tag = "NetworkQualityManager"
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun startSpeedTest() {
        Log.d(tag, "Starting speed test")
        _testState.value = NetworkTestState(
            isRunning = true,
            currentPhase = "Initializing...",
            progress = 0,
            logMessages = listOf(log("Test initialization started"))
        )
    }

    private fun log(message: String): String {
        val timestamp = sdf.format(Date(System.currentTimeMillis()))
        val logEntry = "[$timestamp]: $message"
        Log.d(tag, logEntry)
        return logEntry
    }

    private fun addLogMessage(message: String) {
        val logEntry = log(message)
        val currentState = _testState.value
        _testState.value = currentState.copy(
            logMessages = (listOf(logEntry) + currentState.logMessages).take(50) // Keep last 50 logs
        )
    }

    override fun onTestStarted() {
        Log.d(tag, "Test started")
        _testState.value = _testState.value.copy(
            isRunning = true,
            currentPhase = "Test started",
            isCompleted = false,
            error = null,
            progress = 0,
            currentTestValue = "",
            trafficTestValue = ""
        )
        addLogMessage("Test started")
    }

    override fun onFetchServerFailed(errorCode: Int?) {
        Log.e(tag, "Fetch server failed with error code: $errorCode")
        _testState.value = _testState.value.copy(
            isRunning = false,
            error = "Failed to fetch server (Error: $errorCode)",
            isCompleted = true,
            currentPhase = "Server fetch error"
        )
        addLogMessage("Server fetch error: $errorCode")
    }

    override fun onFindingBestServerStarted() {
        Log.d(tag, "Finding best server started")
        _testState.value = _testState.value.copy(
            currentPhase = "Finding best server..."
        )
        addLogMessage("Finding best server...")
    }

    override fun onTestFinished(result: SpeedTestResult?) {
        Log.d(tag, "Test finished with result: $result")
        if (result != null) {
            _testState.value = _testState.value.copy(
                isRunning = false,
                currentPhase = "Test completed",
                downloadSpeed = result.downloadSpeed.toDouble(),
                uploadSpeed = result.uploadSpeed.toDouble(),
                ping = result.ping,
                jitter = result.jitter,
                serverDomain = result.server?.Domain ?: "Unknown",
                connectionType = result.connectionTypeHuman ?: "Unknown",
                packetLoss = result.packetLoss,
                isCompleted = true,
                progress = 240, // Max progress
                currentTestValue = "Test completed"
            )
            addLogMessage("Test Finished: Server[${result.server?.Domain}] -> $result")
        } else {
            _testState.value = _testState.value.copy(
                isRunning = false,
                error = "Test completed but no results received",
                isCompleted = true
            )
            addLogMessage("Test completed but no results received")
        }
    }

    override fun onPingStarted() {
        Log.d(tag, "Ping test started")
        _testState.value = _testState.value.copy(
            currentPhase = "Testing ping...",
            progress = 20,
            currentTestValue = "Progress"
        )
        addLogMessage("Ping Started")
    }

    override fun onPingFinished(ping: Int, jitter: Int) {
        Log.d(tag, "Ping finished: $ping ms, Jitter: $jitter ms")
        _testState.value = _testState.value.copy(
            ping = ping,
            jitter = jitter,
            currentPhase = "Ping test completed",
            progress = 40,
            currentTestValue = "$ping ms | jitter: $jitter"
        )
        addLogMessage("Ping Finished: $ping ms| jitter: $jitter")
    }

    override fun onDownloadTestStarted() {
        Log.d(tag, "Download test started")
        _testState.value = _testState.value.copy(
            currentPhase = "Testing download speed...",
            currentTestValue = "",
            trafficTestValue = ""
        )
        addLogMessage("Download Test Started")
    }

    override fun onDownloadTestProgress(progress: Int, instantSpeed: Double, avgSpeed: Double) {
        Log.d(tag, "Download progress: $progress%, Instant: $instantSpeed Mbps, Avg: $avgSpeed Mbps")
        _testState.value = _testState.value.copy(
            progress = 40 + progress, // Following reference: 40 base + progress
            downloadSpeed = avgSpeed,
            currentPhase = "Download",
            currentTestValue = "${String.format("%.2f", avgSpeed)} Mb/s",
            trafficTestValue = "TransferredMb: ${String.format("%.2f", instantSpeed)}"
        )
        addLogMessage("Download Test Progress: $progress% -> ${String.format("%.2f", avgSpeed)} Mb/s\nTransferredMb: ${String.format("%.2f", instantSpeed)}")
    }

    override fun onDownloadTestFinished(downloadSpeed: Double) {
        Log.d(tag, "Download test finished: $downloadSpeed Mbps")
        _testState.value = _testState.value.copy(
            downloadSpeed = downloadSpeed,
            currentPhase = "Download completed",
            currentTestValue = "${String.format("%.2f", downloadSpeed)} Mb/s"
        )
        addLogMessage("Download Test Finished: ${String.format("%.2f", downloadSpeed)} Mb/s")
    }

    override fun onUploadTestStarted() {
        Log.d(tag, "Upload test started")
        _testState.value = _testState.value.copy(
            currentPhase = "Testing upload speed...",
            currentTestValue = "",
            trafficTestValue = ""
        )
        addLogMessage("Upload Test Started")
    }

    override fun onUploadTestProgress(progress: Int, instantSpeed: Double, avgSpeed: Double) {
        Log.d(tag, "Upload progress: $progress%, Instant: $instantSpeed Mbps, Avg: $avgSpeed Mbps")
        _testState.value = _testState.value.copy(
            progress = 140 + progress,
            uploadSpeed = avgSpeed,
            currentPhase = "Upload",
            currentTestValue = "${String.format("%.2f", avgSpeed)} Mb/s",
            trafficTestValue = "TransferredMb: ${String.format("%.2f", instantSpeed)}"
        )
        addLogMessage("Upload Test Progress: $progress% -> ${String.format("%.2f", avgSpeed)} Mb/s\nTransferredMb: ${String.format("%.2f", instantSpeed)}")
    }

    override fun onUploadTestFinished(uploadSpeed: Double) {
        Log.d(tag, "Upload test finished: $uploadSpeed Mbps")
        _testState.value = _testState.value.copy(
            uploadSpeed = uploadSpeed,
            currentPhase = "Upload completed",
            currentTestValue = "${String.format("%.2f", uploadSpeed)} Mb/s"
        )
        addLogMessage("Upload Test Finished: ${String.format("%.2f", uploadSpeed)} Mb/s")
    }

    override fun onTestWarning(warning: String?) {
        Log.w(tag, "Test warning: $warning")
        _testState.value = _testState.value.copy(
            currentPhase = "Warning",
            currentTestValue = "Test Warning",
            trafficTestValue = warning ?: "Unknown warning"
        )
        addLogMessage("Test Warning: $warning")
    }

    override fun onTestFatalError(error: String?) {
        Log.e(tag, "Test fatal error: $error")
        _testState.value = _testState.value.copy(
            isRunning = false,
            error = error ?: "Unknown fatal error",
            isCompleted = true,
            currentPhase = "Fatal Error",
            currentTestValue = "Test Error",
            trafficTestValue = error ?: "Unknown error"
        )
        addLogMessage("Test Fatal Error: $error")
    }

    override fun onTestInterrupted(reason: String?) {
        Log.w(tag, "Test interrupted: $reason")
        _testState.value = _testState.value.copy(
            isRunning = false,
            error = "Test interrupted: ${reason ?: "Unknown reason"}",
            isCompleted = true,
            currentPhase = "Test interrupted",
            currentTestValue = reason ?: "Test interrupted"
        )
        addLogMessage("Test Interrupted: $reason")
    }
}