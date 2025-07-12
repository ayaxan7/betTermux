package com.ayaan.mongofsterminal.data.api

import com.ayaan.mongofsterminal.data.model.ApiRequest
import com.ayaan.mongofsterminal.data.model.FileSystemRequest
import com.ayaan.mongofsterminal.data.model.FileSystemResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface FileSystemApi {
    @POST("/api/fs")
    suspend fun performAction(@Body request: ApiRequest): FileSystemResponse
}
