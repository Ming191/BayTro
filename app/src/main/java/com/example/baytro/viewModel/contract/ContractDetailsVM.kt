package com.example.baytro.viewModel.contract

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.UserRepository
import com.example.baytro.view.screens.UiState
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable


class ContractDetailsVM(
    private val functions: FirebaseFunctions,
    private val contractRepository: ContractRepository,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val qrSessionRepository: QrSessionRepository
): ViewModel() {
    private val _qrState = mutableStateOf<UiState<String>>(UiState.Idle)
    val qrState : State<UiState<String>> = _qrState

    private val _formState = MutableStateFlow(ContractDetailsFormState())
    val formState : StateFlow<ContractDetailsFormState> = _formState

    private val _pendingSessions = MutableStateFlow<List<PendingQrSession>>(emptyList())
    val pendingSessions: StateFlow<List<PendingQrSession>> = _pendingSessions

    private val contractId: String = savedStateHandle.get<String>("contractId")!!

    init {
        viewModelScope.launch {
            val contract = contractRepository.getById(contractId)
            val room = roomRepository.getById(contract?.roomId ?: "" )
            val building = buildingRepository.getById(room?.buildingId ?: "")
            val tenantList = contract?.tenantId?.mapNotNull { userId ->
                userRepository.getById(userId)
            } ?: emptyList()
            _formState.value = _formState.value.copy(
                contractNumber = contract?.contractNumber ?: "",
                status = contract?.status ?: Status.PENDING,
                roomNumber = room?.roomNumber ?: "",
                buildingName = building?.name ?: "",
                startDate = contract?.startDate?: "",
                endDate = contract?.endDate?: "",
                tenantList = tenantList,
                rentalFee = contract?.rentalFee?.toString() ?: "",
                deposit = contract?.deposit?.toString() ?: "",
            )
        }
    }

    private fun listenForPendingSessions() {
        viewModelScope.launch {
            qrSessionRepository.listenForScannedSessions(contractId).collect { sessions ->
                _pendingSessions.value = sessions
                Log.d("ContractDetailsVM", "Pending sessions updated: ${sessions.size} items")
            }
        }
    }

    fun confirmTenant(sessionId: String) {
        viewModelScope.launch {
            try {
                val request = mapOf("sessionId" to sessionId)
                functions
                    .getHttpsCallable("confirmTenantLink")
                    .call(request)
                    .await()
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Error confirming tenant", e)
            }
        }
    }

    fun declineTenant(sessionId: String) {
        TODO("Not yet implemented")
    }

    fun generateQrCode() {
        if (contractId.isEmpty()) {
            _qrState.value = UiState.Error("Data for QR code cannot be empty")
            return
        }

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Log.e("ContractDetailsVM", "No authenticated user found")
            _qrState.value = UiState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            _qrState.value = UiState.Loading
            Log.d("ContractDetailsVM", "Starting QR code generation process")

            try {
                Log.d("ContractDetailsVM", "User is authenticated: ${currentUser.uid}")
                val request = mapOf("contractId" to contractId)
                Log.d("ContractDetailsVM", "Created request: $request")

                val result = functions
                    .getHttpsCallable("generate_qr_session")
                    .call(request)
                    .await()

                Log.d("ContractDetailsVM", "Cloud function called successfully")

                val data = result.data as Map<*, *>
                val sessionId = data["sessionId"] as String
                Log.d("ContractDetailsVM", "Received sessionId: $sessionId")

                _qrState.value = UiState.Success(sessionId)
            } catch (e: Exception) {
                Log.e("ContractDetailsVM", "Error generating QR code", e)
                Log.e("ContractDetailsVM", "Error message: ${e.message}")
                Log.e("ContractDetailsVM", "Error type: ${e.javaClass.simpleName}")

                _qrState.value = UiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun clearQrCode() {
        _qrState.value = UiState.Idle
    }
}

@Serializable
data class GenerateQrRequest(val contractId: String)
