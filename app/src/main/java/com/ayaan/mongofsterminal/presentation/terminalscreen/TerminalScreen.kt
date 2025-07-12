package com.ayaan.mongofsterminal.presentation.terminalscreen

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.DirectoryEntryLine
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.DisplayImageFromBase64
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.TerminalOutputLine
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.TerminalPromptLine

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TerminalScreen(
    navController: NavController,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    // Observe the logout state
    val isLoggedOut by viewModel.isLoggedOut

    // Navigate to sign-in screen when user logs out
    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            navController.navigate("signin_screen") {
                // Clear the back stack so user can't navigate back to terminal after logout
                popUpTo("terminal_screen") { inclusive = true }
            }
        }
    }

    val commandInput by viewModel.commandInput
    val commandHistory = viewModel.commandHistory
    val isLoading by viewModel.isLoading
    val workingDir by viewModel.workingDir
    val currentPathDisplay by viewModel.currentPathDisplay
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
                                pathDisplay = currentPathDisplay, // Use user-friendly path
                                command = entry.command
                            )
                        }
                        is TerminalEntry.Output -> {
                            TerminalOutputLine(entry.text, entry.type)
                        }
                        is TerminalEntry.Listing -> {
                            // Grid layout for directory listing
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 80.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(((entry.items.size / 4 + 1) * 64).dp)
                                    .padding(vertical = 8.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                items(entry.items) { node ->
                                    DirectoryEntryLine(node = node)
                                }
                            }
                        }
                        is TerminalEntry.TextOutput -> {
                            Text(
                                text = entry.text,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                        is TerminalEntry.ImageOutput -> {
                            DisplayImageFromBase64(
                                base64Data = entry.base64Data,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TerminalPromptLine(
                        username = username,
                        hostname = hostname,
                        cwd = workingDir,
                        pathDisplay = currentPathDisplay,
                        command = null
                    )
                    TextField(
                        value = commandInput,
                        onValueChange = { viewModel.onCommandInputChange(it) },
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent {
                                when (it.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_ENTER -> {
                                        viewModel.onCommandSubmit()
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        viewModel.onHistoryUp()
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
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