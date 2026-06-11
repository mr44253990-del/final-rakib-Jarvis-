package com.example.gemini

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class MistralMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class MistralChatRequest(
    val model: String,
    val messages: List<MistralMessage>
)

@JsonClass(generateAdapter = true)
data class MistralChatResponse(
    val choices: List<MistralChoice>? = null
)

@JsonClass(generateAdapter = true)
data class MistralChoice(
    val message: MistralMessage? = null
)

interface MistralApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: MistralChatRequest
    ): MistralChatResponse
}

object MistralRetrofitClient {
    private const val BASE_URL = "https://api.mistral.ai/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: MistralApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(MistralApiService::class.java)
    }
}
