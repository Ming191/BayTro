package com.example.baytro.viewModel.building

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingWithStats
import com.example.baytro.utils.cloudFunctions.BuildingCloudFunctions
import com.example.baytro.utils.SingleEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BuildingListUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val statusFilter: BuildingStatusFilter = BuildingStatusFilter.ALL,
    val buildings: List<BuildingWithStats> = emptyList()
)

enum class BuildingStatusFilter { ALL, ACTIVE, INACTIVE }


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BuildingListVM(
    private val buildingCloudFunctions: BuildingCloudFunctions
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(BuildingStatusFilter.ALL)
    private val _refreshTrigger = MutableStateFlow(0)

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private val _successEvent = MutableSharedFlow<SingleEvent<String>>()
    val successEvent: SharedFlow<SingleEvent<String>> = _successEvent.asSharedFlow()

    private val _isDeletingBuilding = MutableStateFlow(false)
    val isDeletingBuilding: StateFlow<Boolean> = _isDeletingBuilding.asStateFlow()

    private val _buildingsData: StateFlow<Pair<Boolean, List<BuildingWithStats>>> = combine(
        _searchQuery.debounce(300),
        _statusFilter,
        _refreshTrigger
    ) { query, status, _ ->
        Pair(query, status)
    }.flatMapLatest { (query, status) ->
        flow {
            emit(Pair(true, emptyList<BuildingWithStats>()))

            val result = buildingCloudFunctions.getBuildingListWithStats(
                searchQuery = query,
                statusFilter = status.name
            )

            result.onSuccess { response ->
                emit(Pair(false, response.buildings))
            }
            result.onFailure { exception ->
                _errorEvent.emit(SingleEvent(exception.message ?: "Failed to load buildings"))
                emit(Pair(false, emptyList()))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(true, emptyList())
    )

    val uiState: StateFlow<BuildingListUiState> = combine(
        _searchQuery,
        _statusFilter,
        _buildingsData
    ) { query, status, (isLoading, buildings) ->
        BuildingListUiState(
            isLoading = isLoading,
            searchQuery = query,
            statusFilter = status,
            buildings = buildings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BuildingListUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: BuildingStatusFilter) {
        _statusFilter.value = filter
    }

    fun refresh() {
        _refreshTrigger.value++
    }

    fun archiveBuilding(buildingId: String) {
        viewModelScope.launch {
            _isDeletingBuilding.value = true
            val result = buildingCloudFunctions.archiveBuilding(buildingId)
            result.onSuccess {
                _successEvent.emit(SingleEvent("Building deleted successfully"))
                refresh()
            }
            result.onFailure { exception ->
                _errorEvent.emit(SingleEvent(exception.message ?: "Failed to delete building"))
            }
            _isDeletingBuilding.value = false
        }
    }
}