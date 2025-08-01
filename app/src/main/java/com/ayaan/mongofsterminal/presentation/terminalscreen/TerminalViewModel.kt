package com.ayaan.mongofsterminal.presentation.terminalscreen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.autocomplete.AutocompleteManager
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.ayaan.mongofsterminal.data.model.FileSystemRequest
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.ayaan.mongofsterminal.network.NetworkQualityManager
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.data.UiFileSystemNode
import com.ayaan.mongofsterminal.presentation.terminalscreen.model.FileNodeResponse
import com.ayaan.mongofsterminal.presentation.terminalscreen.model.PathResolutionResponse
import com.ayaan.mongofsterminal.presentation.terminalscreen.model.TerminalEntry
import com.ayaan.mongofsterminal.presentation.terminalscreen.model.TerminalOutputType
import com.ayaan.mongofsterminal.utils.FileUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.speedchecker.android.sdk.SpeedcheckerSDK
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.get

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val fileSystemRepository: FileSystemRepository, // Using Repository instead of direct API
    geminiApi: GeminiApi,
    private val firebaseAuth: FirebaseAuth,
    val networkQualityManager: NetworkQualityManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val commandInput = mutableStateOf("")
    val commandHistory = mutableStateListOf<TerminalEntry>()
    // List to store command history as strings for autocomplete context
    private val commandHistoryStrings = mutableListOf<String>()
    val isLoading = mutableStateOf(false)
    val workingDir = mutableStateOf("root")
    val currentPathDisplay = mutableStateOf("~")
    val username = mutableStateOf("")
    val hostname = mutableStateOf(" )")
    var historyIndex = -1
    // State to track if user has logged out
    val isLoggedOut = mutableStateOf(false)
    // State to track if account has been deleted
    val isAccountDeleted = mutableStateOf(false)
    // State to track authentication errors
    val authError = mutableStateOf<String?>(null)
    // AutocompleteManager instance
    val autocompleteManager = AutocompleteManager(geminiApi, viewModelScope)
    val suggestions get() = autocompleteManager.suggestions

    // File picker launcher for selecting files to upload
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    // Variable to store the target filename for upload
    private var uploadTargetFilename: String? = null

    init {
        // Set username from Firebase display name or email when available
        updateUsernameFromFirebase()
    }

    private fun updateUsernameFromFirebase() {
        val user = firebaseAuth.currentUser
        username.value = when {
            !user?.displayName.isNullOrEmpty() -> user?.displayName ?: "user"
            !user?.email.isNullOrEmpty() -> user?.email?.split("@")?.first() ?: "user"
            else -> "user"
        }
    }

    fun onCommandInputChange(input: String) {
        commandInput.value = input
        Log.d("GeminiIntegration", "Fetching Gemini suggestions for input: $input")
        // Pass the command history to the fetchSuggestions function
        autocompleteManager.fetchSuggestions(input, commandHistoryStrings)
    }
    fun onCommandSubmit() {
        val command = commandInput.value.trim()
        if (command.isEmpty()) return
        commandHistory.add(TerminalEntry.Prompt(command, workingDir.value))
        commandHistoryStrings.add(command) // Add to history strings for autocomplete
        commandInput.value = ""
        isLoading.value = true
        viewModelScope.launch {
            val output = handleCommand(command)
            commandHistory.add(output)
            isLoading.value = false
        }
    }

    private suspend fun handleCommand(command: String): TerminalEntry {
        val tokens = command.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return TerminalEntry.Output("", TerminalOutputType.Normal)
        return try {
            when (tokens[0]) {
                "upload" -> {
                    val targetFilename = tokens.getOrNull(1)
                    if (targetFilename != null) {
                        uploadTargetFilename = targetFilename
                    }

                    // Launch file picker
                    filePickerLauncher.launch("*/*")

                    // Return a message indicating upload process has started
                    TerminalEntry.Output("Opening file picker... Select a file to upload" +
                        (if (targetFilename != null) " as '$targetFilename'" else ""),
                        TerminalOutputType.Normal)
                }

                "ls" -> {
                    val req = FileSystemRequest(
                        action = "getChildren",
                        parentId = workingDir.value
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "ls response: $res")
                    if (res.success) {
                        try {
                            // Handle when data is a List
                            if (res.data is List<*>) {
                                val items = (res.data as List<*>).mapNotNull { item ->
                                    if (item is Map<*, *>) {
                                        val name = (item["name"] as? String) ?: ""
                                        val type = (item["type"] as? String) ?: "unknown"
                                        val id = (item["id"] as? String) ?: ""
                                        val mimeType = item["mimeType"] as? String
                                        UiFileSystemNode(name, type, id, mimeType)
                                    } else null
                                }
                                if (items.isNotEmpty()) {
                                    return TerminalEntry.Listing(items)
                                }
                            }

                            // For safety, try to convert via Gson if the above approach fails
                            val gson = Gson()
                            val jsonString = gson.toJson(res.data)
                            val itemType = object : TypeToken<List<UiFileSystemNode>>() {}.type
                            val items = gson.fromJson<List<UiFileSystemNode>>(jsonString, itemType)

                            if (items != null && items.isNotEmpty()) {
                                TerminalEntry.Listing(items)
                            } else {
                                // Fallback to string representation
                                val text = res.data?.toString() ?: ""
                                TerminalEntry.Output(text, TerminalOutputType.Normal)
                            }
                        } catch (e: Exception) {
                            Log.e("TerminalVM", "Error parsing ls output", e)
                            // Fallback to string representation
                            val text = res.data?.toString() ?: ""
                            TerminalEntry.Output(text, TerminalOutputType.Normal)
                        }
                    } else {
                        TerminalEntry.Output(res.error ?: "Unknown error", TerminalOutputType.Error)
                    }
                }

                "echo" -> {
                    val commandString = tokens.joinToString(" ")

                    // Check if this might be a direct file write without redirection operators
                    if (!commandString.contains(">") && tokens.size >= 3) {
                        // This could be "echo content filename" format
                        val content = tokens.subList(1, tokens.size - 1).joinToString(" ")
                        val filename = tokens.last()

                        // Only treat as file write if the last token doesn't have quotes and
                        // looks like a potential filename (contains no spaces)
                        if (!filename.contains("\"") && !filename.contains(" ")) {
                            return handleEchoFileWrite(content, filename, false)
                        }
                    }

                    // Improved redirection detection that handles >> and > properly
                    val appendRedirect = ">>"
                    val overwriteRedirect = ">"

                    val redirectOp = when {
                        commandString.contains(appendRedirect) -> appendRedirect
                        commandString.contains(overwriteRedirect) -> overwriteRedirect
                        else -> null
                    }

                    if (redirectOp == null) {
                        return TerminalEntry.Output(tokens.drop(1).joinToString(" "), TerminalOutputType.Normal)
                    }

                    // Split command on first occurrence of redirection operator
                    // This handles case when content includes quotes or special characters
                    val contentPart = commandString.substringBefore(redirectOp)
                    val filePart = commandString.substringAfter(redirectOp)

                    val contentToWrite = contentPart.substringAfter("echo").trim()
                    val filePath = filePart.trim()

                    if (filePath.isEmpty()) {
                        return TerminalEntry.Output("echo: missing output file", TerminalOutputType.Error)
                    }

                    val append = redirectOp == appendRedirect

                    // Add debug logging
                    Log.d("TerminalVM", "Echo redirect: op=$redirectOp, content='$contentToWrite', file='$filePath', append=$append")

                    return handleEchoFileWrite(contentToWrite, filePath, append)
                }
                "cd" -> {
                    val target = tokens.getOrNull(1) ?: "~"
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = target
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "cd response: $res")
                    if (res.success) {
                        try {
                            // Handle both formats: legacy string format and new object format
                            when (res.data) {
                                // Case 1: String format (legacy API) - just ID
                                is String -> {
                                    workingDir.value = res.data
                                    // For display, use target if it starts with / or ~, otherwise append to current path
                                    if (target.startsWith("/") || target.startsWith("~")) {
                                        currentPathDisplay.value = target
                                    } else if (target == "..") {
                                        // Go up one level in path
                                        val current = currentPathDisplay.value
                                        currentPathDisplay.value = current.substringBeforeLast('/', "~")
                                    } else {
                                        // Append to current path unless it's already at root
                                        if (currentPathDisplay.value == "~" || currentPathDisplay.value.endsWith("/")) {
                                            currentPathDisplay.value += target
                                        } else {
                                            currentPathDisplay.value += "/$target"
                                        }
                                    }
                                    TerminalEntry.Output("", TerminalOutputType.Normal)
                                }

                                // Case 2: Map format (new API) - contains id and path
                                is Map<*, *> -> {
                                    val id = (res.data as Map<*, *>)["id"] as? String
                                    val path = (res.data as Map<*, *>)["path"] as? String

                                    if (id != null) {
                                        workingDir.value = id
                                        if (path != null) {
                                            currentPathDisplay.value = path
                                        } else {
                                            // Fallback if path is missing but id exists
                                            currentPathDisplay.value = if (target.startsWith("/") || target.startsWith("~")) {
                                                target
                                            } else {
                                                "${currentPathDisplay.value}/$target"
                                            }
                                        }
                                        TerminalEntry.Output("", TerminalOutputType.Normal)
                                    } else {
                                        TerminalEntry.Output("Invalid directory response", TerminalOutputType.Error)
                                    }
                                }

                                // Case 3: Other formats (unexpected)
                                else -> {
                                    // Try to convert using Gson as last resort
                                    try {
                                        val gson = Gson()
                                        val json = gson.toJson(res.data)
                                        val pathRes = gson.fromJson(json, PathResolutionResponse::class.java)
                                        if (pathRes.id != null) {
                                            workingDir.value = pathRes.id
                                            currentPathDisplay.value = pathRes.path ?: target
                                            TerminalEntry.Output("", TerminalOutputType.Normal)
                                        } else {
                                            TerminalEntry.Output("Invalid directory response", TerminalOutputType.Error)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TerminalVM", "Error converting cd response via Gson", e)
                                        TerminalEntry.Output("Directory not found", TerminalOutputType.Error)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TerminalVM", "Error parsing cd response", e)
                            TerminalEntry.Output("Directory not found", TerminalOutputType.Error)
                        }
                    } else {
                        TerminalEntry.Output(res.error ?: "Directory not found", TerminalOutputType.Error)
                    }
                }

                "cat" -> {
                    val file = tokens.getOrNull(1)
                    if (file == null) return TerminalEntry.Output("Usage: cat <file>", TerminalOutputType.Error)

                    // First resolve the file path to get the ID
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = file
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "cat resolve response: $res")

                    if (res.success) {
                        try {
                            // Parse the path resolution response
                            val gson = Gson()
                            val pathData = if (res.data is Map<*, *>) {
                                val id = (res.data as Map<*, *>)["id"] as? String
                                if (id != null) id else null
                            } else if (res.data is String) {
                                res.data
                            } else {
                                try {
                                    val pathRes = gson.fromJson(gson.toJson(res.data), PathResolutionResponse::class.java)
                                    pathRes.id
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (pathData != null) {
                                // Now get the file node content
                                val fileId = pathData.toString()
                                val getNodeReq = FileSystemRequest(action = "getNode", id = fileId)
                                val nodeRes = fileSystemRepository.performAction(getNodeReq)
                                Log.d("TerminalVM", "cat getNode response: $nodeRes")

                                if (nodeRes.success) {
                                    // Parse the file node response
                                    val fileNode = if (nodeRes.data is Map<*, *>) {
                                        val name = (nodeRes.data as Map<*, *>)["name"] as? String ?: ""
                                        val content = (nodeRes.data as Map<*, *>)["content"] as? String ?: ""
                                        val mimeType = (nodeRes.data as Map<*, *>)["mimeType"] as? String ?: ""
                                        FileNodeResponse(name, content, mimeType)
                                    } else {
                                        gson.fromJson(gson.toJson(nodeRes.data), FileNodeResponse::class.java)
                                    }

                                    // Return appropriate TerminalEntry based on mime type
                                    return when {
                                        fileNode.mimeType.startsWith("image/") -> {
                                            TerminalEntry.ImageOutput(fileNode.content)
                                        }
                                        fileNode.mimeType == "text/plain" ||
                                                fileNode.mimeType == "application/json" ||
                                                fileNode.mimeType.contains("text") -> {
                                            TerminalEntry.TextOutput(fileNode.content)
                                        }
                                        else -> {
                                            TerminalEntry.Output(
                                                "File type ${fileNode.mimeType} is not viewable in the terminal",
                                                TerminalOutputType.Error
                                            )
                                        }
                                    }
                                } else {
                                    TerminalEntry.Output(nodeRes.error ?: "File not found", TerminalOutputType.Error)
                                }
                            } else {
                                TerminalEntry.Output("Failed to resolve file path", TerminalOutputType.Error)
                            }
                        } catch (e: Exception) {
                            Log.e("TerminalVM", "Error in cat command", e)
                            TerminalEntry.Output("Error: ${e.message}", TerminalOutputType.Error)
                        }
                    } else {
                        TerminalEntry.Output(res.error ?: "File not found", TerminalOutputType.Error)
                    }
                }
                "mkdir" -> {
                    val dir = tokens.getOrNull(1)
                    if (dir == null) return TerminalEntry.Output("Usage: mkdir <dir>", TerminalOutputType.Error)
                    val req = FileSystemRequest(
                        action = "createDirectoryNode",
                        name = dir,
                        parentId = workingDir.value
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "mkdir response: $res")
                    if (res.success) {
                        TerminalEntry.Output("Directory created", TerminalOutputType.Normal)
                    } else {
                        TerminalEntry.Output(res.error ?: "Failed to create directory", TerminalOutputType.Error)
                    }
                }
                "touch" -> {
                    val file = tokens.getOrNull(1)
                    if (file == null) return TerminalEntry.Output("Usage: touch <file>", TerminalOutputType.Error)
                    val req = FileSystemRequest(
                        action = "createFileNode",
                        name = file,
                        parentId = workingDir.value
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "touch response: $res")
                    if (res.success) {
                        TerminalEntry.Output("File created", TerminalOutputType.Normal)
                    } else {
                        TerminalEntry.Output(res.error ?: "Failed to create file", TerminalOutputType.Error)
                    }
                }
                "rm" -> {
                    val target = tokens.getOrNull(1)
                    if (target == null) return TerminalEntry.Output("Usage: rm <file|dir>", TerminalOutputType.Error)
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = target
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "rm resolve response: $res")
                    if (res.success && res.data is String) {
                        val nodeId = res.data
                        val delReq = FileSystemRequest(action = "deleteNodeAction", nodeId = nodeId)
                        val delRes = fileSystemRepository.performAction(delReq)
                        Log.d("TerminalVM", "rm delete response: $delRes")
                        if (delRes.success) {
                            TerminalEntry.Output("Deleted", TerminalOutputType.Normal)
                        } else {
                            TerminalEntry.Output(delRes.error ?: "Failed to delete", TerminalOutputType.Error)
                        }
                    } else {
                        TerminalEntry.Output(res.error ?: "Not found", TerminalOutputType.Error)
                    }
                }
                "pwd" -> {
                    val req = FileSystemRequest(
                        action = "getNodePathString",
                        nodeId = workingDir.value
                    )
                    val res = fileSystemRepository.performAction(req)
                    Log.d("TerminalVM", "pwd response: $res")
                    if (res.success) {
                        TerminalEntry.Output(res.data?.toString() ?: "", TerminalOutputType.Normal)
                    } else {
                        TerminalEntry.Output(res.error ?: "Failed to get path", TerminalOutputType.Error)
                    }
                }
                "history" -> {
                    val history = commandHistory.filterIsInstance<TerminalEntry.Prompt>()
                        .mapIndexed { idx, entry -> "  ${idx + 1}: ${entry.command}" }
                        .joinToString("\n")
                    TerminalEntry.Output(history, TerminalOutputType.Normal)
                }
                "clear" -> {
                    commandHistory.clear()
                    TerminalEntry.Output("", TerminalOutputType.Normal)
                }

                "logout" -> {
                    // Sign out from Firebase Auth
                    firebaseAuth.signOut()
                    // Update the logged out state
                    isLoggedOut.value = true
                    // Return a message indicating successful logout
                    TerminalEntry.Output("Logging out...", TerminalOutputType.Normal)
                }

                "deleteaccount" -> {
                    val currentUser = firebaseAuth.currentUser

                    if (currentUser == null) {
                        return TerminalEntry.Output("Error: You must be logged in to delete your account", TerminalOutputType.Error)
                    }

                    // Check if the user entered the confirmation parameter
                    val confirmParam = tokens.getOrNull(1)
                    if (confirmParam != "--confirm") {
                        return TerminalEntry.Output(
                            "Warning: This will permanently delete your account and all associated data.\n" +
                            "To confirm, type: deleteaccount --confirm",
                            TerminalOutputType.Error
                        )
                    }

                    try {
                        // Try to delete the user account
                        isLoading.value = true

                        // Get the current user's UID for backend data deletion
                        val uid = currentUser.uid

                        // First, make API call to delete user data from backend
                        val deleteDataReq = FileSystemRequest(
                            action = "deleteUserAccount",
                            payload = mapOf("userId" to uid)
                        )
                        viewModelScope.launch {
                            try {
                                // Make the API call to delete user data
                                val deleteDataRes = fileSystemRepository.performAction(deleteDataReq)

                                if (deleteDataRes.success) {
                                    // Log successful data deletion
                                    val deletedCount = (deleteDataRes.data as? Map<*, *>)?.get("deletedCount") as? Int ?: 0
                                    val message = (deleteDataRes.data as? Map<*, *>)?.get("message") as? String ?: ""
                                    Log.d("TerminalVM", "Backend data deletion successful: $message ($deletedCount items removed)")

                                    commandHistory.add(TerminalEntry.Output(
                                        "Successfully deleted all user data: $deletedCount files and directories removed.",
                                        TerminalOutputType.Normal
                                    ))
                                } else {
                                    Log.e("TerminalVM", "Failed to delete backend data: ${deleteDataRes.error}")
                                    commandHistory.add(TerminalEntry.Output(
                                        "Warning: Failed to delete your data from our servers. Please contact support.",
                                        TerminalOutputType.Error
                                    ))
                                }

                                // Now delete the Firebase user account
                                deleteFirebaseAccount(currentUser)

                            } catch (e: Exception) {
                                Log.e("TerminalVM", "Exception during backend data deletion: ${e.message}", e)
                                commandHistory.add(TerminalEntry.Output(
                                    "Warning: Failed to delete your data from our servers. Please contact support.",
                                    TerminalOutputType.Error
                                ))

                                // Still attempt to delete the Firebase account
                                deleteFirebaseAccount(currentUser)
                            }
                        }

                        return TerminalEntry.Output("Deleting account and all associated data...", TerminalOutputType.Normal)
                    } catch (e: Exception) {
                        isLoading.value = false
                        Log.e("TerminalVM", "Exception during account deletion: ${e.message}", e)
                        return TerminalEntry.Output("Error: ${e.message}", TerminalOutputType.Error)
                    }
                }
                "help" -> {
                    val helpText = buildString {
//                        appendLine("╭─────────────────────────────────────────────────────────────╮")
//                        appendLine("│                    BetterMux Command Reference             │")
//                        appendLine("╰─────────────────────────────────────────────────────────────╯")
                        appendLine()
                        appendLine("FILE & DIRECTORY OPERATIONS:")
                        appendLine("  ls                    - List files and directories in current location")
                        appendLine("  cd <directory>        - Change to specified directory (use .. for parent)")
                        appendLine("  pwd                   - Show current directory path")
                        appendLine("  mkdir <name>          - Create a new directory")
                        appendLine("  touch <filename>      - Create a new empty file")
                        appendLine("  rm <file|dir>         - Delete specified file or directory")
                        appendLine("  cat <filename>        - Display file contents (supports text and images)")
                        appendLine()
                        appendLine("FILE CONTENT OPERATIONS:")
                        appendLine("  echo <text>           - Display text or write to file with > or >>")
                        appendLine("  echo <text> > <file>  - Write text to file (overwrites existing content)")
                        appendLine("  echo <text> >> <file> - Append text to file (preserves existing content)")
                        appendLine()
                        appendLine("SYSTEM & NAVIGATION:")
                        appendLine("  clear                 - Clear the terminal screen")
                        appendLine("  history               - Show command history")
                        appendLine("  help                  - Display this help information")
                        appendLine()
                        appendLine("NETWORK DIAGNOSTICS:")
                        appendLine("  networkquality start  - Run comprehensive network speed test")
                        appendLine("  networkquality status - Show current test status and results")
                        appendLine("  networkquality logs   - View detailed test logs")
                        appendLine("  networkquality help   - Show network testing command help")
                        appendLine()
                        appendLine("ACCOUNT MANAGEMENT:")
                        appendLine("  logout                - Sign out of current account")
                        appendLine("  deleteaccount         - Permanently delete account and all data")
                        appendLine()
                        appendLine("TIPS & SHORTCUTS:")
                        appendLine("  • Use arrow keys (↑/↓) to navigate command history")
                        appendLine("  • Tab completion suggestions appear automatically")
                        appendLine("  • AI-powered command suggestions via Gemini integration")
                        appendLine("  • Use 'networkquality status' during tests for real-time progress")
                        appendLine("  • File operations support both absolute and relative paths")
                        appendLine()
                        appendLine("EXAMPLES:")
                        appendLine("  mkdir projects && cd projects")
                        appendLine("  echo 'Hello World' > welcome.txt")
                        appendLine("  cat welcome.txt")
                        appendLine("  networkquality start")
                        appendLine()
                        appendLine("For more details on any command, try using it with common patterns")
                        appendLine("or check the AI suggestions that appear as you type.")
                        appendLine("NOTE: The AI just provides terminal commands in suggestions which means it is not aware of the application's limitations.")
                    }
                    Log.d("helpText", helpText)
                    TerminalEntry.Output(helpText, TerminalOutputType.Normal)
                }

                "networkquality" -> {
                    // Handle network quality testing command
                    val subCommand = tokens.getOrNull(1)
                    when (subCommand) {
                        "start", null -> {
                            // Start the network speed test
                            try {
                                networkQualityManager.startSpeedTest()

                                // Start the speed test using the SDK - following reference implementation
                                viewModelScope.launch {
                                    try {
                                        // Use SpeedTest.startTest instead of SpeedcheckerSDK.startTest
                                        SpeedcheckerSDK.SpeedTest.setOnSpeedTestListener(networkQualityManager)
                                        SpeedcheckerSDK.SpeedTest.startTest(context)
                                    } catch (e: Exception) {
                                        Log.e("TerminalVM", "Failed to start network test: ${e.message}", e)
                                        commandHistory.add(TerminalEntry.Output("✗ Failed to start network test: ${e.message}", TerminalOutputType.Error))
                                    }
                                }

                                TerminalEntry.Output("→ Initializing network speed test...", TerminalOutputType.Normal)
                            } catch (e: Exception) {
                                Log.e("TerminalVM", "Error starting network test: ${e.message}", e)
                                TerminalEntry.Output("✗ Error: ${e.message}", TerminalOutputType.Error)
                            }
                        }

                        "status" -> {
                            // Show current test status with visual component
                            val testState = networkQualityManager.testState.value
                            TerminalEntry.NetworkQualityOutput(testState)
                        }

                        "logs" -> {
                            // Show test logs
                            val testState = networkQualityManager.testState.value
                            if (testState.logMessages.isNotEmpty()) {
                                val logsText = buildString {
                                    appendLine("▤ Network Test Logs:")
                                    appendLine("════════════════════════════════════════")
                                    testState.logMessages.take(20).forEach { log ->
                                        appendLine("► $log")
                                    }
                                    appendLine("════════════════════════════════════════")
                                }
                                TerminalEntry.Output(logsText, TerminalOutputType.Normal)
                            } else {
                                TerminalEntry.Output("▤ No logs available", TerminalOutputType.Normal)
                            }
                        }

                        else -> {
                            TerminalEntry.Output("✗ Unknown networkquality command: $subCommand\n? Use 'networkquality help' for available commands", TerminalOutputType.Error)
                        }
                    }
                }


                // Add more commands as needed
                else -> TerminalEntry.Output("Unknown command: ${tokens[0]}", TerminalOutputType.Error)
            }
        } catch (e: Exception) {
            Log.e("TerminalVM", "Exception: ${e.message}", e)
            TerminalEntry.Output(e.message ?: "Error", TerminalOutputType.Error)
        }
    }

    private suspend fun handleEchoFileWrite(content: String, filePath: String, append: Boolean): TerminalEntry {
        // Add debug logging to help troubleshoot
        Log.d("TerminalVM", "handleEchoFileWrite: content='$content', filePath='$filePath', append=$append")

        val pathParts = filePath.split('/')
        val fileName = pathParts.last()
        val dirPath = if (pathParts.size > 1) pathParts.dropLast(1).joinToString("/") else "."

        Log.d("TerminalVM", "handleEchoFileWrite: fileName='$fileName', dirPath='$dirPath', workingDir='${workingDir.value}'")

        // If we're writing to a file in the current directory, use workingDir directly
        // instead of trying to resolve "." which might be causing issues
        if (dirPath == "." || dirPath.isEmpty()) {
            // We're in the current directory, use workingDir directly
            val parentDirId = workingDir.value

            // Get children to check if file exists
            val getChildrenReq = FileSystemRequest(action = "getChildren", parentId = parentDirId)
            val childrenRes = fileSystemRepository.performAction(getChildrenReq)

            if (!childrenRes.success) {
                return TerminalEntry.Output("echo: could not check for existing file.", TerminalOutputType.Error)
            }

            val gson = Gson()
            val childrenJson = gson.toJson(childrenRes.data)
            val itemType = object : TypeToken<List<UiFileSystemNode>>() {}.type
            val items = gson.fromJson<List<UiFileSystemNode>>(childrenJson, itemType) ?: emptyList()

            val existingFile = items.find { it.name == fileName && it.type == "file" }
            val existingDir = items.find { it.name == fileName && it.type == "directory" }

            if (existingDir != null) {
                return TerminalEntry.Output("echo: cannot write to '$filePath': Is a directory", TerminalOutputType.Error)
            }

            if (existingFile != null) {
                if (existingFile.mimeType != null && existingFile.mimeType != "text/plain") {
                    return TerminalEntry.Output("echo: cannot write to '${fileName}': Not a plain text file.", TerminalOutputType.Error)
                }
                val updateReq = FileSystemRequest(action = "updateFileNodeContent", id = existingFile.id, newContent = content, append = append)
                val updateRes = fileSystemRepository.performAction(updateReq)
                if (!updateRes.success) {
                    return TerminalEntry.Output(updateRes.error ?: "Failed to update file", TerminalOutputType.Error)
                }
            } else {
                val createReq = FileSystemRequest(action = "createFileNode", name = fileName, parentId = parentDirId, content = content, mimeType = "text/plain")
                val createRes = fileSystemRepository.performAction(createReq)
                if (!createRes.success) {
                    return TerminalEntry.Output(createRes.error ?: "Failed to create file", TerminalOutputType.Error)
                }
            }
            return TerminalEntry.Output("", TerminalOutputType.Normal)

        } else {
            // Original code for paths with directories
            val resolveParentReq = FileSystemRequest(action = "resolveNodePath", currentDirId = workingDir.value, targetPath = dirPath)
            val parentRes = fileSystemRepository.performAction(resolveParentReq)
            Log.d("TerminalVM", "handleEchoFileWrite: resolveParentReq response success=${parentRes.success}, data=${parentRes.data}")

            if (!parentRes.success) {
                return TerminalEntry.Output("echo: directory for '$filePath' not found.", TerminalOutputType.Error)
            }

            val parentData = parentRes.data as? Map<*, *>
            val parentDirId = parentData?.get("id") as? String

            if (parentDirId == null) {
                return TerminalEntry.Output("echo: could not resolve parent directory.", TerminalOutputType.Error)
            }

            val getChildrenReq = FileSystemRequest(action = "getChildren", parentId = parentDirId)
            val childrenRes = fileSystemRepository.performAction(getChildrenReq)

            if (!childrenRes.success) {
                return TerminalEntry.Output("echo: could not check for existing file.", TerminalOutputType.Error)
            }

            val gson = Gson()
            val childrenJson = gson.toJson(childrenRes.data)
            val itemType = object : TypeToken<List<UiFileSystemNode>>() {}.type
            val items = gson.fromJson<List<UiFileSystemNode>>(childrenJson, itemType) ?: emptyList()

            val existingFile = items.find { it.name == fileName && it.type == "file" }
            val existingDir = items.find { it.name == fileName && it.type == "directory" }

            if (existingDir != null) {
                return TerminalEntry.Output("echo: cannot write to '$filePath': Is a directory", TerminalOutputType.Error)
            }

            if (existingFile != null) {
                if (existingFile.mimeType != null && existingFile.mimeType != "text/plain") {
                    return TerminalEntry.Output("echo: cannot write to '${fileName}': Not a plain text file.", TerminalOutputType.Error)
                }
                val updateReq = FileSystemRequest(action = "updateFileNodeContent", id = existingFile.id, newContent = content, append = append)
                val updateRes = fileSystemRepository.performAction(updateReq)
                if (!updateRes.success) {
                    return TerminalEntry.Output(updateRes.error ?: "Failed to update file", TerminalOutputType.Error)
                }
            } else {
                val createReq = FileSystemRequest(action = "createFileNode", name = fileName, parentId = parentDirId, content = content, mimeType = "text/plain")
                val createRes = fileSystemRepository.performAction(createReq)
                if (!createRes.success) {
                    return TerminalEntry.Output(createRes.error ?: "Failed to create file", TerminalOutputType.Error)
                }
            }
            return TerminalEntry.Output("", TerminalOutputType.Normal)
        }
    }

    private fun deleteFirebaseAccount(user: com.google.firebase.auth.FirebaseUser) {
        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Set the account deleted flag
                isAccountDeleted.value = true

                // Log the successful deletion
                Log.d("TerminalVM", "Firebase user account successfully deleted")
            } else {
                // If deletion fails, show the error
                val error = task.exception?.message ?: "Unknown error"
                Log.e("TerminalVM", "Failed to delete Firebase account: $error")
                commandHistory.add(TerminalEntry.Output(
                    "Failed to delete account: $error\nYou may need to re-authenticate.",
                    TerminalOutputType.Error
                ))
            }
            isLoading.value = false
        }
    }

    fun onHistoryUp() {
        if (commandHistory.isEmpty()) return
        if (historyIndex == -1) historyIndex = commandHistory.size - 1
        else if (historyIndex > 0) historyIndex--
        val entry = commandHistory[historyIndex]
        if (entry is TerminalEntry.Prompt) {
            commandInput.value = entry.command
        }
    }

    fun onHistoryDown() {
        if (commandHistory.isEmpty() || historyIndex == -1) return
        if (historyIndex < commandHistory.size - 1) historyIndex++
        val entry = commandHistory[historyIndex]
        if (entry is TerminalEntry.Prompt) {
            commandInput.value = entry.command
        }
    }

    // Helper functions for network quality display
    private fun generateProgressBar(current: Int, max: Int, width: Int = 30): String {
        val percentage = (current.toFloat() / max.toFloat() * 100).toInt().coerceIn(0, 100)
        val filled = (current.toFloat() / max.toFloat() * width).toInt().coerceIn(0, width)
        val empty = width - filled

        return buildString {
            append("▐")
            repeat(filled) { append("█") }
            repeat(empty) { append("░") }
            append("▌ $percentage%")
        }
    }

    private fun getSpeedRating(speedMbps: Double): String {
        return when {
            speedMbps >= 100 -> "★ Excellent"
            speedMbps >= 50 -> "▲ Very Good"
            speedMbps >= 25 -> "● Good"
            speedMbps >= 10 -> "○ Fair"
            speedMbps >= 5 -> "▽ Slow"
            else -> "✗ Very Slow"
        }
    }

    private fun getPingRating(pingMs: Int): String {
        return when {
            pingMs <= 20 -> "★ Excellent"
            pingMs <= 50 -> "● Good"
            pingMs <= 100 -> "○ Fair"
            pingMs <= 200 -> "▽ Poor"
            else -> "✗ Very Poor"
        }
    }

    private fun getJitterRating(jitterMs: Int): String {
        return when {
            jitterMs <= 5 -> "★ Excellent"
            jitterMs <= 15 -> "● Good"
            jitterMs <= 30 -> "○ Fair"
            jitterMs <= 50 -> "▽ Poor"
            else -> "✗ Very Poor"
        }
    }

    private fun getPacketLossRating(packetLoss: Double): String {
        return when {
            packetLoss == 0.0 -> "★ Perfect"
            packetLoss <= 1.0 -> "● Good"
            packetLoss <= 3.0 -> "○ Fair"
            packetLoss <= 5.0 -> "▽ Poor"
            else -> "✗ Very Poor"
        }
    }

    // Initialize the file picker launcher
    fun registerFilePickerLauncher(launcher: ActivityResultLauncher<String>) {
        filePickerLauncher = launcher
    }

    // Handle the result of the file picker
    fun onFilePicked(uri: Uri?) {
        if (uri == null) {
            // No file was picked
            commandHistory.add(TerminalEntry.Output("⚠ File selection cancelled", TerminalOutputType.Error))
            isLoading.value = false
            uploadTargetFilename = null
            return
        }

        viewModelScope.launch {
            try {
                isLoading.value = true

                // Get file info - use the original file name only for display
                val originalFileName = FileUtils.getFileName(context, uri)
                val mimeType = FileUtils.getMimeType(context, uri)
                val fileSize = FileUtils.getReadableFileSize(context, uri)

                // Use the specified target filename from the command
                val targetFileName = uploadTargetFilename ?: originalFileName

                // Show processing message
                commandHistory.add(TerminalEntry.Output(
                    "→ Processing file: $originalFileName ($mimeType, $fileSize)...",
                    TerminalOutputType.Normal
                ))

                // Read file content based on its type
                val (fileContent, isBase64) = FileUtils.getFileContent(context, uri, mimeType)

                // Create request payload with the target filename
                val req = FileSystemRequest(
                    action = "createFileNode",
                    name = targetFileName,
                    parentId = workingDir.value,
                    content = fileContent,
                    mimeType = mimeType
                )

                // Send request to API
                val res = fileSystemRepository.performAction(req)

                Log.d("TerminalVM", "Upload response: $res")

                if (res.success) {
                    commandHistory.add(TerminalEntry.Output(
                        "✓ File uploaded successfully as '$targetFileName' to ${currentPathDisplay.value}",
                        TerminalOutputType.Normal
                    ))
                } else {
                    commandHistory.add(TerminalEntry.Output(
                        "✗ Upload failed: ${res.error ?: "Unknown error"}",
                        TerminalOutputType.Error
                    ))
                }
            } catch (e: Exception) {
                Log.e("TerminalVM", "Error uploading file", e)
                commandHistory.add(TerminalEntry.Output(
                    "✗ Error: ${e.message}",
                    TerminalOutputType.Error
                ))
            } finally {
                isLoading.value = false
                uploadTargetFilename = null
            }
        }
    }
}
