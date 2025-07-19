package com.ayaan.mongofsterminal.presentation.auth.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreenFooter() {
    Text(
        text = "└─[~ ]$ _",
        color = Color(0xFF4EB839),
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 16.dp)
    )
}