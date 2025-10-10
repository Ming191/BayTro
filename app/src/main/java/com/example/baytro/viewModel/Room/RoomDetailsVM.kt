package com.example.baytro.viewModel.Room

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.navigation.Screens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoomDetailsVM(
    private val roomRepository: RoomRepository,
    private val contractRepository: ContractRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val _contract = MutableStateFlow<List<Contract>>(emptyList())
    val contract: StateFlow<List<Contract>> = _contract

    private val _tenants = MutableStateFlow<List<User>>(emptyList())
    val tenants: StateFlow<List<User>> = _tenants

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isDeleteOnClicked = MutableStateFlow(false)
    val isDeleteOnClicked = _isDeleteOnClicked

    fun loadRoom() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val room = roomRepository.getById(roomId)
                Log.d("RoomDetailsVM", "RoomInRoomDetailsVM: $room")
                _room.value = room
            } catch (e: Exception) {
                e.printStackTrace()
                _room.value = null
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
                _contract.value = contracts
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
                if (roomId != null) {
                    roomRepository.delete(roomId)
                    Log.d("RoomDetailsVM", "Room $roomId deleted successfully.")
                } else {
                    Log.e("RoomDetailsVM", "Room ID is null, cannot delete.")
                }

                _isDeleteOnClicked.value = false
            } catch (e: Exception) {
                Log.e("RoomDetailsVM", "Error deleting room", e)
                _isDeleteOnClicked.value = false
            }
        }
    }

}