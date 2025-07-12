package com.ayaan.mongofsterminal.data.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.ayaan.mongofsterminal.BuildConfig

data class GeminiRequest(
    val contents: List<GeminiContent>
)
data class GeminiContent(
    val parts: List<GeminiPart>
)
data class GeminiPart(
    val text: String
)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)
data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApi {
    @Headers(
        "Content-Type: application/json",
        "X-goog-api-key: ${BuildConfig.GEMINI_API_KEY}"
    )
    @POST("/v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun getSuggestions(
        @Body request: GeminiRequest
    ): GeminiResponse
}
