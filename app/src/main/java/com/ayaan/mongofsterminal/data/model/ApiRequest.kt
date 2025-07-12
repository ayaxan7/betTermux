package com.ayaan.mongofsterminal.data.model

/**
 * New model for API requests that conform to the updated backend structure
 * which requires parameters to be nested in a payload object
 */
data class ApiRequest(
    val action: String,
    val uid: String,
    val payload: Map<String, Any?>
)
