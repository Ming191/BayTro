package com.example.baytro.service

import com.example.baytro.data.chatbot.ChatQueryRequest
import com.example.baytro.data.chatbot.ChatQueryResponse
import com.example.baytro.data.chatbot.SearchRequest
import com.example.baytro.data.chatbot.SearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class GraphRAGApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://10.0.2.2:5000"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun healthCheck(): Result<Boolean> {
        return try {
            android.util.Log.d("GraphRAGApiService", "Connecting to: $baseUrl/health")
            val response: HttpResponse = httpClient.get {
                url("$baseUrl/health")
            }
            
            android.util.Log.d("GraphRAGApiService", "Response status: ${response.status.value}")
            if (response.status.value in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Health check failed: ${response.status}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("GraphRAGApiService", "Health check error: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun queryLaw(request: ChatQueryRequest): Result<ChatQueryResponse> {
        return try {
            val response: HttpResponse = httpClient.post {
                url("$baseUrl/query")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.value in 200..299) {
                val responseBody: ChatQueryResponse = response.body()
                Result.success(responseBody)
            } else {
                Result.failure(Exception("Query failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun semanticSearch(request: SearchRequest): Result<SearchResponse> {
        return try {
            val response: HttpResponse = httpClient.post {
                url("$baseUrl/search")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            if (response.status.value in 200..299) {
                val responseBody: SearchResponse = response.body()
                Result.success(responseBody)
            } else {
                Result.failure(Exception("Search failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
