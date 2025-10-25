package com.example.baytro.data.chatbot

import android.util.Log
import com.example.baytro.service.GraphRAGApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatbotRepository(
    private val graphRAGApiService: GraphRAGApiService
) {
    companion object {
        private const val TAG = "ChatbotRepository"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: Flow<Boolean> = _isLoading.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    suspend fun checkConnection(): Result<Boolean> {
        return try {
            Log.d(TAG, "Attempting to connect to GraphRAG server...")
            val result = graphRAGApiService.healthCheck()
            _isConnected.value = result.isSuccess
            if (result.isSuccess) {
                Log.d(TAG, "Successfully connected to GraphRAG server")
            } else {
                Log.e(TAG, "Health check failed: ${result.exceptionOrNull()?.message}")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Connection check failed: ${e.javaClass.simpleName} - ${e.message}", e)
            _isConnected.value = false
            Result.failure(e)
        }
    }

    suspend fun sendMessage(question: String): Result<ChatMessage> {
        return try {
            _isLoading.value = true

            // Add user message
            val userMessage = ChatMessage(
                id = generateId(),
                content = question,
                isFromUser = true
            )
            addMessage(userMessage)

            // Query the API
            val request = ChatQueryRequest(question = question)
            val response = graphRAGApiService.queryLaw(request)

            if (response.isSuccess) {
                val queryResponse = response.getOrThrow()
                val botMessage = ChatMessage(
                    id = generateId(),
                    content = queryResponse.answer,
                    isFromUser = false,
                    context = queryResponse.context
                )
                addMessage(botMessage)
                Result.success(botMessage)
            } else {
                val errorMessage = ChatMessage(
                    id = generateId(),
                    content = "Sori, failed to send message rồi",
                    isFromUser = false
                )
                addMessage(errorMessage)
                Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            val errorMessage = ChatMessage(
                id = generateId(),
                content = "Failed to send message, try again",
                isFromUser = false
            )
            addMessage(errorMessage)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun semanticSearch(query: String, topK: Int = 5): Result<SearchResponse> {
        return try {
            val request = SearchRequest(query = query, top_k = topK)
            graphRAGApiService.semanticSearch(request)
        } catch (e: Exception) {
            Log.e(TAG, "Semantic search failed", e)
            Result.failure(e)
        }
    }

    fun addMessage(message: ChatMessage) {
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(message)
        _messages.value = currentMessages
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    private fun generateId(): String {
        return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}
