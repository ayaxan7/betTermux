package com.ayaan.mongofsterminal.presentation.terminalscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaan.mongofsterminal.presentation.terminalscreen.components.data.UiFileSystemNode

@Composable
fun DirectoryEntryIcon(node: UiFileSystemNode, modifier: Modifier = Modifier) {
    val (icon, tint) = when {
        node.type == "directory" -> Icons.Filled.Folder to Color(0xFF4FC3F7) // Blue for directories
        node.mimeType?.startsWith("image/") == true -> Icons.Filled.Image to Color(0xFFFFB74D) // Orange for images
        node.mimeType?.contains("code") == true || node.name.endsWith(".kt") || node.name.endsWith(".java") ->
            Icons.Filled.Code to Color(0xFF81C784) // Green for code files
        else -> Icons.Filled.Description to Color(0xFFFFFFFF) // White for regular files
    }

    Icon(
        imageVector = icon,
        contentDescription = node.name,
        tint = tint,
        modifier = modifier.size(22.dp)
    )
}

@Composable
fun DirectoryEntryLine(node: UiFileSystemNode, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DirectoryEntryIcon(node = node)

        Text(
            text = node.name,
            color = when (node.type) {
                "directory" -> Color(0xFF4FC3F7) // Blue for directories
                else -> Color(0xFFFFFFFF) // White for regular files
            },
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
