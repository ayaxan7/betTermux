package com.ayaan.mongofsterminal.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun TerminalPromptLine(
    username: String,
    hostname: String,
    cwd: String,
    pathDisplay: String,
    command: String?
) {
    val promptColor = Color(0xFF4FC3F7)  // Light blue for prompt
    val commandColor = Color.White

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Display the prompt with user@host:path$ format
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.Green)) {
                    append(username)
                }
                append("@")
                withStyle(style = SpanStyle(color = Color.Green)) {
                    append(hostname)
                }
                append(":")
                withStyle(style = SpanStyle(color = promptColor)) {
                    append(pathDisplay)
                }
                withStyle(style = SpanStyle(color = Color.Yellow)) {
                    append(" $ ")
                }
            }
        )

        // Display the command if provided
        if (!command.isNullOrEmpty()) {
            Text(text = command, color = commandColor)
        }
    }
}
