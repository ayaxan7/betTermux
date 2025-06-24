package com.ayaan.mongofsterminal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data class for path resolution response from the 'cd' command
 */
data class PathResolutionResponse(
    val id: String,
    val path: String
)

/**
 * Data class for file node response from the 'cat' command
 */
data class FileNodeResponse(
    val name: String,
    val content: String,
    val mimeType: String
)
