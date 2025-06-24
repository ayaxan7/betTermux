package com.ayaan.mongofsterminal.data.model

// Generic response for all file system actions

data class FileSystemResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null,
    val error: String? = null
)

