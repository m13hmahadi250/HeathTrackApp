package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): JsonObject
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {
    suspend fun analyzeFood(prompt: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) return@withContext emptyList()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a smart food analyzer. The user will input what they ate. You must parse it into a list of specific food items with estimated portions, calories, macros (protein, carbs, fat, fiber). Default to standard serving sizes if not specified. Output strictly as JSON following the schema."))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = buildJsonObject {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("foodName") { put("type", "STRING") }
                                    putJsonObject("portion") { put("type", "STRING") }
                                    putJsonObject("calories") { put("type", "INTEGER") }
                                    putJsonObject("protein") { put("type", "NUMBER") }
                                    putJsonObject("carbs") { put("type", "NUMBER") }
                                    putJsonObject("fat") { put("type", "NUMBER") }
                                    putJsonObject("fiber") { put("type", "NUMBER") }
                                }
                            }
                        }
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response["candidates"]?.jsonArray
                ?.getOrNull(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.getOrNull(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
            
            if (responseText != null) {
                val jsonArray = Json.parseToJsonElement(responseText).jsonArray
                jsonArray.map { element ->
                    val obj = element.jsonObject
                    FoodItem(
                        foodName = obj["foodName"]?.jsonPrimitive?.content ?: "",
                        portion = obj["portion"]?.jsonPrimitive?.content ?: "",
                        calories = obj["calories"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        protein = obj["protein"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
                        carbs = obj["carbs"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
                        fat = obj["fat"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
                        fiber = obj["fiber"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

data class FoodItem(
    val foodName: String,
    val portion: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float
)
