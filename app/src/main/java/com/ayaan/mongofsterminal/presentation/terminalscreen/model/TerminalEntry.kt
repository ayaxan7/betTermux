package com.ayaan.mongofsterminal.presentation.terminalscreen.model

import com.ayaan.mongofsterminal.presentation.terminalscreen.components.data.UiFileSystemNode

sealed class TerminalEntry {
    data class Prompt(val command: String, val cwd: String) : TerminalEntry()
    data class Output(val text: String, val type: TerminalOutputType) : TerminalEntry()
    data class Listing(val items: List<UiFileSystemNode>) : TerminalEntry()
    data class TextOutput(val text: String) : TerminalEntry()
    data class ImageOutput(val base64Data: String) : TerminalEntry()
}