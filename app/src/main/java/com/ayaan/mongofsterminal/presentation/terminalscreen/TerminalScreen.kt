package com.ayaan.mongofsterminal.presentation.terminalscreen

import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ayaan.mongofsterminal.navigation.Route
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.DirectoryEntryLine
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.DisplayImageFromBase64
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.NetworkQualityDisplay
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.TerminalOutputLine
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.TerminalPromptLine
import com.ayaan.mongofsterminal.presentation.terminalscreen.model.TerminalEntry
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.speedchecker.android.sdk.SpeedcheckerSDK

@Composable
fun TerminalScreen(
    navController: NavController = rememberNavController(), viewModel: TerminalViewModel = hiltViewModel()
) {
    val isLoggedOut by viewModel.isLoggedOut
    val isAccountDeleted by viewModel.isAccountDeleted
    val androidContext= LocalActivity.current
    LaunchedEffect(Unit) {
        SpeedcheckerSDK.askPermissions(androidContext)
    }
//    // Configure system bars
//    val systemUiController = rememberSystemUiController()
//    val terminalBackgroundColor = Color(0xFF121212) // Dark terminal background
//
//    DisposableEffect(systemUiController) {
//        // Make status bar transparent with dark icons
//        systemUiController.setSystemBarsColor(
//            color = Color.Black, darkIcons = false
//        )
//        onDispose {}
//    }

    // Navigate to sign-in screen when user logs out
    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            navController.navigate(Route.SignUpScreen.route) {
                popUpTo(Route.TerminalScreen.route) { inclusive = true }
            }
        }
    }

    // Navigate to sign-in screen when user's account is deleted
    LaunchedEffect(isAccountDeleted) {
        if (isAccountDeleted) {
            navController.navigate(Route.SignUpScreen.route) {
                popUpTo(Route.TerminalScreen.route) { inclusive = true }
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
    LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Terminal gradient background
    val terminalGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E1E1E), Color(0xFF121212)
        )
    )

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            viewModel.onFilePicked(uri)
        }
    )

    // Register file picker launcher with ViewModel
    LaunchedEffect(Unit) {
        SpeedcheckerSDK.askPermissions(androidContext)
        viewModel.registerFilePickerLauncher(filePickerLauncher)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp, color = Color(0xFF32363E), shape = RoundedCornerShape(12.dp)
                ), color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(terminalGradient)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                        .fillMaxSize()
                ) {
                    // Terminal header/title
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "┌─[ BetterMux ]─[ $username@$hostname ]",
                                color = Color(0xFF4FC3F7),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Command history display
                    commandHistory.forEach { entry ->
                        when (entry) {
                            is TerminalEntry.Prompt -> {
                                TerminalPromptLine(
                                    username = username,
                                    hostname = hostname,
                                    cwd = entry.cwd,
                                    pathDisplay = currentPathDisplay,
                                    command = entry.command
                                )
                            }

                            is TerminalEntry.Output -> {
                                TerminalOutputLine(entry.text, entry.type)
                            }

                            is TerminalEntry.Listing -> {
                                // Grid layout for directory listing with styled container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A1D21))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF32363E),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                ) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 80.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(
                                                ((entry.items.size / 4 + 1) * 64).dp.coerceAtMost(
                                                    240.dp
                                                )
                                            )
                                            .padding(8.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        items(entry.items) { node ->
                                            DirectoryEntryLine(node = node)
                                        }
                                    }
                                }
                            }

                            is TerminalEntry.TextOutput -> {
                                Text(
                                    text = entry.text,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }

                            is TerminalEntry.ImageOutput -> {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF4FC3F7),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                ) {
                                    DisplayImageFromBase64(
                                        base64Data = entry.base64Data,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            is TerminalEntry.NetworkQualityOutput -> {
                                NetworkQualityDisplay(
                                    testState = entry.testState,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Command input row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
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
                                cursorColor = Color(0xFF4FC3F7),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = TextStyle(
                                color = Color(0xFF80CBC4), fontFamily = FontFamily.Monospace
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.onCommandSubmit()
                                    keyboardController?.hide()
                                }))
                    }

                    // Autocomplete suggestions (Gemini) with styled container
                    if (suggestions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF23272E))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF4FC3F7).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Column {
                                suggestions.forEach { suggestion ->
                                    Text(
                                        text = "→ $suggestion",
                                        color = Color(0xFF80CBC4),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading indicator with terminal-styled overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF80CBC4),
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Processing...",
                                color = Color(0xFF4FC3F7),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}