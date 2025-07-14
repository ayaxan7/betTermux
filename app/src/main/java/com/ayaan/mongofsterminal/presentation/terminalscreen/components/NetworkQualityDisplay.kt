package com.ayaan.mongofsterminal.presentation.terminalscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaan.mongofsterminal.data.networktester.NetworkTestState

@Composable
fun NetworkQualityDisplay(
    testState: NetworkTestState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1D21))
            .border(
                width = 1.dp,
                color = Color(0xFF4FC3F7).copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NetworkCheck,
                    contentDescription = "Network Test",
                    tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Network Quality Test",
                    color = Color(0xFF4FC3F7),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Status
            StatusIndicator(testState = testState)

            // Progress bar for running tests
            if (testState.isRunning) {
                ProgressSection(testState = testState)
            }

            // Results section for completed tests
            if (testState.isCompleted && testState.error == null) {
                ResultsSection(testState = testState)
            }

            // Error section
            if (testState.error != null) {
                ErrorSection(error = testState.error)
            }
        }
    }
}

@Composable
private fun StatusIndicator(testState: NetworkTestState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (icon, color, status) = when {
            testState.isRunning -> Triple(Icons.Default.PlayArrow, Color(0xFF4CAF50), "RUNNING")
            testState.isCompleted && testState.error == null -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), "COMPLETED")
            testState.error != null -> Triple(Icons.Default.Error, Color(0xFFF44336), "FAILED")
            else -> Triple(Icons.Default.Circle, Color(0xFF9E9E9E), "IDLE")
        }

        Icon(
            imageVector = icon,
            contentDescription = status,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Status: $status",
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ProgressSection(testState: NetworkTestState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "Progress",
                tint = Color(0xFF80CBC4),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = testState.currentPhase,
                color = Color(0xFF80CBC4),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Progress bar
        Column {
            LinearProgressIndicator(
                progress = (testState.progress.toFloat() / testState.progressMax.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF4FC3F7),
                trackColor = Color(0xFF32363E)
            )
            Text(
                text = "${testState.progress}/${testState.progressMax} (${((testState.progress.toFloat() / testState.progressMax.toFloat()) * 100).toInt()}%)",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Current values
        if (testState.currentTestValue.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Current Value",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = testState.currentTestValue,
                    color = Color(0xFFFFB74D),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ResultsSection(testState: NetworkTestState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Server info
        if (testState.serverDomain.isNotEmpty()) {
            MetricRow(
                icon = Icons.Default.Storage,
                label = "Server",
                value = testState.serverDomain,
                color = Color(0xFF80CBC4)
            )
        }

        if (testState.connectionType.isNotEmpty()) {
            MetricRow(
                icon = Icons.Default.Wifi,
                label = "Connection",
                value = testState.connectionType,
                color = Color(0xFF80CBC4)
            )
        }

        Divider(color = Color(0xFF32363E), thickness = 1.dp)

        // Speed results
        Text(
            text = "Speed Results",
            color = Color(0xFF4FC3F7),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        if (testState.downloadSpeed > 0.0) {
            SpeedMetricRow(
                icon = Icons.Default.GetApp,
                label = "Download",
                speed = testState.downloadSpeed,
                unit = "Mbps"
            )
        }

        if (testState.uploadSpeed > 0.0) {
            SpeedMetricRow(
                icon = Icons.Default.Publish,
                label = "Upload",
                speed = testState.uploadSpeed,
                unit = "Mbps"
            )
        }

        Divider(color = Color(0xFF32363E), thickness = 1.dp)

        // Latency results
        Text(
            text = "Latency Results",
            color = Color(0xFF4FC3F7),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        if (testState.ping > 0) {
            LatencyMetricRow(
                icon = Icons.Default.Radar,
                label = "Ping",
                value = testState.ping,
                unit = "ms",
                isPing = true
            )
        }

        if (testState.jitter > 0) {
            LatencyMetricRow(
                icon = Icons.Default.ShowChart,
                label = "Jitter",
                value = testState.jitter,
                unit = "ms",
                isPing = false
            )
        }

        testState.packetLoss?.let { packetLoss ->
            PacketLossRow(
                packetLoss = packetLoss
            )
        }
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SpeedMetricRow(
    icon: ImageVector,
    label: String,
    speed: Double,
    unit: String
) {
    val (ratingIcon, ratingColor, ratingText) = getSpeedRating(speed)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF80CBC4),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = "${String.format("%.2f", speed)} $unit",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(100.dp)
        )
        Icon(
            imageVector = ratingIcon,
            contentDescription = ratingText,
            tint = ratingColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = ratingText,
            color = ratingColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun LatencyMetricRow(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    isPing: Boolean
) {
    val (ratingIcon, ratingColor, ratingText) = if (isPing) {
        getPingRating(value)
    } else {
        getJitterRating(value)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF80CBC4),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label:",
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = "$value $unit",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(100.dp)
        )
        Icon(
            imageVector = ratingIcon,
            contentDescription = ratingText,
            tint = ratingColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = ratingText,
            color = ratingColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PacketLossRow(packetLoss: Double) {
    val (ratingIcon, ratingColor, ratingText) = getPacketLossRating(packetLoss)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Packet Loss",
            tint = Color(0xFF80CBC4),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Packet Loss:",
            color = Color(0xFF9E9E9E),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = "${String.format("%.2f", packetLoss)}%",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(100.dp)
        )
        Icon(
            imageVector = ratingIcon,
            contentDescription = ratingText,
            tint = ratingColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = ratingText,
            color = ratingColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ErrorSection(error: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = error,
            color = Color(0xFFF44336),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Helper functions for rating icons and colors
private fun getSpeedRating(speedMbps: Double): Triple<ImageVector, Color, String> {
    return when {
        speedMbps >= 100 -> Triple(Icons.Default.Star, Color(0xFF4CAF50), "Excellent")
        speedMbps >= 50 -> Triple(Icons.Default.TrendingUp, Color(0xFF8BC34A), "Very Good")
        speedMbps >= 25 -> Triple(Icons.Default.ThumbUp, Color(0xFFFFB74D), "Good")
        speedMbps >= 10 -> Triple(Icons.Default.Remove, Color(0xFFFF9800), "Fair")
        speedMbps >= 5 -> Triple(Icons.Default.TrendingDown, Color(0xFFFF5722), "Slow")
        else -> Triple(Icons.Default.Close, Color(0xFFF44336), "Very Slow")
    }
}

private fun getPingRating(pingMs: Int): Triple<ImageVector, Color, String> {
    return when {
        pingMs <= 20 -> Triple(Icons.Default.Star, Color(0xFF4CAF50), "Excellent")
        pingMs <= 50 -> Triple(Icons.Default.ThumbUp, Color(0xFF8BC34A), "Good")
        pingMs <= 100 -> Triple(Icons.Default.Remove, Color(0xFFFFB74D), "Fair")
        pingMs <= 200 -> Triple(Icons.Default.TrendingDown, Color(0xFFFF5722), "Poor")
        else -> Triple(Icons.Default.Close, Color(0xFFF44336), "Very Poor")
    }
}

private fun getJitterRating(jitterMs: Int): Triple<ImageVector, Color, String> {
    return when {
        jitterMs <= 5 -> Triple(Icons.Default.Star, Color(0xFF4CAF50), "Excellent")
        jitterMs <= 15 -> Triple(Icons.Default.ThumbUp, Color(0xFF8BC34A), "Good")
        jitterMs <= 30 -> Triple(Icons.Default.Remove, Color(0xFFFFB74D), "Fair")
        jitterMs <= 50 -> Triple(Icons.Default.TrendingDown, Color(0xFFFF5722), "Poor")
        else -> Triple(Icons.Default.Close, Color(0xFFF44336), "Very Poor")
    }
}

private fun getPacketLossRating(packetLoss: Double): Triple<ImageVector, Color, String> {
    return when {
        packetLoss == 0.0 -> Triple(Icons.Default.Star, Color(0xFF4CAF50), "Perfect")
        packetLoss <= 1.0 -> Triple(Icons.Default.ThumbUp, Color(0xFF8BC34A), "Good")
        packetLoss <= 3.0 -> Triple(Icons.Default.Remove, Color(0xFFFFB74D), "Fair")
        packetLoss <= 5.0 -> Triple(Icons.Default.TrendingDown, Color(0xFFFF5722), "Poor")
        else -> Triple(Icons.Default.Close, Color(0xFFF44336), "Very Poor")
    }
}
