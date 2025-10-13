package com.example.baytro.viewModel.contract

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.RoomRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    companion object {
        private const val TAG = "ContractListVM"
    }

    // User info
    private val landlordId: String? = authRepository.getCurrentUser()?.uid

    // Tab selection
    private val _selectedTab = MutableStateFlow(ContractTab.ACTIVE)
    val selectedTab: StateFlow<ContractTab> = _selectedTab.asStateFlow()

    // Data
    private val _contracts = MutableStateFlow<List<ContractWithRoom>>(emptyList())
    val contracts: StateFlow<List<ContractWithRoom>> = _contracts.asStateFlow()

    private val _ownedBuildings = MutableStateFlow<List<Building>>(emptyList())
    val ownedBuildings: StateFlow<List<Building>> = _ownedBuildings.asStateFlow()

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    val selectedBuildingId: StateFlow<String?> = _selectedBuildingId.asStateFlow()

    private val _filteredContracts = MutableStateFlow<List<ContractWithRoom>>(emptyList())
    val filteredContracts: StateFlow<List<ContractWithRoom>> = _filteredContracts.asStateFlow()

    // UI state
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Cache and internal state
    private val contractCache = mutableMapOf<ContractTab, List<ContractWithRoom>>()
    private var userBuildingIds: List<String> = emptyList()
    private var isInitialLoadComplete = false

    // Pagination state
    private val _itemsPerPage = 10
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Derived pagination streams
    val totalPages: StateFlow<Int> = filteredContracts
        .map { list -> if (list.isEmpty()) 0 else ((list.size - 1) / _itemsPerPage) + 1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val paginatedContracts: StateFlow<List<ContractWithRoom>> = combine(filteredContracts, _currentPage) { list, page ->
        val startIndex = page * _itemsPerPage
        if (startIndex >= list.size) emptyList() else list.subList(startIndex, minOf(startIndex + _itemsPerPage, list.size))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hasNextPage: StateFlow<Boolean> = combine(_currentPage, totalPages) { page, total ->
        page < total - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val hasPreviousPage: StateFlow<Boolean> = _currentPage
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        initializeViewModel()
    }

    // ==================== Initialization ====================

    private fun initializeViewModel() {
        loadBuildingsAndContracts()

        viewModelScope.launch {
            _searchQuery.collect {
                applyFilters()
            }
        }
    }

    private fun loadBuildingsAndContracts() {
        viewModelScope.launch {
            landlordId?.let { uid ->
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "========== START: Loading buildings ==========")

                try {
                    val buildings = async {
                        buildingRepository.getBuildingsByUserId(uid)
                    }.await()

                    val buildingsTime = System.currentTimeMillis()
                    Log.d(TAG, "Buildings loaded: ${buildings.size} buildings in ${buildingsTime - startTime}ms")

                    userBuildingIds = buildings.map { it.id }
                    _ownedBuildings.value = buildings

                    Log.d(TAG, "========== END: Loading buildings (Total: ${buildingsTime - startTime}ms) ==========")

                    isInitialLoadComplete = true
                    refreshContracts()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading buildings: ${e.message}", e)
                    _error.value = e.message
                    _loading.value = false
                }
            }
        }
    }

    // ==================== Public Actions ====================

    fun selectTab(tab: ContractTab) {
        _selectedTab.value = tab

        contractCache[tab]?.let {
            Log.d(TAG, "Using cached contracts for tab: $tab (${it.size} contracts)")
            _contracts.value = it
            applyFilters()
            return
        }

        if (!isInitialLoadComplete) {
            Log.d(TAG, "Tab changed to $tab but initial load not complete yet, skipping fetch")
            return
        }

        Log.d(TAG, "No cache for tab: $tab, fetching contracts...")
        // Set loading to true IMMEDIATELY to prevent empty state flicker
        _loading.value = true
        refreshContracts()
        _currentPage.value = 0
    }

    fun refreshContracts() {
        if (landlordId == null) {
            Log.w(TAG, "Cannot refresh contracts: landlordId is null")
            return
        }

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "========== START: Refreshing contracts (Tab: ${_selectedTab.value}) ==========")

                val statusesToFetch = getStatusesForTab(_selectedTab.value)
                Log.d(TAG, "Fetching contracts with statuses: $statusesToFetch")

                // Fetch contracts
                val fetchStartTime = System.currentTimeMillis()
                val fetchedContracts = fetchContracts(landlordId, statusesToFetch)
                val fetchEndTime = System.currentTimeMillis()
                Log.d(TAG, "Contracts fetched: ${fetchedContracts.size} contracts in ${fetchEndTime - fetchStartTime}ms")

                // Filter by building ownership
                val filterStartTime = System.currentTimeMillis()
                val filteredContracts = filterByBuildingOwnership(fetchedContracts)
                val filterEndTime = System.currentTimeMillis()
                Log.d(TAG, "Contracts filtered: ${filteredContracts.size} contracts (removed ${fetchedContracts.size - filteredContracts.size}) in ${filterEndTime - filterStartTime}ms")

                // Map contracts to rooms in parallel
                val mappingStartTime = System.currentTimeMillis()
                val contractsWithRooms = mapContractsToRooms(filteredContracts)
                val mappingEndTime = System.currentTimeMillis()
                Log.d(TAG, "Room mapping completed: ${contractsWithRooms.size} contracts mapped in ${mappingEndTime - mappingStartTime}ms (avg: ${(mappingEndTime - mappingStartTime) / contractsWithRooms.size.coerceAtLeast(1)}ms per contract)")

                _contracts.value = contractsWithRooms
                contractCache[_selectedTab.value] = contractsWithRooms

                val filterApplyStartTime = System.currentTimeMillis()
                applyFilters()
                val filterApplyEndTime = System.currentTimeMillis()
                Log.d(TAG, "Filters applied in ${filterApplyEndTime - filterApplyStartTime}ms")

                logPerformanceSummary(
                    fetchTime = fetchEndTime - fetchStartTime,
                    filterTime = filterEndTime - filterStartTime,
                    mappingTime = mappingEndTime - mappingStartTime,
                    filterApplyTime = filterApplyEndTime - filterApplyStartTime,
                    totalTime = System.currentTimeMillis() - startTime
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing contracts: ${e.message}", e)
                _error.value = e.message
                _contracts.value = emptyList()
                _filteredContracts.value = emptyList()
            } finally {
                _loading.value = false
                delay(100)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBuildingId(buildingId: String?) {
        _selectedBuildingId.value = buildingId
        applyFilters()
        _currentPage.value = 0
    }

    // ==================== Private Helper Methods ====================

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase().trim()
        val buildingId = _selectedBuildingId.value

        _filteredContracts.value = _contracts.value.filter { contractWithRoom ->
            matchesSearchQuery(contractWithRoom, query) && matchesBuildingFilter(contractWithRoom, buildingId)
        }
        _currentPage.value = 0
    }

    fun nextPage() { if (hasNextPage.value) _currentPage.value += 1 }
    fun previousPage() { if (hasPreviousPage.value) _currentPage.value -= 1 }
    fun goToPage(page: Int) { if (page >= 0 && page < totalPages.value) _currentPage.value = page }

    private fun matchesSearchQuery(contractWithRoom: ContractWithRoom, query: String): Boolean {
        if (query.isEmpty()) return true

        return contractWithRoom.contract.contractNumber.lowercase().contains(query) ||
                contractWithRoom.roomNumber.lowercase().contains(query)
    }

    private fun matchesBuildingFilter(contractWithRoom: ContractWithRoom, buildingId: String?): Boolean {
        if (buildingId.isNullOrEmpty()) return true

        return contractWithRoom.contract.buildingId == buildingId
    }

    private fun getStatusesForTab(tab: ContractTab): List<Status> {
        return when (tab) {
            ContractTab.ACTIVE -> listOf(Status.ACTIVE, Status.OVERDUE)
            ContractTab.PENDING -> listOf(Status.PENDING)
            ContractTab.ENDED -> listOf(Status.ENDED)
        }
    }

    private suspend fun fetchContracts(landlordId: String, statuses: List<Status>): List<Contract> {
        return coroutineScope {
            async {
                contractRepository.getContractsByStatus(landlordId, statuses)
            }.await()
        }
    }

    private fun filterByBuildingOwnership(contracts: List<Contract>): List<Contract> {
        return contracts.filter { it.buildingId in userBuildingIds }
    }

    private suspend fun mapContractsToRooms(contracts: List<Contract>): List<ContractWithRoom> {
        Log.d(TAG, "Starting parallel room mapping for ${contracts.size} contracts...")

        return coroutineScope {
            contracts.map { contract ->
                async {
                    val roomStartTime = System.currentTimeMillis()
                    val room = roomRepository.getById(contract.roomId)
                    val roomEndTime = System.currentTimeMillis()
                    Log.d(TAG, "Room fetched for contract ${contract.id}: roomId=${contract.roomId}, roomNumber=${room?.roomNumber} (${roomEndTime - roomStartTime}ms)")

                    ContractWithRoom(
                        contract = contract,
                        roomNumber = room?.roomNumber ?: "N/A"
                    )
                }
            }.awaitAll()
        }
    }

    private fun logPerformanceSummary(
        fetchTime: Long,
        filterTime: Long,
        mappingTime: Long,
        filterApplyTime: Long,
        totalTime: Long
    ) {
        Log.d(TAG, "========== END: Refreshing contracts ==========")
        Log.d(TAG, "SUMMARY:")
        Log.d(TAG, "  - Fetch: ${fetchTime}ms")
        Log.d(TAG, "  - Filter: ${filterTime}ms")
        Log.d(TAG, "  - Room Mapping: ${mappingTime}ms")
        Log.d(TAG, "  - Apply Filters: ${filterApplyTime}ms")
        Log.d(TAG, "  - TOTAL: ${totalTime}ms")
        Log.d(TAG, "========================================================")
    }
}