package com.example.baytro.viewModel.contract

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.BuildingSummary
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.ContractCloudFunctions
import com.example.baytro.utils.cloudFunctions.ContractWithRoom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ContractTab { ACTIVE, PENDING, ENDED }

@Stable
data class ContractListUiState(
    val isLoading: Boolean = true,
    val buildings: List<BuildingSummary> = emptyList(),
    val selectedBuildingId: String? = null,
    val searchQuery: String = "",
    val selectedTab: ContractTab = ContractTab.ACTIVE,
    val contracts: List<ContractWithRoom> = emptyList()
)


@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ContractListVM(
    private val contractCloudFunctions: ContractCloudFunctions,
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ContractTab.ACTIVE)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    private val _buildings = MutableStateFlow<List<BuildingSummary>>(emptyList())
    private val _refreshTrigger = MutableStateFlow(0)

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    val uiState: StateFlow<ContractListUiState>

    init {
        loadBuildings()
        val debouncedSearchQuery = _searchQuery.debounce(300)

        uiState = combine(
            _selectedTab,
            _searchQuery,
            debouncedSearchQuery,
            _selectedBuildingId,
            _buildings,
            _refreshTrigger
        ) { flows ->
            val tab = flows[0] as ContractTab
            val immediateQuery = flows[1] as String
            val debouncedQuery = flows[2] as String
            val buildingId = flows[3] as String?
            val buildings = flows[4] as List<BuildingSummary>
            SearchFilters(tab, immediateQuery, debouncedQuery, buildingId, buildings)
        }.flatMapLatest { filters ->
            flow {
                emit(ContractListUiState(
                    isLoading = true,
                    buildings = filters.buildings,
                    selectedBuildingId = filters.selectedBuildingId,
                    searchQuery = filters.immediateSearchQuery,
                    selectedTab = filters.selectedTab,
                    contracts = emptyList()
                ))

                val result = contractCloudFunctions.getContractList(
                    statusFilter = filters.selectedTab.name,
                    buildingIdFilter = filters.selectedBuildingId,
                    searchQuery = filters.debouncedSearchQuery
                )

                result.onSuccess { response ->
                    emit(ContractListUiState(
                        isLoading = false,
                        buildings = filters.buildings,
                        selectedBuildingId = filters.selectedBuildingId,
                        searchQuery = filters.immediateSearchQuery,
                        selectedTab = filters.selectedTab,
                        contracts = response.contracts
                    ))
                }
                result.onFailure { exception ->
                    _errorEvent.emit(SingleEvent(exception.message ?: "Failed to load contracts"))
                    emit(ContractListUiState(
                        isLoading = false,
                        buildings = filters.buildings,
                        selectedBuildingId = filters.selectedBuildingId,
                        searchQuery = filters.immediateSearchQuery,
                        selectedTab = filters.selectedTab,
                        contracts = emptyList()
                    ))
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContractListUiState()
        )
    }

    private data class SearchFilters(
        val selectedTab: ContractTab,
        val immediateSearchQuery: String,
        val debouncedSearchQuery: String,
        val selectedBuildingId: String?,
        val buildings: List<BuildingSummary>
    )

    private fun loadBuildings() {
        viewModelScope.launch {
            try {
                val landlordId = authRepository.getCurrentUser()?.uid ?: return@launch
                val buildings = buildingRepository.getBuildingSummariesByLandlord(landlordId)
                _buildings.value = buildings
            } catch (e: Exception) {
                _errorEvent.emit(SingleEvent("Failed to load buildings filter: ${e.message}"))
            }
        }
    }

    fun selectTab(tab: ContractTab) { _selectedTab.value = tab }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedBuildingId(buildingId: String?) { _selectedBuildingId.value = buildingId }

    fun refresh() {
        _refreshTrigger.value += 1
        loadBuildings()
    }
}