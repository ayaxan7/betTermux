package com.ayaan.mongofsterminal.presentation.terminalscreen

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.autocomplete.AutocompleteManager
import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.ayaan.mongofsterminal.data.model.FileSystemRequest
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.data.UiFileSystemNode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.get

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val fileSystemApi: FileSystemApi,
    private val geminiApi: GeminiApi
) : ViewModel() {
    val commandInput = mutableStateOf("")
    val commandHistory = mutableStateListOf<TerminalEntry>()
    val isLoading = mutableStateOf(false)
    val workingDir = mutableStateOf("root")
    val currentPathDisplay = mutableStateOf("~")
    val username = mutableStateOf("ayaan")
    val hostname = mutableStateOf(" )")
    var historyIndex = -1
    // AutocompleteManager instance
    val autocompleteManager = AutocompleteManager(geminiApi, viewModelScope, "") // Set API key as needed
    val suggestions get() = autocompleteManager.suggestions

    fun onCommandInputChange(input: String) {
        commandInput.value = input
        autocompleteManager.fetchSuggestions(input)
    }

    fun onCommandSubmit() {
        val command = commandInput.value.trim()
        if (command.isEmpty()) return
        commandHistory.add(TerminalEntry.Prompt(command, workingDir.value))
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
                "ls" -> {
                    val req = FileSystemRequest(
                        action = "getChildren",
                        parentId = workingDir.value
                    )
                    val res = fileSystemApi.performAction(req)
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
                    val redirectOp = if (commandString.contains(">>")) ">>" else if (commandString.contains(">")) ">" else null

                    if (redirectOp == null) {
                        return TerminalEntry.Output(tokens.drop(1).joinToString(" "), TerminalOutputType.Normal)
                    }

                    val parts = commandString.split(redirectOp, limit = 2)
                    val contentToWrite = parts[0].substringAfter("echo").trim()
                    val filePath = parts.getOrNull(1)?.trim()

                    if (filePath.isNullOrEmpty()) {
                        return TerminalEntry.Output("echo: missing output file", TerminalOutputType.Error)
                    }

                    val append = redirectOp == ">>"

                    val pathParts = filePath.split('/')
                    val fileName = pathParts.last()
                    val dirPath = if (pathParts.size > 1) pathParts.dropLast(1).joinToString("/") else "."

                    val resolveParentReq = FileSystemRequest(action = "resolveNodePath", currentDirId = workingDir.value, targetPath = dirPath)
                    val parentRes = fileSystemApi.performAction(resolveParentReq)
                    if (!parentRes.success) {
                        return TerminalEntry.Output("echo: directory for '$filePath' not found.", TerminalOutputType.Error)
                    }

                    val parentData = parentRes.data as? Map<*, *>
                    val parentDirId = parentData?.get("id") as? String

                    if (parentDirId == null) {
                        return TerminalEntry.Output("echo: could not resolve parent directory.", TerminalOutputType.Error)
                    }

                    val getChildrenReq = FileSystemRequest(action = "getChildren", parentId = parentDirId)
                    val childrenRes = fileSystemApi.performAction(getChildrenReq)

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
                        val updateReq = FileSystemRequest(action = "updateFileNodeContent", id = existingFile.id, newContent = contentToWrite, append = append)
                        val updateRes = fileSystemApi.performAction(updateReq)
                        if (!updateRes.success) {
                            return TerminalEntry.Output(updateRes.error ?: "Failed to update file", TerminalOutputType.Error)
                        }
                    } else {
                        val createReq = FileSystemRequest(action = "createFileNode", name = fileName, parentId = parentDirId, content = contentToWrite, mimeType = "text/plain")
                        val createRes = fileSystemApi.performAction(createReq)
                        if (!createRes.success) {
                            return TerminalEntry.Output(createRes.error ?: "Failed to create file", TerminalOutputType.Error)
                        }
                    }
                    TerminalEntry.Output("", TerminalOutputType.Normal)
                }
                "cd" -> {
                    val target = tokens.getOrNull(1) ?: "~"
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = target
                    )
                    val res = fileSystemApi.performAction(req)
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
                    val res = fileSystemApi.performAction(req)
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
                                val nodeRes = fileSystemApi.performAction(getNodeReq)
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
                    val res = fileSystemApi.performAction(req)
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
                    val res = fileSystemApi.performAction(req)
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
                    val res = fileSystemApi.performAction(req)
                    Log.d("TerminalVM", "rm resolve response: $res")
                    if (res.success && res.data is String) {
                        val nodeId = res.data
                        val delReq = FileSystemRequest(action = "deleteNodeAction", nodeId = nodeId)
                        val delRes = fileSystemApi.performAction(delReq)
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
                    val res = fileSystemApi.performAction(req)
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
                // Add more commands as needed
                else -> TerminalEntry.Output("Unknown command: ${tokens[0]}", TerminalOutputType.Error)
            }
        } catch (e: Exception) {
            Log.e("TerminalVM", "Exception: ${e.message}", e)
            TerminalEntry.Output(e.message ?: "Error", TerminalOutputType.Error)
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
}

sealed class TerminalEntry {
    data class Prompt(val command: String, val cwd: String) : TerminalEntry()
    data class Output(val text: String, val type: TerminalOutputType) : TerminalEntry()
    data class Listing(val items: List<UiFileSystemNode>) : TerminalEntry()
    data class TextOutput(val text: String) : TerminalEntry()
    data class ImageOutput(val base64Data: String) : TerminalEntry()
}

enum class TerminalOutputType {
    Normal, Error, Directory, File
}

data class PathResolutionResponse(val id: String, val path: String)
data class FileNodeResponse(val name: String, val content: String, val mimeType: String)
