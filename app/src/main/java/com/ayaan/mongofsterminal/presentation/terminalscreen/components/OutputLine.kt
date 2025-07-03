package com.ayaan.mongofsterminal.presentation.terminalscreen.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ayaan.mongofsterminal.presentation.terminalscreen.TerminalOutputType

@Composable
fun TerminalOutputLine(text: String, type: TerminalOutputType) {
    val color = when (type) {
        TerminalOutputType.Normal -> Color.White
        TerminalOutputType.Error -> Color.Red
        TerminalOutputType.Directory -> Color(0xFF80CBC4)
        TerminalOutputType.File -> Color(0xFFB2DFDB)
    }
    Text(text = text, color = color)
}