package com.ayaan.mongofsterminal.autocomplete

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.ayaan.mongofsterminal.data.api.GeminiRequest
import com.ayaan.mongofsterminal.data.api.GeminiContent
import com.ayaan.mongofsterminal.data.api.GeminiPart

class AutocompleteManager(
    private val geminiApi: GeminiApi,
    private val coroutineScope: CoroutineScope
) {
    val suggestions = mutableStateListOf<String>()
    var geminiJob: Job? = null
    private var debounceJob: Job? = null
    private val debounceDelay = 500L // 500ms delay

    fun fetchSuggestions(input: String, history: List<String> = emptyList()) {
        // Cancel previous debounce job if it exists
        debounceJob?.cancel()

        if (input.isBlank()) {
            suggestions.clear()
            return
        }

        // Create a new debounce job
        debounceJob = coroutineScope.launch {
            delay(debounceDelay) // Wait for the debounce period

            // Now execute the actual API call
            geminiJob?.cancel()

            val sampleCommands = "ls, cd, mkdir, touch, rm, cat, echo, pwd"
            val historyText = if (history.isNotEmpty()) "Previous commands: ${history.joinToString(", " )}" else ""
            val prompt = "You are a terminal assistant. For the following input, suggest ONLY 3-5 exact shell commands or arguments to complete it. DO NOT provide explanations or descriptions. Return ONLY command strings separated by line breaks. Sample commands: $sampleCommands. $historyText Context: $input"

            geminiJob = launch {
                try {
                    Log.d("GeminiIntegration", "Making API call for: $input")
                    val response = geminiApi.getSuggestions(
                        GeminiRequest(
                            contents = listOf(
                                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                            )
                        )
                    )
                    Log.d("GeminiIntegration", "Response: $response")

                    // Extract just the command suggestions by splitting on newlines and filtering
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    val cleanedSuggestions = text.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("```") && !it.contains("explanation") }
                        .take(5)

                    suggestions.clear()
                    suggestions.addAll(cleanedSuggestions)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) {
                        Log.d("GeminiIntegration", "API call cancelled for: $input")
                    } else {
                        Log.e("GeminiIntegration", "Error fetching suggestions: ", e)
                    }
                    suggestions.clear()
                }
            }
        }
    }
}
