package com.ayaan.mongofsterminal.di

import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.ayaan.mongofsterminal.data.repository.FileSystemRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("FileSystem")
    fun provideRetrofitforFileSystemApi(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://backend-render-m20l.onrender.com") // Default, can be changed at runtime
//            .addConverterFactory(ScalarsConverterFactory.create()) // Add this first for plain text responses
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideFileSystemApi(@Named("FileSystem") retrofit: Retrofit): FileSystemApi {
        return retrofit.create(FileSystemApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFileSystemRepository(
        fileSystemApi: FileSystemApi,
        firebaseAuth: FirebaseAuth
    ): FileSystemRepository {
        return FileSystemRepository(fileSystemApi, firebaseAuth)
    }

    @Provides
    @Singleton
    @Named("Gemini")
    fun provideRetrofitforGeminiApi(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApi(@Named("Gemini") retrofit: Retrofit): GeminiApi{
        return retrofit.create(GeminiApi::class.java)
    }
}
