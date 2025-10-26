package com.example.baytro.service

import android.graphics.Bitmap
import android.util.Log
import com.example.baytro.data.MeterReadingResponse
import com.example.baytro.data.chatbot.ChatQueryRequest
import com.example.baytro.data.chatbot.ChatQueryResponse
import com.example.baytro.data.chatbot.SearchRequest
import com.example.baytro.data.chatbot.SearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Unified API Service for BayTro Backend
 * Handles both GraphRAG Chatbot and Meter Reading
 */
class BayTroApiService(
    private val httpClient: HttpClient,

    private val baseUrl: String = "https://neediest-kellye-weaklier.ngrok-free.dev"
) {
    companion object {
        private const val TAG = "BayTroApiService"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        prettyPrint = true
    }


    suspend fun healthCheck(): Result<Boolean> {
        return try {
            Log.d(TAG, "Health check: $baseUrl/health")
            val response: HttpResponse = httpClient.get {
                url("$baseUrl/health")
            }
            
            Log.d(TAG, "Response status: ${response.status.value}")
            if (response.status.value in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Health check failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health check error: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun queryChatbot(request: ChatQueryRequest): Result<ChatQueryResponse> {
        return try {
            Log.d(TAG, "Chatbot query: ${request.question}")
            val response: HttpResponse = httpClient.post {
                url("$baseUrl/api/chatbot/query")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.value in 200..299) {
                val responseBody: ChatQueryResponse = response.body()
                Log.d(TAG, "Query success: ${responseBody.answer.take(50)}...")
                Result.success(responseBody)
            } else {
                val error = "Query failed: ${response.status}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Query error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchChatbot(request: SearchRequest): Result<SearchResponse> {
        return try {
            Log.d(TAG, "Chatbot search: ${request.query}")
            val response: HttpResponse = httpClient.post {
                url("$baseUrl/api/chatbot/search")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.value in 200..299) {
                val responseBody: SearchResponse = response.body()
                Log.d(TAG, "Search success: ${responseBody.results.size} results")
                Result.success(responseBody)
            } else {
                val error = "Search failed: ${response.status}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun chatbotHealth(): Result<Boolean> {
        return try {
            val response: HttpResponse = httpClient.get {
                url("$baseUrl/api/chatbot/health")
            }
            Result.success(response.status.value in 200..299)
        } catch (e: Exception) {
            Log.e(TAG, "Chatbot health error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== Meter Reading ====================

    suspend fun predictMeterReading(bitmap: Bitmap): Result<MeterReadingResponse> {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            Log.d(TAG, "Meter prediction: Image size ${imageBytes.size} bytes")

            val response: HttpResponse = httpClient.submitFormWithBinaryData(
                url = "$baseUrl/api/meter/predict",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"meter.jpg\"")
                    })
                }
            )

            if (response.status.value in 200..299) {
                val responseText = response.bodyAsText()
                val meterResponse = json.decodeFromString<MeterReadingResponse>(responseText)
                Log.d(TAG, "Meter prediction success: ${meterResponse.text}")
                Result.success(meterResponse)
            } else {
                val error = "Meter prediction failed: ${response.status}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Meter prediction error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun meterHealth(): Result<Boolean> {
        return try {
            val response: HttpResponse = httpClient.get {
                url("$baseUrl/api/meter/health")
            }
            Result.success(response.status.value in 200..299)
        } catch (e: Exception) {
            Log.e(TAG, "Meter health error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
