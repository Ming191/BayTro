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

data class ContractWithRoom(
    val contract: Contract,
    val roomNumber: String
)

class ContractListVM(
    private val contractRepository: ContractRepository,
    authRepository: AuthRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(ContractTab.ACTIVE)
    val selectedTab: StateFlow<ContractTab> = _selectedTab.asStateFlow()

    private val _contracts = MutableStateFlow<List<ContractWithRoom>>(emptyList())
    val contracts: StateFlow<List<ContractWithRoom>> = _contracts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredContracts = MutableStateFlow<List<ContractWithRoom>>(emptyList())
    val filteredContracts: StateFlow<List<ContractWithRoom>> = _filteredContracts.asStateFlow()

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
        loadBuildingsAndContracts()
        viewModelScope.launch {
            _searchQuery.collect { query ->
                applyFilters()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase().trim()
        _filteredContracts.value = if (query.isEmpty()) {
            _contracts.value
        } else {
            _contracts.value.filter { contractWithRoom ->
                contractWithRoom.contract.contractNumber.lowercase().contains(query) ||
                contractWithRoom.roomNumber.lowercase().contains(query)
            }
        }
    }

    private fun loadBuildingsAndContracts() {
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
                _contracts.value = it
                applyFilters()
            }
            return
        }
        _contracts.value = emptyList()
        _filteredContracts.value = emptyList()
        refreshContracts()
    }

    fun refreshContracts() {
        if (landlordId == null) return
        _contracts.value = emptyList()
        _filteredContracts.value = emptyList()

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

                val contractsWithRooms = filteredContracts.map { contract ->
                    val room = roomRepository.getById(contract.roomId)
                    ContractWithRoom(
                        contract = contract,
                        roomNumber = room?.roomNumber ?: "N/A"
                    )
                }

                _contracts.value = contractsWithRooms
                contractCache[_selectedTab.value] = contractsWithRooms
                applyFilters()
            } catch (e: Exception) {
                _error.value = e.message
                _contracts.value = emptyList()
                _filteredContracts.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}