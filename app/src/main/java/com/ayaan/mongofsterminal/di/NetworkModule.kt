package com.ayaan.mongofsterminal.di

import com.ayaan.mongofsterminal.data.api.FileSystemApi
import com.ayaan.mongofsterminal.data.api.GeminiApi
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    @Named("FileSystem")
    fun provideRetrofitforFileSystemApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://backend-render-m20l.onrender.com") // Default, can be changed at runtime
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(OkHttpClient.Builder().build())
            .build()
    }

    @Provides
    @Singleton
    fun provideFileSystemApi(@Named("FileSystem") retrofit: Retrofit): FileSystemApi {
        return retrofit.create(FileSystemApi::class.java)
    }

    @Provides
    @Singleton
    @Named("Gemini")
    fun provideRetrofitforGeminiApi(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(OkHttpClient.Builder().build())
            .build()
    }
    @Provides
    @Singleton
    fun provideGeminiApi(@Named("Gemini") retrofit: Retrofit): GeminiApi{
        return retrofit.create(GeminiApi::class.java)
    }
}
