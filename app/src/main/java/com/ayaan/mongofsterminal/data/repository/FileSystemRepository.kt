package com.ayaan.mongofsterminal.data.repository

import android.util.Log
import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.ayaan.mongofsterminal.data.model.ApiRequest
import com.ayaan.mongofsterminal.data.model.FileSystemRequest
import com.ayaan.mongofsterminal.data.model.FileSystemResponse
import com.google.firebase.auth.FirebaseAuth
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.full.memberProperties

/**
 * Repository that handles file system operations with user authentication
 * Automatically injects Firebase UID into all requests and converts to the required API format
 */
@Singleton
class FileSystemRepository @Inject constructor(
    private val fileSystemApi: FileSystemApi,
    private val firebaseAuth: FirebaseAuth
) {
//    /**
//     * Performs a file system action with automatic UID injection and auth checks
//     * @param request The file system request without UID
//     * @return FileSystemResponse from the API
//     * @throws IllegalStateException if user is not authenticated
//     */
    suspend fun performAction(request: FileSystemRequest): FileSystemResponse {
        val currentUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("User not authenticated. Please login first.")

        val uid = currentUser.uid

        // Convert request to payload format expected by the updated API
        val payload = buildPayloadFromRequest(request)
        // Create API request with proper structure
        val apiRequest = ApiRequest(
            action = request.action,
            uid = uid,
            payload = payload
        )

        return try {
            Log.d(
                "FileSystemRepository",
                "Performing action: ${request.action} for user: $uid with body: $apiRequest"
            )
            fileSystemApi.performAction(apiRequest)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("API_ERROR", "HTTP ${e.code()}: $errorBody")
            Log.e("API_ERROR", "Request URL: ${e.response()?.raw()?.request?.url}")
            FileSystemResponse(
                success = false,
                error = "HTTP ${e.code()}: $errorBody",
                data = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle network errors and return a properly formatted response
            FileSystemResponse(
                success = false,
                error = e.message ?: "Network error occurred",
                data = null
            )
        }
    }

    /**
     * Initializes the file system for a newly registered user
     * @param uid The user ID to initialize the filesystem for
     * @return FileSystemResponse from the API
     */
    suspend fun initializeFileSystem(uid: String): FileSystemResponse {
        val apiRequest = ApiRequest(
            action = "initializeFileSystem",
            uid = uid,
            payload = emptyMap()
        )

        return try {
            fileSystemApi.performAction(apiRequest)
        } catch (e: Exception) {
            e.printStackTrace()
            FileSystemResponse(
                success = false,
                error = e.message ?: "Failed to initialize file system",
                data = null
            )
        }
    }

    /**
     * Builds a payload map from a FileSystemRequest by extracting non-null properties
     * excluding action and uid which are top-level fields
     */
    private fun buildPayloadFromRequest(request: FileSystemRequest): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>()
        // If the request already contains a payload, use that as the base
        if (request.payload != null) {
            return request.payload
        }
        // Otherwise build payload from individual fields
        // Add all non-null fields to the payload except action and uid
        if (request.id != null) payload["id"] = request.id
        if (request.parentId != null) payload["parentId"] = request.parentId
        if (request.nodeId != null) payload["nodeId"] = request.nodeId
        if (request.currentDirId != null) payload["currentDirId"] = request.currentDirId
        if (request.targetPath != null) payload["targetPath"] = request.targetPath
        if (request.name != null) payload["name"] = request.name
        if (request.content != null) payload["content"] = request.content
        if (request.mimeType != null) payload["mimeType"] = request.mimeType
        if (request.newContent != null) payload["newContent"] = request.newContent
        if (request.append != null) payload["append"] = request.append
        if (request.recursive != null) payload["recursive"] = request.recursive
        if (request.newParentId != null) payload["newParentId"] = request.newParentId
        if (request.newName != null) payload["newName"] = request.newName
        if (request.pattern != null) payload["pattern"] = request.pattern
        if (request.fileId != null) payload["fileId"] = request.fileId
        if (request.uid != null) payload["userId"] = request.uid
        return payload
    }
}

