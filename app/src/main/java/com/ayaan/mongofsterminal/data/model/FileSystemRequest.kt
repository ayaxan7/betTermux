package com.ayaan.mongofsterminal.data.model

// Generic request for all file system actions

data class FileSystemRequest(
    val action: String,
    val id: String? = null,
    val parentId: String? = null,
    val nodeId: String? = null,
    val currentDirId: String? = null,
    val targetPath: String? = null,
    val name: String? = null,
    val content: String? = null,
    val mimeType: String? = null,
    val newContent: String? = null,
    val append: Boolean? = null,
    val recursive: Boolean? = null,
    val newParentId: String? = null,
    val newName: String? = null,
    val pattern: String? = null,
    val fileId: String? = null
)

