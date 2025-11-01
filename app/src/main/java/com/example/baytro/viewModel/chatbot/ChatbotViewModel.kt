package com.example.baytro.viewModel.chatbot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.chatbot.ChatMessage
import com.example.baytro.data.chatbot.ChatbotRepository
import com.example.baytro.data.chatbot.SearchResponse
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRoleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatbotViewModel(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    init {
        checkConnection()
        observeMessages()
        observeLoading()
        observeConnection()
        observeUserRole()  // NEW: Observe global user role
    }

    // NEW: Observe user role from global state
    private fun observeUserRole() {
        viewModelScope.launch {
            UserRoleState.userRole.collect { role ->
                val apiRole = when (role) {
                    is Role.Landlord -> "landlord"
                    is Role.Tenant -> "tenant"
                    null -> "tenant"  // Default to tenant if not set
                }
                Log.d("ChatbotViewModel", "Role changed: $role -> API role: $apiRole")
                _uiState.value = _uiState.value.copy(userRole = apiRole)
            }
        }
    }

    private fun checkConnection() {
        viewModelScope.launch {
            chatbotRepository.checkConnection()
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatbotRepository.messages.collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }

    private fun observeLoading() {
        viewModelScope.launch {
            chatbotRepository.isLoading.collect { isLoading ->
                _uiState.value = _uiState.value.copy(isLoading = isLoading)
            }
        }
    }

    private fun observeConnection() {
        viewModelScope.launch {
            chatbotRepository.isConnected.collect { isConnected ->
                _uiState.value = _uiState.value.copy(isConnected = isConnected)
            }
        }
    }

    fun sendMessage(question: String) {
        if (question.isBlank()) return
        
        viewModelScope.launch {
            Log.d("ChatbotViewModel", "Sending message with role: ${_uiState.value.userRole}")
            chatbotRepository.sendMessage(question, _uiState.value.userRole)
        }
    }

    fun clearChat() {
        chatbotRepository.clearMessages()
    }

    fun retryConnection() {
        checkConnection()
    }

    fun semanticSearch(query: String, topK: Int = 5) {
        viewModelScope.launch {
            try {
                val result = chatbotRepository.semanticSearch(query, topK)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(searchResults = result.getOrNull())
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Search failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Unknown error")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = null)
    }

    fun setUserRole(role: String) {
        _uiState.value = _uiState.value.copy(userRole = role)
    }
}

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null,
    val searchResults: SearchResponse? = null,
    val userRole: String = "tenant"  // NEW: Default role is tenant
)
