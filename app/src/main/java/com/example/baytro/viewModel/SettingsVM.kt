package com.example.baytro.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

sealed class PrefixCheckState {
    object Idle : PrefixCheckState()
    object Checking : PrefixCheckState()
    object Available : PrefixCheckState()
    object Taken : PrefixCheckState()
    data class Error(val message: String) : PrefixCheckState()
}

data class SettingsUiState(
    val prefix: String = "",
    val suffixLength: String = "",
    val prefixCheckState: PrefixCheckState = PrefixCheckState.Idle,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val originalPrefix: String? = null,
    val errorEvent: SingleEvent<String>? = null
)

sealed class SettingsUserEvent {
    data class OnPrefixChange(val newPrefix: String) : SettingsUserEvent()
    data class OnSuffixLengthChange(val newLength: String) : SettingsUserEvent()
    object OnSaveClick : SettingsUserEvent()
    object OnErrorShown : SettingsUserEvent()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SettingsVM(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var checkPrefixJob: Job? = null
    private val currentUserId = auth.currentUser?.uid

    init {
        loadCurrentUserTemplate()
    }

    fun onEvent(event: SettingsUserEvent) {
        when (event) {
            is SettingsUserEvent.OnPrefixChange -> onPrefixChange(event.newPrefix)
            is SettingsUserEvent.OnSuffixLengthChange -> onSuffixLengthChange(event.newLength)
            is SettingsUserEvent.OnSaveClick -> saveConfiguration()
            is SettingsUserEvent.OnErrorShown -> _uiState.update { it.copy(errorEvent = null) }
        }
    }

    private fun loadCurrentUserTemplate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (currentUserId == null) {
                _uiState.update { it.copy(isLoading = false, errorEvent = SingleEvent("User not logged in.")) }
                return@launch
            }
            try {
                val user = userRepository.getById(currentUserId)
                val landlordRole = user?.role as? Role.Landlord
                val template = landlordRole?.paymentCodeTemplate

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        prefix = template?.prefix ?: "",
                        suffixLength = template?.suffixLength?.toString() ?: "6",
                        originalPrefix = template?.prefix
                    )
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to load user template", e)
                _uiState.update { it.copy(isLoading = false, errorEvent = SingleEvent("Failed to load settings.")) }
            }
        }
    }

    private fun onPrefixChange(newPrefix: String) {
        val filteredText = newPrefix.filter { it.isLetterOrDigit() }.uppercase().take(12)
        _uiState.update { it.copy(prefix = filteredText) }

        checkPrefixJob?.cancel()
        checkPrefixJob = viewModelScope.launch {
            delay(400L)
            checkPrefixAvailability(filteredText)
        }
    }

    private fun onSuffixLengthChange(newLength: String) {
        if (newLength.all { it.isDigit() } && newLength.length <= 1) {
            _uiState.update { it.copy(suffixLength = newLength) }
        }
    }

    private suspend fun checkPrefixAvailability(prefix: String) {
        val trimmedPrefix = prefix.trim()
        val currentState = _uiState.value

        if (trimmedPrefix.length < 3 || trimmedPrefix.equals(currentState.originalPrefix, ignoreCase = true)) {
            _uiState.update { it.copy(prefixCheckState = PrefixCheckState.Idle) }
            return
        }

        _uiState.update { it.copy(prefixCheckState = PrefixCheckState.Checking) }
        try {
            val result = functions.getHttpsCallable("isPaymentCodePrefixAvailable")
                .call(mapOf("prefix" to trimmedPrefix)).await()
            val isAvailable = (result.data as? Map<*, *>)?.get("isAvailable") as? Boolean
            val newState = if (isAvailable == true) PrefixCheckState.Available else PrefixCheckState.Taken
            _uiState.update { it.copy(prefixCheckState = newState) }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("SettingsVM", "Error checking prefix", e)
            _uiState.update { it.copy(prefixCheckState = PrefixCheckState.Error("Could not verify prefix.")) }
        }
    }

    private fun saveConfiguration() {
        val currentState = _uiState.value
        val suffixLength = currentState.suffixLength.toIntOrNull()

        if (currentState.isSaving) return
        if (currentState.prefixCheckState is PrefixCheckState.Checking) {
            _uiState.update { it.copy(errorEvent = SingleEvent("Please wait for prefix check to complete.")) }
            return
        }
        if (currentState.prefixCheckState is PrefixCheckState.Taken) {
            _uiState.update { it.copy(errorEvent = SingleEvent("This prefix is already in use.")) }
            return
        }
        if (currentState.prefix.isBlank() || suffixLength == null || suffixLength !in 4..8) {
            _uiState.update { it.copy(errorEvent = SingleEvent("Invalid prefix or suffix length.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val data = mapOf(
                    "prefix" to currentState.prefix,
                    "suffixLength" to suffixLength
                )
                functions.getHttpsCallable("set_payment_code_template").call(data).await()

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        originalPrefix = it.prefix,
                        prefixCheckState = PrefixCheckState.Idle,
                        errorEvent = SingleEvent("Settings saved successfully!")
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("SettingsVM", "Failed to save template", e)
                val errorMessage = e.message ?: "An unknown error occurred."
                _uiState.update { it.copy(isSaving = false, errorEvent = SingleEvent(errorMessage)) }
            }
        }
    }
}