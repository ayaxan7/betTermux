package com.ayaan.mongofsterminal.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream

@Composable
fun DisplayImageFromBase64(base64Data: String, modifier: Modifier = Modifier) {
// Process base64 string and return bitmap or null if there's an error
    val bitmap = try {
        // Extract just the base64 content if it includes a data URI prefix
        val cleanBase64 = if (base64Data.contains(",")) {
            base64Data.substring(base64Data.indexOf(",") + 1)
        } else {
            base64Data
        }

        // Decode the base64 string to a byte array
        val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

        // Create a bitmap from the byte array
        val inputStream = ByteArrayInputStream(imageBytes)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }

    // Render UI based on bitmap result without try-catch around composable functions
    if (bitmap != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image from terminal",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Failed to display image", color = Color.Red)
        }
    }
}
