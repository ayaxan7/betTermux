package com.ayaan.mongofsterminal.presentation.terminalscreen.components.data

import com.google.gson.annotations.SerializedName

/**
 * UI representation of a file system node (file or directory)
 */
data class UiFileSystemNode(
    val name: String,
    val type: String,
    val id: String,
    @SerializedName("mimeType") val mimeType: String? = null
)