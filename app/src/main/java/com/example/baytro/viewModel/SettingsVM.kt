package com.example.baytro.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.user.PaymentCodeTemplate
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SettingsVM(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private val _template = MutableStateFlow<PaymentCodeTemplate?>(null)
    val template: StateFlow<PaymentCodeTemplate?> = _template.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    private val _prefixQuery = MutableStateFlow("")

    val prefixCheckState: StateFlow<PrefixCheckState> = _prefixQuery
        .debounce(400L)
        .distinctUntilChanged()
        .flatMapLatest { prefix ->
            val trimmedPrefix = prefix.trim()
            if (trimmedPrefix.length < 3) {
                flowOf(PrefixCheckState.Idle)
            } else {
                flow<PrefixCheckState> {
                    emit(PrefixCheckState.Checking)
                    try {
                        val result = functions.getHttpsCallable("isPaymentCodePrefixAvailable")
                            .call(mapOf("prefix" to trimmedPrefix)).await()
                        val isAvailable = (result.data as? Map<*, *>)?.get("isAvailable") as? Boolean
                        emit(if (isAvailable == true) PrefixCheckState.Available else PrefixCheckState.Taken)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("SettingsVM", "Error checking prefix", e)
                        emit(PrefixCheckState.Error("Could not verify prefix."))
                    }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PrefixCheckState.Idle
        )

    init {
        loadCurrentUserTemplate()
    }

    private fun loadCurrentUserTemplate() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val user = userRepository.getById(userId)
                val landlordRole = user?.role as? Role.Landlord
                val currentTemplate = landlordRole?.paymentCodeTemplate
                _template.value = currentTemplate
                currentTemplate?.prefix?.let {
                    _prefixQuery.value = it
                }
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to load user template", e)
                _events.emit(SettingsEvent.ShowToast("Failed to load settings."))
            }
        }
    }

    fun onPrefixQueryChanged(newQuery: String) {
        _prefixQuery.value = newQuery
    }

    fun savePaymentCodeTemplate(suffixLengthStr: String) {
        val currentPrefixState = prefixCheckState.value
        val upperPrefix = _prefixQuery.value.trim().uppercase()
        val suffixLength = suffixLengthStr.toIntOrNull()

        if (currentPrefixState is PrefixCheckState.Checking) {
            viewModelScope.launch { _events.emit(SettingsEvent.ShowToast("Please wait for prefix check to complete.")) }
            return
        }
        if (currentPrefixState is PrefixCheckState.Taken) {
            viewModelScope.launch { _events.emit(SettingsEvent.ShowToast("This prefix is already in use.")) }
            return
        }
        if (upperPrefix.isBlank() || suffixLength == null || suffixLength !in 4..8) {
            viewModelScope.launch { _events.emit(SettingsEvent.ShowToast("Prefix must not be empty and suffix length must be between 4 and 8.")) }
            return
        }

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "prefix" to upperPrefix,
                    "suffixLength" to suffixLength
                )
                val result = functions.getHttpsCallable("set_payment_code_template")
                    .call(data).await()

                val success = (result.data as? Map<*, *>)?.get("success") as? Boolean
                if (success == true) {
                    val newTemplate = PaymentCodeTemplate(upperPrefix, suffixLength)
                    _template.value = newTemplate
                    _events.emit(SettingsEvent.ShowToast("Settings saved successfully!"))
                } else {
                    _events.emit(SettingsEvent.ShowToast("Failed to save settings."))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("SettingsVM", "Failed to save template", e)
                val errorMessage = when {
                    e.message?.contains("ALREADY_EXISTS") == true -> "This prefix is already in use by another user."
                    e.message?.contains("INVALID_ARGUMENT") == true -> "Invalid prefix or suffix length."
                    e.message?.contains("UNAUTHENTICATED") == true -> "You must be logged in to save settings."
                    else -> "Failed to save settings. Please try again."
                }
                _events.emit(SettingsEvent.ShowToast(errorMessage))
            }
        }
    }
}