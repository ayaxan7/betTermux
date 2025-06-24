package com.ayaan.mongofsterminal.presentation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.ayaan.mongofsterminal.data.model.FileSystemRequest
import com.ayaan.mongofsterminal.data.model.FileSystemResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val fileSystemApi: FileSystemApi
) : ViewModel() {
    val commandInput = mutableStateOf("")
    val commandHistory = mutableStateListOf<TerminalEntry>()
    val isLoading = mutableStateOf(false)
    val suggestions = mutableStateListOf<String>()
    val workingDir = mutableStateOf("~")
    val username = mutableStateOf("ayaan")
    val hostname = mutableStateOf("macbook")
    var historyIndex = -1

    fun onCommandInputChange(input: String) {
        commandInput.value = input
        // TODO: Trigger Gemini autocomplete here
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
                    if (res.success) {
                        val list = (res.data as? List<*>)?.joinToString("  ") ?: res.data?.toString() ?: ""
                        TerminalEntry.Output(list, TerminalOutputType.Normal)
                    } else {
                        TerminalEntry.Output(res.error ?: "Unknown error", TerminalOutputType.Error)
                    }
                }
                "cd" -> {
                    val target = tokens.getOrNull(1) ?: "~"
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = target
                    )
                    val res = fileSystemApi.performAction(req)
                    if (res.success && res.data is String) {
                        workingDir.value = res.data
                        TerminalEntry.Output("", TerminalOutputType.Normal)
                    } else {
                        TerminalEntry.Output(res.error ?: "Directory not found", TerminalOutputType.Error)
                    }
                }
                "cat" -> {
                    val file = tokens.getOrNull(1)
                    if (file == null) return TerminalEntry.Output("Usage: cat <file>", TerminalOutputType.Error)
                    val req = FileSystemRequest(
                        action = "resolveNodePath",
                        currentDirId = workingDir.value,
                        targetPath = file
                    )
                    val res = fileSystemApi.performAction(req)
                    if (res.success && res.data is String) {
                        val fileId = res.data
                        val getNodeReq = FileSystemRequest(action = "getNode", id = fileId)
                        val nodeRes = fileSystemApi.performAction(getNodeReq)
                        if (nodeRes.success) {
                            TerminalEntry.Output(nodeRes.data?.toString() ?: "", TerminalOutputType.Normal)
                        } else {
                            TerminalEntry.Output(nodeRes.error ?: "File not found", TerminalOutputType.Error)
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
                    if (res.success && res.data is String) {
                        val nodeId = res.data
                        val delReq = FileSystemRequest(action = "deleteNodeAction", nodeId = nodeId)
                        val delRes = fileSystemApi.performAction(delReq)
                        if (delRes.success) {
                            TerminalEntry.Output("Deleted", TerminalOutputType.Normal)
                        } else {
                            TerminalEntry.Output(delRes.error ?: "Failed to delete", TerminalOutputType.Error)
                        }
                    } else {
                        TerminalEntry.Output(res.error ?: "Not found", TerminalOutputType.Error)
                    }
                }
                // Add more commands as needed
                else -> TerminalEntry.Output("Unknown command: ${tokens[0]}", TerminalOutputType.Error)
            }
        } catch (e: Exception) {
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
}

enum class TerminalOutputType {
    Normal, Error, Directory, File
}
