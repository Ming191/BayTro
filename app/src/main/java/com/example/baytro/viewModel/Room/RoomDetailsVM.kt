package com.example.baytro.viewModel.Room

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.Status
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.service.Service
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.BuildingCloudFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RoomDetailsVM(
    private val roomRepository: RoomRepository,
    private val contractRepository: ContractRepository,
    private val userRepository: UserRepository,
    private val buildingRepository: BuildingRepository,
    private val buildingCloudFunctions: BuildingCloudFunctions,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val _buildingServices = MutableStateFlow<List<Service>>(emptyList())
    val buildingServices: StateFlow<List<Service>> = _buildingServices

    private val _extraServices = MutableStateFlow<List<Service>>(emptyList())
    val extraServices: StateFlow<List<Service>> = _extraServices


    private val _contract = MutableStateFlow<List<Contract>>(emptyList())
    val contract: StateFlow<List<Contract>> = _contract

    private val _tenants = MutableStateFlow<List<User>>(emptyList())
    val tenants: StateFlow<List<User>> = _tenants

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isDeleteOnClicked = MutableStateFlow(false)
    val isDeleteOnClicked = _isDeleteOnClicked

    private val _isDeletingRoom = MutableStateFlow(false)
    val isDeletingRoom: StateFlow<Boolean> = _isDeletingRoom

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private val _successEvent = MutableSharedFlow<SingleEvent<String>>()
    val successEvent: SharedFlow<SingleEvent<String>> = _successEvent.asSharedFlow()

    fun loadRoom() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val room = roomRepository.getById(roomId)
                Log.d("RoomDetailsVM", "RoomInRoomDetailsVM: $room")
                _room.value = room
                loadBuildingServices()
                listenToExtraServicesRealtime()
            } catch (e: Exception) {
                e.printStackTrace()
                _room.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadBuildingServices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val buildingId = _room.value?.buildingId
                if (buildingId != null) {
                    val services = buildingRepository.getServicesByBuildingId(buildingId)
                    _buildingServices.value = services
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _buildingServices.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRoomContract() {
        viewModelScope.launch {
            try {
                val contracts = contractRepository.getContractsByRoomId(roomId)
                Log.d("RoomDetailsVM", "ContractsInRoomDetailsVM: ${contracts.size}")
                val filteredContracts = contracts.filter { it.status != Status.ENDED }
                _contract.value = filteredContracts
            } catch (e: Exception) {
                e.printStackTrace()
                _contract.value = emptyList()
            }
        }
    }
    fun getRoomTenants() {
        viewModelScope.launch {
            try {
                val tenantsIds = contractRepository.getTenantsByRoomId(roomId) // list string of tenantIds
                Log.d("RoomDetailsVM", "TenantsIdsInRoomDetailsVM: ${tenantsIds.size}")
                val tenants = userRepository.getUsersByIds(tenantsIds) // list of User that is tenant
                Log.d("RoomDetailsVM", "TenantsInRoomDetailsVM: ${tenants.size}")
                _tenants.value = tenants
            } catch (e : Exception) {
                e.printStackTrace()
                _tenants.value = emptyList()
            }
        }
    }

    fun listenToExtraServicesRealtime() {
        viewModelScope.launch {
            val roomId = _room.value?.id ?: return@launch
            try {
                roomRepository.listenToRoomExtraServices(roomId)
                    .collect { extras ->
                        _extraServices.value = extras
                    }
            } catch (e: Exception) {
                Log.e("RoomDetailsVM", "Error listening to extra services", e)
            }
        }
    }


    fun onDeleteClick() {
        _isDeleteOnClicked.value = true
    }

    fun onCancelDelete() {
        _isDeleteOnClicked.value = false
    }

    fun deleteRoom() {
        viewModelScope.launch {
            try {
                val roomId = room.value?.id
                if (roomId == null) {
                    Log.e("RoomDetailsVM", "Room ID is null, cannot delete.")
                    _errorEvent.emit(SingleEvent("Room ID is not available."))
                    _isDeleteOnClicked.value = false
                    return@launch
                }

                _isDeletingRoom.value = true

                val result = buildingCloudFunctions.archiveRoom(roomId)

                result.onSuccess { message ->
                    Log.d("RoomDetailsVM", "Room $roomId archived successfully: $message")
                    _successEvent.emit(SingleEvent(message))
                    _isDeleteOnClicked.value = false
                }

                result.onFailure { exception ->
                    Log.e("RoomDetailsVM", "Error archiving room $roomId", exception)
                    _errorEvent.emit(SingleEvent(exception.message ?: "Failed to archive room"))
                    _isDeleteOnClicked.value = false
                }

            } catch (e: Exception) {
                Log.e("RoomDetailsVM", "Unexpected error deleting room", e)
                _errorEvent.emit(SingleEvent("An unexpected error occurred: ${e.message}"))
                _isDeleteOnClicked.value = false
            } finally {
                _isDeletingRoom.value = false
            }
        }
    }

}