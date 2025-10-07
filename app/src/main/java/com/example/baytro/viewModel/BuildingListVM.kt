package com.example.baytro.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.contract.Status
import com.example.baytro.data.room.RoomRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuildingWithStats(
    val building: Building,
    val occupiedRooms: Int,
    val totalRooms: Int
)

class BuildingListVM(
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val contractRepository: ContractRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings

    private val _buildingsWithStats = MutableStateFlow<List<BuildingWithStats>>(emptyList())

    private val _isLoading = MutableStateFlow(true) // Start with loading true
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _hasLoadedOnce = MutableStateFlow(false)
    val hasLoadedOnce: StateFlow<Boolean> = _hasLoadedOnce

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow(BuildingStatusFilter.ALL)
    val statusFilter: StateFlow<BuildingStatusFilter> = _statusFilter

    // Pagination
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _itemsPerPage = 10

    @OptIn(FlowPreview::class)
    val filteredBuildings: StateFlow<List<BuildingWithStats>> = combine(
        _buildingsWithStats,
        _searchQuery.debounce(500),
        _statusFilter
    ) { list, query, status ->
        val normalizedQuery = query.trim().lowercase()
        list.filter { buildingWithStats ->
            val building = buildingWithStats.building
            val matchesQuery = if (normalizedQuery.isBlank()) {
                true
            } else {
                building.name.lowercase().contains(normalizedQuery) ||
                        building.address.lowercase().contains(normalizedQuery)
            }

            val matchesStatus = when (status) {
                BuildingStatusFilter.ALL -> true
                BuildingStatusFilter.ACTIVE -> building.status.equals("active", ignoreCase = true)
                BuildingStatusFilter.INACTIVE -> building.status.equals("inactive", ignoreCase = true)
            }

            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Paginated buildings for current page
    val paginatedBuildings: StateFlow<List<BuildingWithStats>> = combine(
        filteredBuildings,
        _currentPage
    ) { buildings, page ->
        val startIndex = page * _itemsPerPage
        val endIndex = minOf(startIndex + _itemsPerPage, buildings.size)
        if (startIndex < buildings.size) {
            buildings.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Total pages
    val totalPages: StateFlow<Int> = filteredBuildings.combine(_currentPage) { buildings, _ ->
        if (buildings.isEmpty()) 0 else ((buildings.size - 1) / _itemsPerPage) + 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Has next page
    val hasNextPage: StateFlow<Boolean> = combine(
        _currentPage,
        totalPages
    ) { page, total ->
        page < total - 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // Has previous page
    val hasPreviousPage: StateFlow<Boolean> = _currentPage.combine(totalPages) { page, _ ->
        page > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun loadBuildings() {
        viewModelScope.launch {
            Log.d("BuildingListVM", "loadBuildings() started - Setting isLoading = true")
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                Log.d("BuildingListVM", "Fetching buildings for user: ${currentUser.uid}")
                val userBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                Log.d("BuildingListVM", "Fetched ${userBuildings.size} buildings")

                // Fetch room statistics for each building in parallel
                val buildingsWithStats = userBuildings.map { building ->
                    async {
                        val rooms = roomRepository.getRoomsByBuildingId(building.id)
                        val totalRooms = rooms.size

                        // Get all contracts for this building and count active ones
                        val contracts = contractRepository.getContractsByBuildingId(building.id)
                        val activeContracts = contracts.filter { it.status == Status.ACTIVE }
                        val occupiedRooms = activeContracts.distinctBy { it.roomId }.size

                        Log.d("BuildingListVM", "Building ${building.name}: $occupiedRooms/$totalRooms rooms occupied")

                        BuildingWithStats(
                            building = building,
                            occupiedRooms = occupiedRooms,
                            totalRooms = totalRooms
                        )
                    }
                }.awaitAll() // Wait for all async operations to complete

                Log.d("BuildingListVM", "Setting buildings list with ${userBuildings.size} items")
                _buildings.value = userBuildings
                _buildingsWithStats.value = buildingsWithStats
            } catch (e: Exception) {
                Log.e("BuildingListVM", "Error loading buildings", e)
                _buildings.value = emptyList()
                _buildingsWithStats.value = emptyList()
            } finally {
                Log.d("BuildingListVM", "loadBuildings() finished - Setting isLoading = false, hasLoadedOnce = true")
                _isLoading.value = false
                _hasLoadedOnce.value = true
            }
        }
    }

    // Force reload buildings (for pull-to-refresh or after editing)
    fun refreshBuildings() {
        Log.d("BuildingListVM", "refreshBuildings() called - forcing reload")
        _hasLoadedOnce.value = false
        loadBuildings()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 0 // Reset to first page when search changes
    }

    fun setStatusFilter(filter: BuildingStatusFilter) {
        _statusFilter.value = filter
        _currentPage.value = 0 // Reset to first page when filter changes
    }

    fun nextPage() {
        if (hasNextPage.value) {
            _currentPage.value += 1
        }
    }

    fun previousPage() {
        if (hasPreviousPage.value) {
            _currentPage.value -= 1
        }
    }

    //TODO() Add goToPage with validation
    fun goToPage(page: Int) {
        if (page >= 0 && page < totalPages.value) {
            _currentPage.value = page
        }
    }

    enum class BuildingStatusFilter {
        ALL,
        ACTIVE,
        INACTIVE
    }
}