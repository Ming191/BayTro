package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BuildingListVM(
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings

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
    val itemsPerPage: Int = _itemsPerPage

    val filteredBuildings: StateFlow<List<Building>> = combine(
        _buildings,
        _searchQuery.debounce(500),
        _statusFilter
    ) { list, query, status ->
        val normalizedQuery = query.trim().lowercase()
        list.filter { building ->
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
    val paginatedBuildings: StateFlow<List<Building>> = combine(
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
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val userBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                delay(300)
                _buildings.value = userBuildings
            } catch (e: Exception) {
                // Handle error - could emit error state
                _buildings.value = emptyList()
            } finally {
                _isLoading.value = false
                _hasLoadedOnce.value = true
            }
        }
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