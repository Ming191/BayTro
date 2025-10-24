package com.example.baytro.viewModel.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.RequestCloudFunctions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategorizedRequests(
    val pending: List<FullRequestInfo> = emptyList(),
    val inProgress: List<FullRequestInfo> = emptyList(),
    val done: List<FullRequestInfo> = emptyList()
)

data class RequestListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLandlord: Boolean = false,
    val buildings: List<BuildingSummary> = emptyList(),
    val selectedBuildingId: String? = null,
    val categorizedRequests: CategorizedRequests = CategorizedRequests(),
    val nextCursor: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RequestListVM(
    private val requestCloudFunctions: RequestCloudFunctions,
    private val buildingRepository: BuildingRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _selectedBuildingId = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    private var currentRequests: List<FullRequestInfo> = emptyList()
    private var currentNextCursor: String? = null
    private val currentUserId = auth.currentUser?.uid

    val uiState: StateFlow<RequestListUiState>

    init {
        uiState = combine(
            _selectedBuildingId,
            _refreshTrigger
        ) { buildingId, _ ->
            buildingId
        }.flatMapLatest { buildingId ->
            flow {
                emit(RequestListUiState(isLoading = true, selectedBuildingId = buildingId))

                val isLandlord = if (currentUserId != null) {
                    try {
                        val user = userRepository.getById(currentUserId)
                        user?.role is Role.Landlord
                    } catch (_: Exception) {
                        _errorEvent.emit(SingleEvent("Failed to load user role."))
                        false
                    }
                } else false

                val buildings = if (isLandlord) {
                    try {
                        buildingRepository.getBuildingSummariesByLandlord(currentUserId!!)
                    } catch (_: Exception) {
                        _errorEvent.emit(SingleEvent("Failed to load buildings filter."))
                        emptyList()
                    }
                } else emptyList()

                val result = requestCloudFunctions.getRequestList(
                    buildingIdFilter = buildingId,
                    limit = 10
                )

                result.onSuccess { response ->
                    currentRequests = response.requests
                    currentNextCursor = response.nextCursor

                    val categorized = categorizeRequests(currentRequests)

                    emit(
                        RequestListUiState(
                            isLoading = false,
                            isLandlord = isLandlord,
                            buildings = buildings,
                            selectedBuildingId = buildingId,
                            categorizedRequests = categorized,
                            nextCursor = currentNextCursor
                        )
                    )
                }

                result.onFailure { exception ->
                    _errorEvent.emit(SingleEvent(exception.message ?: "Failed to load requests"))
                    emit(
                        RequestListUiState(
                            isLoading = false,
                            buildings = buildings,
                            selectedBuildingId = buildingId
                        )
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RequestListUiState()
        )
    }

    fun selectBuilding(buildingId: String?) {
        _selectedBuildingId.value = buildingId
    }

    fun refresh() {
        currentNextCursor = null
        currentRequests = emptyList()
        _refreshTrigger.value++
    }

    fun loadNextPage() {
        val cursor = currentNextCursor ?: return
        val buildingId = _selectedBuildingId.value

        viewModelScope.launch {
            val current = uiState.value
            emitUiState(current.copy(isLoadingMore = true))

            val result = requestCloudFunctions.getRequestList(
                buildingIdFilter = buildingId,
                limit = 10,
                startAfter = cursor
            )

            result.onSuccess { response ->
                val merged = (currentRequests + response.requests)
                    .distinctBy { it.request.id }

                currentRequests = merged
                currentNextCursor = response.nextCursor

                emitUiState(
                    current.copy(
                        isLoadingMore = false,
                        categorizedRequests = categorizeRequests(merged),
                        nextCursor = currentNextCursor
                    )
                )
            }

            result.onFailure { e ->
                _errorEvent.emit(SingleEvent(e.message ?: "Failed to load next page"))
                emitUiState(current.copy(isLoadingMore = false))
            }
        }
    }

    private fun categorizeRequests(requests: List<FullRequestInfo>): CategorizedRequests {
        return CategorizedRequests(
            pending = requests.filter { it.request.status == RequestStatus.PENDING },
            inProgress = requests.filter { it.request.status == RequestStatus.IN_PROGRESS },
            done = requests.filter { it.request.status == RequestStatus.DONE }
        )
    }

    private fun emitUiState(state: RequestListUiState) {
        viewModelScope.launch {
            _refreshTrigger.value = _refreshTrigger.value
        }
    }
}
