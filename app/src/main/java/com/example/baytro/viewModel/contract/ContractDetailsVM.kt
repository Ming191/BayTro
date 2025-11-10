package com.example.baytro.viewModel.contract

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.cloudFunctions.ContractCloudFunctions
import com.example.baytro.utils.cloudFunctions.EndContractResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface QrGenerationState {
    object Idle : QrGenerationState
    object Loading : QrGenerationState
    data class Success(val sessionId: String) : QrGenerationState
    data class Error(val message: String) : QrGenerationState
}

sealed interface EndContractState {
    object Idle : EndContractState
    object Loading : EndContractState
    data class Warning(val response: EndContractResponse) : EndContractState
    data class Success(val response: EndContractResponse) : EndContractState
    data class Error(val message: String) : EndContractState
}

class ContractDetailsVM(
    private val functions: FirebaseFunctions,
    private val contractRepository: ContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val userRepository: UserRepository,
    private val qrSessionRepository: QrSessionRepository,
    private val contractCloudFunctions: ContractCloudFunctions
) : ViewModel() {

    private var contractId: String? = null
    private var dataLoadingJob: Job? = null
    private var pendingSessionsJob: Job? = null

    // --- State Properties ---
    private val _formState = MutableStateFlow(ContractDetailsFormState())
    val formState: StateFlow<ContractDetailsFormState> = _formState.asStateFlow()

    private val _qrState = MutableStateFlow<QrGenerationState>(QrGenerationState.Idle)
    val qrState: StateFlow<QrGenerationState> = _qrState.asStateFlow()

    private val _endContractState = MutableStateFlow<EndContractState>(EndContractState.Idle)
    val endContractState: StateFlow<EndContractState> = _endContractState.asStateFlow()

    private val _pendingSessions = MutableStateFlow<List<PendingQrSession>>(emptyList())
    val pendingSessions: StateFlow<List<PendingQrSession>> = _pendingSessions.asStateFlow()

    private val _confirmingSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val confirmingSessionIds: StateFlow<Set<String>> = _confirmingSessionIds.asStateFlow()

    private val _decliningSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val decliningSessionIds: StateFlow<Set<String>> = _decliningSessionIds.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _isLandlord = MutableStateFlow(false)
    val isLandlord: StateFlow<Boolean> = _isLandlord.asStateFlow()

    fun loadContract(id: String) {
        if (contractId == id) return // Avoid reloading if the ID is the same
        contractId = id

        // Cancel old jobs
        dataLoadingJob?.cancel()
        pendingSessionsJob?.cancel()

        // Start new listeners
        listenForContractUpdates()
        listenForPendingSessions()
        checkUserRole()
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val user = userRepository.getById(currentUserId)
                _isLandlord.value = user?.role is com.example.baytro.data.user.Role.Landlord
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Error checking user role: ${e.message}", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun listenForContractUpdates() {
        val id = contractId ?: return
        dataLoadingJob = viewModelScope.launch {
            contractRepository.getContractFlow(id)
                .filterNotNull()
                .flatMapLatest { contract ->
                    val roomFlow = roomRepository.getRoomFlow(contract.roomId).filterNotNull()
                    val tenantsFlow = if (contract.tenantIds.isNotEmpty()) {
                        userRepository.getUsersByIdsFlow(contract.tenantIds)
                    } else {
                        flowOf(emptyList())
                    }

                    combine(roomFlow, tenantsFlow) { room, tenants ->
                        val building = buildingRepository.getById(room.buildingId)
                        Triple(contract, room, tenants) to building
                    }
                }
                .onStart { _loading.value = true }
                .catch { e ->
                    Log.e("ContractDetailsVM", "Error listening for contract updates", e)
                    _actionError.value = "Failed to load contract details."
                    _loading.value = false
                }
                .collect { (data, building) ->
                    val (contract, room, tenants) = data
                    _formState.value = ContractDetailsFormState(
                        contractNumber = contract.contractNumber,
                        buildingName = building?.name ?: "N/A",
                        roomNumber = room.roomNumber,
                        startDate = contract.startDate,
                        endDate = contract.endDate,
                        rentalFee = contract.rentalFee.toString(),
                        deposit = contract.deposit.toString(),
                        tenantList = tenants,
                        status = contract.status
                    )
                    _loading.value = false
                }
        }
    }

    private fun listenForPendingSessions() {
        val id = contractId ?: return
        pendingSessionsJob = viewModelScope.launch {
            qrSessionRepository.listenForScannedSessions(id)
                .catch { e ->
                    Log.e("ContractDetailsVM", "Error listening for pending sessions", e)
                }
                .collect { sessions ->
                    _pendingSessions.value = sessions
                    if (sessions.isNotEmpty() && _qrState.value is QrGenerationState.Success) {
                        _qrState.value = QrGenerationState.Idle
                    }
                }
        }
    }

    fun generateQrCode() {
        val id = contractId ?: return
        viewModelScope.launch {
            _qrState.value = QrGenerationState.Loading
            try {
                val request = mapOf("contractId" to id)
                val result = functions.getHttpsCallable("generateQrSession").call(request).await()

                Log.d("ContractDetailsVM", "generateQrSession result: ${result.data}")

                val responseData = result.data as? Map<*, *>
                if (responseData == null) {
                    Log.e("ContractDetailsVM", "Response data is null or not a Map")
                    throw Exception("Invalid response format from server")
                }

                val data = responseData["data"] as? Map<*, *>
                val sessionId = data?.get("sessionId") as? String

                if (sessionId.isNullOrBlank()) {
                    Log.e("ContractDetailsVM", "Session ID is null or blank. Response data: $responseData")
                    throw Exception("Server did not return a valid session ID. Please check your internet connection and try again.")
                }

                _qrState.value = QrGenerationState.Success(sessionId)
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Error generating QR code", e)
                _qrState.value = QrGenerationState.Error(parseFirebaseError(e))
            }
        }
    }

    fun confirmTenant(sessionId: String) {
        viewModelScope.launch {
            _confirmingSessionIds.update { it + sessionId }
            try {
                val request = mapOf("sessionId" to sessionId)
                functions.getHttpsCallable("confirmTenantLink").call(request).await()
            } catch (e: Exception) {
                _actionError.value = parseFirebaseError(e)
            } finally {
                _confirmingSessionIds.update { it - sessionId }
            }
        }
    }

    fun declineTenant(sessionId: String) {
        viewModelScope.launch {
            _decliningSessionIds.update { it + sessionId }
            try {
                val request = mapOf("sessionId" to sessionId)
                functions.getHttpsCallable("declineTenantLink").call(request).await()
            } catch (e: Exception) {
                _actionError.value = parseFirebaseError(e)
            } finally {
                _decliningSessionIds.update { it - sessionId }
            }
        }
    }

//    fun endContract(onNavigateBack: () -> Unit = {}) {
//        val id = contractId ?: return
//        viewModelScope.launch {
//            try {
//                contractRepository.updateFields(id, mapOf("status" to Status.ENDED))
//                onNavigateBack()
//            } catch (e: Exception) {
//                _actionError.value = e.message ?: "Failed to end contract."
//            }
//        }
//    }

    private fun parseFirebaseError(e: Exception): String {
        if (e is FirebaseFunctionsException) {
            val details = e.details
            if (details is Map<*, *>) {
                return details["message"] as? String ?: e.message ?: "Firebase error"
            }
            return e.message ?: "Firebase error"
        }
        return e.message ?: "An unknown error occurred."
    }

    fun clearQrCode() {
        _qrState.value = QrGenerationState.Idle
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun endContract(forceEnd: Boolean = false) {
        val currentContractId = contractId ?: run {
            _endContractState.value = EndContractState.Error("Contract ID is not set")
            return
        }

        viewModelScope.launch {
            _endContractState.value = EndContractState.Loading
            try {
                val result = contractCloudFunctions.endContract(currentContractId, forceEnd)

                result.fold(
                    onSuccess = { response ->
                        when (response.status) {
                            "warning" -> {
                                _endContractState.value = EndContractState.Warning(response)
                            }
                            "success" -> {
                                _endContractState.value = EndContractState.Success(response)
                            }
                            else -> {
                                _endContractState.value = EndContractState.Error(response.message)
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("ContractDetailsVM", "Error ending contract: ${error.message}", error)
                        _endContractState.value = EndContractState.Error(
                            error.message ?: "Failed to end contract"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Unexpected error ending contract: ${e.message}", e)
                _endContractState.value = EndContractState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun resetEndContractState() {
        _endContractState.value = EndContractState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        dataLoadingJob?.cancel()
        pendingSessionsJob?.cancel()
    }
}