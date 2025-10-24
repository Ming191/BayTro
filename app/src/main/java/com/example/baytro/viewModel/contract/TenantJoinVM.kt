package com.example.baytro.viewModel.contract

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.service.TenantJoinEventBus
import com.example.baytro.view.screens.UiState
import com.google.firebase.functions.FirebaseFunctions
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TenantJoinVM(
    private val functions: FirebaseFunctions,
    private val qrSessionRepository: QrSessionRepository,
    private val authRepository: AuthRepository,
    private val contractRepository: ContractRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uiState : StateFlow<UiState<String>> = _uiState
    private var eventJob: Job? = null
    private var approvalListenerJob: Job? = null

    init {
        Log.d("TenantJoinVM", "ViewModel initialized, starting pending session check")
        checkForPendingSession()

        eventJob = viewModelScope.launch {
            TenantJoinEventBus.contractConfirmed.collect { contractId ->
                Log.d("TenantJoinVM", "Contract confirmed via FCM: $contractId")
                _uiState.value = UiState.Success(contractId)
            }
        }
    }

    private fun checkForPendingSession() {
        Log.d("TenantJoinVM", "checkForPendingSession() called")
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                Log.d("TenantJoinVM", "Current user: ${currentUser?.uid}")

                if (currentUser != null) {
                    // First check if tenant already has an active contract
                    val activeContract = contractRepository.getActiveContract(currentUser.uid)
                    if (activeContract != null) {
                        Log.d("TenantJoinVM", "Tenant already has active contract: ${activeContract.id}")
                        _uiState.value = UiState.Success("Contract already exists")
                        return@launch
                    }

                    Log.d("TenantJoinVM", "Checking for pending session for user: ${currentUser.uid}")
                    val hasPendingSession = qrSessionRepository.hasScannedSession(currentUser.uid)
                    Log.d("TenantJoinVM", "Has pending session: $hasPendingSession")

                    if (hasPendingSession) {
                        Log.d("TenantJoinVM", "Setting state to Waiting and starting approval listener")
                        _uiState.value = UiState.Waiting
                        startListeningForApproval(currentUser.uid)
                    } else {
                        Log.d("TenantJoinVM", "Setting state to Idle")
                        _uiState.value = UiState.Idle
                    }
                } else {
                    Log.d("TenantJoinVM", "No current user, setting state to Idle")
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                Log.e("TenantJoinVM", "Error checking pending session: ${e.message}", e)
                _uiState.value = UiState.Idle
            }
        }
    }

    private fun startListeningForApproval(tenantId: String) {
        Log.d("TenantJoinVM", "Starting to listen for session approval for tenant: $tenantId")
        approvalListenerJob?.cancel()
        approvalListenerJob = viewModelScope.launch {
            qrSessionRepository.listenForSessionApproval(tenantId).collect { contractId ->
                if (contractId != null) {
                    Log.d("TenantJoinVM", "Session approved! Contract ID: $contractId")
                    _uiState.value = UiState.Success(contractId)
                }
            }
        }
    }

    fun processQrScan(sessionId: String) {
        Log.d("TenantJoinVM", "processQrScan() called with sessionId: $sessionId")

        if (sessionId.isBlank()) {
            Log.w("TenantJoinVM", "Invalid QR code - session ID is blank")
            _uiState.value = UiState.Error("Invalid QR code")
            return
        }

        viewModelScope.launch {
            Log.d("TenantJoinVM", "Processing QR scan, setting state to Loading")
            _uiState.value = UiState.Loading

            try {
                val data = hashMapOf(
                    "sessionId" to sessionId
                )
                Log.d("TenantJoinVM", "Calling Firebase function with data: $data")

                val result = functions
                    .getHttpsCallable("processQrScan")
                    .call(data)
                    .await()

                Log.d("TenantJoinVM", "Function raw result: ${result.data}")

                val response = result.data as? Map<*, *>
                if (response?.get("status") == "success") {
                    Log.d("TenantJoinVM", "QR scan successful, setting state to Waiting")
                    _uiState.value = UiState.Waiting

                    // Start listening for approval immediately
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser != null) {
                        startListeningForApproval(currentUser.uid)
                    }
                } else {
                    Log.w("TenantJoinVM", "Invalid response - status is not 'success'. Full response: $response")
                    _uiState.value = UiState.Error("Invalid response from server")
                }
            } catch (e : Exception) {
                Log.e("TenantJoinVM", "Error processing QR scan: ${e.message}", e)
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun processQrFromUri(context: Context, imageUri: Uri) {
        Log.d("TenantJoinVM", "processQrFromUri() called with URI: $imageUri")
        _uiState.value = UiState.Loading

        try {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val sourceData = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(sourceData))

            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)

            val sessionId = result.text
            Log.d("TenantJoinVM", "QR decoded from URI, sessionId: $sessionId")
            processQrScan(sessionId)
        } catch (e: Exception) {
            Log.e("TenantJoinVM", "Error decoding QR from URI: ${e.message}", e)
            _uiState.value = UiState.Error(e.message ?: "Failed to decode QR with ZXing")
        }
    }

    fun clearState() {
        Log.d("TenantJoinVM", "clearState() called, setting state to Idle")
        approvalListenerJob?.cancel()
        _uiState.value = UiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        eventJob?.cancel()
        approvalListenerJob?.cancel()
        Log.d("TenantJoinVM", "ViewModel cleared, event listeners canceled")
    }
}