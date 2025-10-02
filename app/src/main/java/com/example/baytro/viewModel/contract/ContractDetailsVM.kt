package com.example.baytro.viewModel.contract

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.UserRepository
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


sealed interface QrGenerationState {
    object Idle : QrGenerationState
    object Loading : QrGenerationState
    data class Success(val sessionId: String) : QrGenerationState
    data class Error(val message: String) : QrGenerationState
}

class ContractDetailsVM(
    private val functions: FirebaseFunctions,
    private val contractRepository: ContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val userRepository: UserRepository,
    private val qrSessionRepository: QrSessionRepository,
): ViewModel() {

    private var contractId: String? = null

    fun loadContract(id: String) {
        contractId = id
        listenForContractUpdates()
        listenForPendingSessions()
    }

    private val _formState = MutableStateFlow(ContractDetailsFormState())
    val formState: StateFlow<ContractDetailsFormState> = _formState
    private val _qrState = mutableStateOf<QrGenerationState>(QrGenerationState.Idle)
    val qrState: State<QrGenerationState> = _qrState
    private val _pendingSessions = MutableStateFlow<List<PendingQrSession>>(emptyList())
    val pendingSessions: StateFlow<List<PendingQrSession>> = _pendingSessions
    private val _confirmingSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val confirmingSessionIds: StateFlow<Set<String>> = _confirmingSessionIds
    private val _decliningSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val decliningSessionIds: StateFlow<Set<String>> = _decliningSessionIds
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private fun listenForContractUpdates() {
        viewModelScope.launch {
            val id = contractId ?: return@launch
            _loading.value = true
            contractRepository.getContractFlow(id).collect { contract ->
                if (contract != null) {
                    Log.d("ContractDetailsVM", "contract.tenantIds: ${contract.tenantIds}")
                    val room = roomRepository.getById(contract.roomId)
                    val building = buildingRepository.getById(room?.buildingId ?: "")
                    Log.d("ContractDetailsVM", "Calling getUsersByIds with: ${contract.tenantIds}")
                    val tenants = if (contract.tenantIds.isNotEmpty()) {
                        val users = userRepository.getUsersByIds(contract.tenantIds)
                        Log.d("ContractDetailsVM", "getUsersByIds result: $users")
                        users
                    } else {
                        emptyList()
                    }

                    val newFormState = ContractDetailsFormState(
                        contractNumber = contract.contractNumber,
                        buildingName = building?.name ?: "N/A",
                        roomNumber = room?.roomNumber ?: "N/A",
                        startDate = contract.startDate,
                        endDate = contract.endDate,
                        rentalFee = contract.rentalFee.toString(),
                        deposit = contract.deposit.toString(),
                        status = contract.status,
                        tenantList = tenants
                    )
                    Log.d("ContractDetailsVM", "formState: $newFormState")
                    _formState.value = newFormState
                    _loading.value = false
                }
            }
        }
    }

    private fun listenForPendingSessions() {
        viewModelScope.launch {
            val id = contractId ?: return@launch
            qrSessionRepository.listenForScannedSessions(id).collect {
                sessions ->
                _pendingSessions.value = sessions
            }
        }
    }

    fun generateQrCode() {
        Log.d("ContractDetailsVM", "generateQrCode called for contractId: $contractId")
        val id = contractId ?: return
        if (id.isBlank()) {
            Log.e("ContractDetailsVM", "Contract ID is missing.")
            _qrState.value = QrGenerationState.Error("Contract ID is missing.")
            return
        }
        viewModelScope.launch {
            _qrState.value = QrGenerationState.Loading
            try {
                val request = mapOf("contractId" to id)
                Log.d("ContractDetailsVM", "Calling generateQrSession with request: $request")
                val result = functions.getHttpsCallable("generate_qr_session").call(request).await()
                val data = result.data as? Map<*, *>
                val sessionId = data?.get("sessionId") as? String ?: throw Exception("sessionId missing in response")
                Log.d("ContractDetailsVM", "QR generation success, sessionId: $sessionId")
                _qrState.value = QrGenerationState.Success(sessionId)
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "QR generation error: ${e.message}", e)
                _qrState.value = QrGenerationState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun confirmTenant(sessionId: String) {
        Log.d("ContractDetailsVM", "confirmTenant called for sessionId: $sessionId")
        viewModelScope.launch {
            _confirmingSessionIds.value += sessionId
            try {
                val request = mapOf("sessionId" to sessionId)
                Log.d("ContractDetailsVM", "Calling confirmTenantLink with request: $request")
                functions.getHttpsCallable("confirm_tenant_link").call(request).await()
                Log.d("ContractDetailsVM", "Tenant confirmed for sessionId: $sessionId")
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Failed to confirm tenant: ${e.message}", e)
                _actionError.value = e.message ?: "Failed to confirm tenant."
            } finally {
                _confirmingSessionIds.value -= sessionId
            }
        }
    }

    fun declineTenant(sessionId: String) {
        Log.d("ContractDetailsVM", "declineTenant called for sessionId: $sessionId")
        viewModelScope.launch {
            _decliningSessionIds.value += sessionId
            try {
                val request = mapOf("sessionId" to sessionId)
                Log.d("ContractDetailsVM", "Calling declineTenantLink with request: $request")
                functions.getHttpsCallable("decline_tenant_link").call(request).await()
                Log.d("ContractDetailsVM", "Tenant declined for sessionId: $sessionId")
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Failed to decline tenant: ${e.message}", e)
                _actionError.value = e.message ?: "Failed to decline tenant."
            } finally {
                _decliningSessionIds.value -= sessionId
            }
        }
    }

    fun clearQrCode() {
        _qrState.value = QrGenerationState.Idle
    }

    fun clearActionError() {
        _actionError.value = null
    }
}