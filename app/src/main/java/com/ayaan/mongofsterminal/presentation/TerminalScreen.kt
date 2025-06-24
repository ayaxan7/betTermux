package com.ayaan.mongofsterminal.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ayaan.mongofsterminal.presentation.components.TerminalOutputLine
import com.ayaan.mongofsterminal.presentation.components.TerminalPromptLine

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel = hiltViewModel()) {
    val commandInput by viewModel.commandInput
    val commandHistory = viewModel.commandHistory
    val isLoading by viewModel.isLoading
    val workingDir by viewModel.workingDir
    val username by viewModel.username
    val hostname by viewModel.hostname
    val suggestions = viewModel.suggestions
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black, shape = MaterialTheme.shapes.medium)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(scrollState)
                    .fillMaxSize()
            ) {
                commandHistory.forEach { entry ->
                    when (entry) {
                        is TerminalEntry.Prompt -> {
                            TerminalPromptLine(
                                username = username,
                                hostname = hostname,
                                cwd = entry.cwd,
                                command = entry.command
                            )
                        }
                        is TerminalEntry.Output -> {
                            TerminalOutputLine(entry.text, entry.type)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalPromptLine(
                        username = username,
                        hostname = hostname,
                        cwd = workingDir,
                        command = null
                    )
                    TextField(
                        value = commandInput,
                        onValueChange = { viewModel.onCommandInputChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent {
                                when (it.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_ENTER -> {
                                        viewModel.onCommandSubmit()
                                        true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        viewModel.onHistoryUp()
                                        true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        viewModel.onHistoryDown()
                                        true
                                    }
                                    else -> false
                                }
                            },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = TextStyle(color = Color.Green),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.onCommandSubmit()
                                keyboardController?.hide()
                            }
                        )
                    )
                }
                // Autocomplete suggestions (Gemini)
                if (suggestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .background(Color(0xFF23272E), shape = MaterialTheme.shapes.small)
                            .padding(4.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                color = Color(0xFF80CBC4),
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF80CBC4),
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                }
            }
        }
    }
}