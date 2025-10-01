package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _statusFilter = MutableStateFlow(BuildingStatusFilter.ALL)
    val statusFilter: StateFlow<BuildingStatusFilter> = _statusFilter

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

    fun loadBuildings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val userBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                _buildings.value = userBuildings
            } catch (e: Exception) {
                // Handle error - could emit error state
                _buildings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: BuildingStatusFilter) {
        _statusFilter.value = filter
    }
    enum class BuildingStatusFilter {
        ALL,
        ACTIVE,
        INACTIVE
    }
}