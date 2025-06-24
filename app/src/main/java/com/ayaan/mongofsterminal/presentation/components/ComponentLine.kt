package com.ayaan.mongofsterminal.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TerminalPromptLine(username: String, hostname: String, cwd: String, command: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$username$hostname",
            color = Color(0xFF80CBC4),
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        if (command != null) {
            Text(text = command, color = Color.Green)
        }
    }
}