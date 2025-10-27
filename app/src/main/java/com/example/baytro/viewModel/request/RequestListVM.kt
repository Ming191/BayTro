package com.example.baytro.viewModel.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.Utils
import com.example.baytro.utils.cloudFunctions.RequestCloudFunctions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.Date

data class RequestListUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLandlord: Boolean = false,
    val buildings: List<BuildingSummary> = emptyList(),
    val selectedBuildingId: String? = null,
    val requests: List<FullRequestInfo> = emptyList(),
    val nextCursor: String? = null,
    val error: SingleEvent<String>? = null,
    val fromDate: String? = null,
    val toDate: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class RequestListVM(
    private val requestCloudFunctions: RequestCloudFunctions,
    private val buildingRepository: BuildingRepository,
    private val userRepository: UserRepository,
    auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestListUiState())
    val uiState: StateFlow<RequestListUiState> = _uiState.asStateFlow()

    private val currentUserId = auth.currentUser?.uid
    private val _loadNextMutex = Mutex()

    // flow theo dõi cả building lẫn khoảng ngày 👇
    private val filterFlow = _uiState
        .map { Triple(it.selectedBuildingId, it.fromDate, it.toDate) }
        .distinctUntilChanged()
        .drop(1)

    init {
        loadInitialData()

        viewModelScope.launch {
            filterFlow.collect { (buildingId, fromDate, toDate) ->
                loadRequests(
                    buildingId = buildingId,
                    fromDate = fromDate,
                    toDate = toDate,
                    isRefresh = true
                )
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (currentUserId == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLandlord = false,
                        buildings = emptyList(),
                        requests = emptyList(),
                        nextCursor = null
                    )
                }
                return@launch
            }

            val userResult = runCatching {
                userRepository.getById(currentUserId)
            }

            val user = userResult.getOrNull()
            val isLandlord = user?.role is Role.Landlord

            var buildings = emptyList<BuildingSummary>()
            if (isLandlord) {
                val buildingsResult = runCatching {
                    buildingRepository.getBuildingSummariesByLandlord(currentUserId)
                }
                buildings = buildingsResult.getOrElse { emptyList() }
            }

            _uiState.update { it.copy(isLandlord = isLandlord, buildings = buildings) }

            loadRequests(
                buildingId = _uiState.value.selectedBuildingId,
                fromDate = _uiState.value.fromDate,
                toDate = _uiState.value.toDate,
                isRefresh = false
            )
        }
    }

    private fun loadRequests(
        buildingId: String?,
        fromDate: String?,
        toDate: String?,
        isRefresh: Boolean
    ) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        requests = emptyList(),
                        nextCursor = null,
                        error = null
                    )
                }
            }

            val result = requestCloudFunctions.getRequestList(
                buildingIdFilter = buildingId,
                limit = 10,
                fromDate = fromDate,
                toDate = toDate
            )

            result.onSuccess { response ->
                // --- Bắt đầu lọc theo createdAt ---
                val from = fromDate?.let { Utils.parseDateToDate(it) }
                val to = toDate?.let {
                    Calendar.getInstance().apply {
                        time = Utils.parseDateToDate(it)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }.time
                }

                val filteredRequests = response.requests.filter { fullInfo ->
                    val createdAt = fullInfo.request.createdAt?.let { Date(it.seconds * 1000) }
                    if (createdAt == null) return@filter true

                    val fromOk = from?.let { createdAt >= it } ?: true
                    val toOk = to?.let { createdAt <= it } ?: true
                    fromOk && toOk
                }
                // --- Kết thúc lọc ---

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requests = filteredRequests,
                        nextCursor = response.nextCursor
                    )
                }
            }

            result.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        requests = if (isRefresh) emptyList() else it.requests,
                        nextCursor = if (isRefresh) null else it.nextCursor,
                        error = SingleEvent(exception.message ?: "Failed to load requests")
                    )
                }
            }
        }
    }



    fun loadNextPage() {
        viewModelScope.launch {
            _loadNextMutex.withLock {
                val currentState = _uiState.value
                val cursor = currentState.nextCursor ?: return@withLock
                if (currentState.isLoading || currentState.isLoadingMore) return@withLock

                _uiState.update { it.copy(isLoadingMore = true, error = null) }

                val result = requestCloudFunctions.getRequestList(
                    buildingIdFilter = currentState.selectedBuildingId,
                    limit = 10,
                    startAfter = cursor,
                    fromDate = currentState.fromDate, // ⬅️ giữ filter khi load thêm
                    toDate = currentState.toDate
                )

                result.onSuccess { response ->
                    val mergedRequests = (currentState.requests + response.requests).distinctBy { it.request.id }
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            requests = mergedRequests,
                            nextCursor = response.nextCursor
                        )
                    }
                }
                result.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = SingleEvent(e.message ?: "Failed to load next page")
                        )
                    }
                }
            }
        }
    }

    fun selectBuilding(buildingId: String?) {
        if (buildingId != _uiState.value.selectedBuildingId) {
            _uiState.update { it.copy(selectedBuildingId = buildingId) }
        }
    }

    fun selectDateRange(from: String?, to: String?) {
        _uiState.update { it.copy(fromDate = from, toDate = to) }
    }

    fun refresh() {
        loadInitialData()
    }
}