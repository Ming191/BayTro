package com.example.baytro.data.chatbot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessage(
    val id: String = "",
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val context: String? = null,
    val isBlocked: Boolean = false  // NEW: Indicates if question was blocked due to role mismatch
)

@Serializable
data class ChatQueryRequest(
    val question: String,
    val user_role: String,  // NEW: "landlord" or "tenant"
    val session_id: String? = null,
    val use_history: Boolean = false,
    val top_k: Int = 5,
    val expand_depth: Int = 2
)

@Serializable
data class ContextNode(
    val id: String,
    val content: String,
    val type: String
)

@Serializable
data class RoleValidation(
    val is_valid: Boolean,
    val action_subject: String,
    val question_type: String,
    val reason: String,
    val suggested_response: String? = null
)

@Serializable
data class ChatQueryResponse(
    val answer: String,
    val context: List<ContextNode>,
    val metadata: JsonElement? = null,
    val session_id: String? = null,
    val role_validation: RoleValidation? = null  // NEW: Role validation result
)

@Serializable
data class SearchRequest(
    val query: String,
    val top_k: Int = 5
)

@Serializable
data class SearchResult(
    val id: String,
    val content: String,
    val type: String,
    val distance: Double
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult>
)
