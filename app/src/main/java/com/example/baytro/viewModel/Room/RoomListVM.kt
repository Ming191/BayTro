package com.example.baytro.viewModel.Room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.room.Floor
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoomListVM(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val contractRepository: ContractRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val buildingId: String = checkNotNull(savedStateHandle["buildingId"])

    private val _building = MutableStateFlow<Building?>(null)
    val building: StateFlow<Building?> = _building

    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms

    private val _buildingTenants = MutableStateFlow<List<String>>(emptyList())
    val buildingTenants: StateFlow<List<String>> = _buildingTenants

    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms: StateFlow<Boolean> = _isLoadingRooms

    private val _roomTenants = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val roomTenants: StateFlow<Map<String, List<String>>> = _roomTenants

    fun fetchBuilding() {
        viewModelScope.launch {
            try {
                _building.value = buildingRepository.getById(buildingId)
            } catch (e: Exception) {
                e.printStackTrace()
                _building.value = null
            }
        }
    }

    fun fetchRooms() {
        viewModelScope.launch {
            try {
                _isLoadingRooms.value = true
                
                val building = _building.value ?: buildingRepository.getById(buildingId)
                _building.value = building
                val rooms = roomRepository.getAll()
                val filteredRooms = rooms.filter { it.buildingId == buildingId }

                val roomTenantsMap = mutableMapOf<String, List<String>>()
                filteredRooms.forEach { room ->
                    val contracts = contractRepository.getContractsByRoomId(room.id)
                    val activeContract = contracts.firstOrNull { it.status == com.example.baytro.data.contract.Status.ACTIVE }
                    if (activeContract != null) {
                        roomTenantsMap[room.id] = activeContract.tenantIds
                    }
                }
                _roomTenants.value = roomTenantsMap

                val roomsGroupByFloor = filteredRooms.groupBy { it.floor }
                val floorsList = roomsGroupByFloor.map { (floorNumber, rooms) ->
                    Floor(number = floorNumber, rooms = rooms)
                }.sortedBy { it.number }

                _floors.value = floorsList
                _rooms.value = filteredRooms
            } catch (e: Exception) {
                e.printStackTrace()
                _floors.value = emptyList()
            } finally {
                _isLoadingRooms.value = false
            }
        }
    }

    fun deleteBuilding(id: String) {
        viewModelScope.launch {
            try {
                buildingRepository.delete(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchBuildingTenants() {
        viewModelScope.launch {
            try {
                val tenants = contractRepository.getTenantsByBuildingId(buildingId)
                _buildingTenants.value = tenants
            } catch (e: Exception) {
                e.printStackTrace()
                _buildingTenants.value = emptyList()
            }
        }
    }
}