package com.ayaan.mongofsterminal.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Utility class for handling file operations, especially for the upload feature
 */
object FileUtils {

    /**
     * Gets the file name from a content URI
     */
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "unknown_file"

        // Try to get the display name from the content provider
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        return fileName
    }

    /**
     * Gets the MIME type from a content URI
     */
    fun getMimeType(context: Context, uri: Uri): String {
        // First try content resolver
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null) {
            return mimeType
        }

        // If content resolver failed, try to get it from file extension
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return if (fileExtension != null) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension) ?: "application/octet-stream"
        } else {
            "application/octet-stream" // Default fallback
        }
    }

    /**
     * Gets a human-readable file size string from a URI
     */
    fun getReadableFileSize(context: Context, uri: Uri): String {
        var fileSize: Long = 0

        // Try to get the size from the content provider
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        // If we couldn't get the size from the content provider, try to read the content
        if (fileSize == 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    fileSize = inputStream.available().toLong()
                }
            } catch (e: Exception) {
                Log.e("FileUtils", "Error getting file size", e)
            }
        }

        return formatFileSize(fileSize)
    }

    /**
     * Formats a file size in bytes to a human-readable string
     */
    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()

        return String.format("%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    /**
     * Converts file content to string or base64 depending on the MIME type
     * @return A pair containing the content string and a boolean indicating if it's base64 encoded
     */
    fun getFileContent(context: Context, uri: Uri, mimeType: String): Pair<String, Boolean> {
        return try {
            if (isTextFile(mimeType)) {
                // For text files, read as string
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                            stringBuilder.append("\n")
                        }
                    }
                }
                Pair(stringBuilder.toString(), false)
            } else {
                // For binary files, convert to base64
                val byteArrayOutputStream = ByteArrayOutputStream()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        byteArrayOutputStream.write(buffer, 0, len)
                    }
                }
                val bytes = byteArrayOutputStream.toByteArray()
                val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                Pair(base64String, true)
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error reading file content", e)
            throw IOException("Could not read file: ${e.message}")
        }
    }

    /**
     * Determines if a file is a text file based on its MIME type
     */
    fun isTextFile(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
               mimeType == "application/json" ||
               mimeType == "application/xml" ||
               mimeType == "application/javascript" ||
               mimeType == "application/typescript"
    }

    /**
     * Gets the file size in bytes
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        // Try to get the size from the content provider
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }

        // If that fails, try to get it by reading the file
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                return inputStream.available().toLong()
            }
        } catch (e: IOException) {
            Log.e("FileUtils", "Error getting file size", e)
        }

        return 0
    }
}
