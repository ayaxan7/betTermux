package com.ayaan.mongofsterminal.autocomplete

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.ayaan.mongofsterminal.data.api.GeminiRequest
import com.ayaan.mongofsterminal.data.api.GeminiContent
import com.ayaan.mongofsterminal.data.api.GeminiPart

class AutocompleteManager(
    private val geminiApi: GeminiApi,
    private val coroutineScope: CoroutineScope,
    private val geminiApiKey: String
) {
    val suggestions = mutableStateListOf<String>()
    var geminiJob: Job? = null

    fun fetchSuggestions(input: String) {
        geminiJob?.cancel()
        if (input.isBlank() || geminiApiKey.isBlank()) {
            suggestions.clear()
            return
        }
        geminiJob = coroutineScope.launch {
            try {
                val prompt = "You are an expert in using the macos terminal. Suggest the next possible command or argument for a shell terminal. Context: $input"
                val response = geminiApi.getSuggestions(
                    GeminiRequest(
                        contents = listOf(
                            GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                        )
                    )
                )
                val suggestionList = response.candidates?.mapNotNull { it.content?.parts?.firstOrNull()?.text } ?: emptyList()
                suggestions.clear()
                suggestions.addAll(suggestionList)
            } catch (e: Exception) {
                suggestions.clear()
            }
        }
    }
}

