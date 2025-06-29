package com.ayaan.mongofsterminal.data.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
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
    @Headers("Content-Type: application/json")
    @POST("/v1beta/models/gemini-pro:generateContent")
    suspend fun getSuggestions(
        @Body request: GeminiRequest
    ): GeminiResponse
}

