package com.example.baytro.viewModel.contract

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ContractTab {
    ACTIVE, PENDING, ENDED
}

// Create a data class to hold contract with room number
data class ContractWithRoom(
    val contract: Contract,
    val roomNumber: String
)

class ContractListVM(
    private val contractRepository: ContractRepository,
    private val authRepository: AuthRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(ContractTab.ACTIVE)
    val selectedTab: StateFlow<ContractTab> = _selectedTab.asStateFlow()

    private val _contracts = MutableStateFlow<List<ContractWithRoom>>(emptyList())
    val contracts: StateFlow<List<ContractWithRoom>> = _contracts.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val landlordId: String? = authRepository.getCurrentUser()?.uid

    // Cache for each tab
    private val contractCache = mutableMapOf<ContractTab, List<ContractWithRoom>>()

    // Store building IDs owned by the user
    private var userBuildingIds: List<String> = emptyList()

    private val _ownedBuildings = MutableStateFlow<List<Building>>(emptyList())
    val ownedBuildings: StateFlow<List<Building>> = _ownedBuildings.asStateFlow()

    init {
        fetchUserBuildingsAndContracts()
    }

    private fun fetchUserBuildingsAndContracts() {
        viewModelScope.launch {
            landlordId?.let { uid ->
                val buildings = buildingRepository.getBuildingsByUserId(uid)
                userBuildingIds = buildings.map { it.id }
                _ownedBuildings.value = buildings
                refreshContracts()
            }
        }
    }

    fun selectTab(tab: ContractTab) {
        _selectedTab.value = tab
        contractCache[tab]?.let {
            viewModelScope.launch {
                _loading.value = true
                kotlinx.coroutines.delay(250) // Show loading for 250ms for animation
                _contracts.value = it
                _loading.value = false
            }
            return
        }
        refreshContracts()
    }

    fun refreshContracts() {
        if (landlordId == null) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val statusesToFetch = when (_selectedTab.value) {
                    ContractTab.ACTIVE -> listOf(Status.ACTIVE, Status.OVERDUE)
                    ContractTab.PENDING -> listOf(Status.PENDING)
                    ContractTab.ENDED -> listOf(Status.ENDED)
                }
                val fetchedContracts = contractRepository.getContractsByStatus(landlordId, statusesToFetch)
                val filteredContracts = fetchedContracts.filter { it.buildingId in userBuildingIds }

                // Fetch room numbers for each contract
                val contractsWithRooms = filteredContracts.map { contract ->
                    val room = roomRepository.getById(contract.roomId)
                    ContractWithRoom(
                        contract = contract,
                        roomNumber = room?.roomNumber ?: "N/A"
                    )
                }

                _contracts.value = contractsWithRooms
                contractCache[_selectedTab.value] = contractsWithRooms
            } catch (e: Exception) {
                _error.value = e.message
                _contracts.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}