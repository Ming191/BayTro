package com.example.baytro.data.chatbot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessage(
    val id: String = "",
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val context: String? = null
)

@Serializable
data class ChatQueryRequest(
    val question: String,
    val k: Int = 5,
    val expand_depth: Int = 2
)

@Serializable
data class ChatQueryResponse(
    val answer: String,
    val context: String,
    val question: String
)

@Serializable
data class SearchRequest(
    val query: String,
    val top_k: Int = 5
)

@Serializable
data class SearchResult(
    val id: String,
    val metadata: Map<String, JsonElement>,
    val document: String,
    val score: Double,
    val distance: Double
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult>,
    val query: String
)
