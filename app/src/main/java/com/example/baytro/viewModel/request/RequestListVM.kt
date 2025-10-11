package com.example.baytro.viewModel.request

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.request.FullRequestInfo
import com.example.baytro.data.request.Request
import com.example.baytro.data.request.RequestRepository
import com.example.baytro.data.request.RequestStatus
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

data class FilteredRequestData(
    val pending: List<FullRequestInfo> = emptyList(),
    val inProgress: List<FullRequestInfo> = emptyList(),
    val done: List<FullRequestInfo> = emptyList()
)

class RequestListVM(
    private val requestRepository: RequestRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val contractRepository: ContractRepository,
    private val userRepository: UserRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val _requestListUiState = MutableStateFlow<UiState<FilteredRequestData>>(UiState.Loading)
    val requestListUiState: StateFlow<UiState<FilteredRequestData>> = _requestListUiState

    private val _formState = MutableStateFlow(RequestListFormState())
    val formState: StateFlow<RequestListFormState> = _formState

    private var requestListenerJob: Job? = null

    private val currentUserId: String? = authRepository.getCurrentUser()?.uid

    init {
        val initStartTime = System.currentTimeMillis()
        Log.d(TAG, "init: initializing RequestListVM - START at $initStartTime")

        if (currentUserId == null) {
            _requestListUiState.value = UiState.Error("No authenticated user")
            Log.e(TAG, "init: No authenticated user")
        } else {
            loadUserRoleAndData()
            Log.d(TAG, "init: COMPLETE in ${System.currentTimeMillis() - initStartTime}ms")
        }
    }

    private fun loadUserRoleAndData() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "loadUserRoleAndData: START")

            try {
                val userFetchStart = System.currentTimeMillis()
                val user = userRepository.getById(currentUserId!!)
                Log.d(TAG, "loadUserRoleAndData: Fetched user in ${System.currentTimeMillis() - userFetchStart}ms")

                if (user == null) {
                    _requestListUiState.value = UiState.Error("User not found")
                    return@launch
                }

                val isLandlord = user.role is Role.Landlord
                _formState.value = _formState.value.copy(isLandlord = isLandlord)
                Log.d(TAG, "loadUserRoleAndData: User is ${if (isLandlord) "LANDLORD" else "TENANT"}")

                if (isLandlord) {
                    val buildingsFetchStart = System.currentTimeMillis()
                    val buildings = buildingRepository.getBuildingsByUserId(currentUserId)
                    Log.d(TAG, "loadUserRoleAndData: Fetched ${buildings.size} buildings in ${System.currentTimeMillis() - buildingsFetchStart}ms")

                    _formState.value = _formState.value.copy(availableBuildings = buildings)

                    if (buildings.isNotEmpty()) {
                        Log.d(TAG, "loadUserRoleAndData: Starting with first building: ${buildings[0].name}")
                        loadRequestsForBuilding(buildings[0])
                    } else {
                        Log.d(TAG, "loadUserRoleAndData: No buildings found")
                        _requestListUiState.value = UiState.Success(FilteredRequestData())
                    }
                } else {
                    val contractFetchStart = System.currentTimeMillis()
                    val contract = contractRepository.getActiveContract(currentUserId)
                    Log.d(TAG, "loadUserRoleAndData: Fetched contract in ${System.currentTimeMillis() - contractFetchStart}ms")

                    if (contract != null) {
                        Log.d(TAG, "loadUserRoleAndData: Found contract, listening to roomId=${contract.roomId}")
                        listenToRequestsByRoomIds(listOf(contract.roomId))
                    } else {
                        Log.d(TAG, "loadUserRoleAndData: No active contract found")
                        _requestListUiState.value = UiState.Success(FilteredRequestData())
                    }
                }

                val totalTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "loadUserRoleAndData: COMPLETE - Total time: ${totalTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "loadUserRoleAndData error", e)
                _requestListUiState.value = UiState.Error(e.message ?: "Error loading data")
            }
        }
    }

    private fun loadRequestsForBuilding(building: Building) {
        _formState.value = _formState.value.copy(selectedBuilding = building)

        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val rooms = roomRepository.getRoomsByBuildingId(building.id)
                Log.d(TAG, "loadRequestsForBuilding: Fetched ${rooms.size} rooms in ${System.currentTimeMillis() - startTime}ms")

                val roomIds = rooms.map { it.id }

                if (roomIds.isNotEmpty()) {
                    listenToRequestsByRoomIds(roomIds)
                } else {
                    _requestListUiState.value = UiState.Success(FilteredRequestData())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadRequestsForBuilding: ERROR", e)
                _requestListUiState.value = UiState.Error(e.message ?: "Error loading requests")
            }
        }
    }

    fun onBuildingChange(building: Building) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "onBuildingChange: START - Changing to building: ${building.name} (${building.id})")

        val current = _formState.value.selectedBuilding
        if (current?.id == building.id) {
            Log.d(TAG, "onBuildingChange: Same building selected, skipping")
            return
        }

        _requestListUiState.value = UiState.Loading
        _formState.value = _formState.value.copy(selectedBuilding = building)
        Log.d(TAG, "onBuildingChange: Set to Loading state")

        viewModelScope.launch {
            try {
                val roomsFetchStart = System.currentTimeMillis()
                val rooms = roomRepository.getRoomsByBuildingId(building.id)
                Log.d(TAG, "onBuildingChange: Fetched ${rooms.size} rooms in ${System.currentTimeMillis() - roomsFetchStart}ms")

                val roomIds = rooms.map { it.id }

                if (roomIds.isNotEmpty()) {
                    Log.d(TAG, "onBuildingChange: Starting to listen to ${roomIds.size} rooms")
                    listenToRequestsByRoomIds(roomIds)
                } else {
                    Log.d(TAG, "onBuildingChange: No rooms found, setting empty state")
                    _requestListUiState.value = UiState.Success(FilteredRequestData())
                }

                val totalTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "onBuildingChange: COMPLETE in ${totalTime}ms")
            } catch (e: Exception) {
                Log.e(TAG, "onBuildingChange: ERROR", e)
                _requestListUiState.value = UiState.Error(e.message ?: "Error fetching rooms")
            }
        }
    }

    private fun listenToRequestsByRoomIds(roomIds: List<String>) {
        requestListenerJob?.cancel()
        requestListenerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "listenToRequestsByRoomIds: START - roomIds count: ${roomIds.size}")

            requestRepository.getRequestsByRoomIdsFlow(roomIds)
                .catch { e ->
                    if (e is CancellationException) throw e
                    _requestListUiState.value = UiState.Error(e.message ?: "Error listening to requests")
                }
                .collect { requests ->
                    val collectTime = System.currentTimeMillis()
                    Log.d(TAG, "listenToRequestsByRoomIds: Received ${requests.size} requests in ${collectTime - startTime}ms")

                    val enrichedRequests = enrichRequestsWithDetails(requests)

                    val enrichedTime = System.currentTimeMillis()
                    Log.d(TAG, "listenToRequestsByRoomIds: Enriched to ${enrichedRequests.size} requests in ${enrichedTime - collectTime}ms")

                    updateUiWithFilteredRequests(enrichedRequests)

                    val totalTime = System.currentTimeMillis()
                    Log.d(TAG, "listenToRequestsByRoomIds: COMPLETE - Total time: ${totalTime - startTime}ms")
                }
        }
    }

    private suspend fun enrichRequestsWithDetails(requests: List<Request>): List<FullRequestInfo> {
        if (requests.isEmpty()) return emptyList()

        val startTime = System.currentTimeMillis()
        Log.d(TAG, "enrichRequestsWithDetails: START with ${requests.size} requests")

        try {
            val tenantIds = requests.map { it.tenantId }.distinct().filter { it.isNotBlank() }
            val roomIds = requests.map { it.roomId }.distinct().filter { it.isNotBlank() }
            val landlordIds = requests.map { it.landlordId }.distinct().filter { it.isNotBlank() }

            Log.d(TAG, "enrichRequestsWithDetails: Need to fetch ${tenantIds.size} tenants, ${roomIds.size} rooms, ${landlordIds.size} landlords")

            val parallelStartTime = System.currentTimeMillis()

            val rooms = if (roomIds.isEmpty()) {
                Log.w(TAG, "enrichRequestsWithDetails: No valid room IDs to fetch")
                emptyList()
            } else {
                roomRepository.getRoomsByIds(roomIds)
            }

            val buildingIds = rooms.map { it.buildingId }.distinct().filter { it.isNotBlank() }
            Log.d(TAG, "enrichRequestsWithDetails: Fetched ${rooms.size} rooms, need ${buildingIds.size} buildings")

            val tenantsDeferred = viewModelScope.async {
                if (tenantIds.isEmpty()) emptyList()
                else userRepository.getUsersByIds(tenantIds)
            }
            val landlordsDeferred = viewModelScope.async {
                if (landlordIds.isEmpty()) emptyList()
                else userRepository.getUsersByIds(landlordIds)
            }
            val buildingsDeferred = viewModelScope.async {
                if (buildingIds.isEmpty()) emptyList()
                else buildingRepository.getBuildingsByIds(buildingIds)
            }

            val tenants = tenantsDeferred.await()
            val landlords = landlordsDeferred.await()
            val buildings = buildingsDeferred.await()

            Log.d(TAG, "enrichRequestsWithDetails: Parallel fetch completed in ${System.currentTimeMillis() - parallelStartTime}ms")

            val tenantsMap = tenants.associateBy { it.id }
            val roomsMap = rooms.associateBy { it.id }
            val buildingsMap = buildings.associateBy { it.id }
            val landlordsMap = landlords.associateBy { it.id }

            val result = requests.mapNotNull { request ->
                val tenant = tenantsMap[request.tenantId]
                val room = roomsMap[request.roomId]
                val building = room?.let { buildingsMap[it.buildingId] }
                val landlord = landlordsMap[request.landlordId]

                if (tenant == null || room == null || building == null || landlord == null) {
                    return@mapNotNull null
                }

                FullRequestInfo(
                    request = request,
                    tenantName = tenant.fullName,
                    tenantPhoneNumber = tenant.phoneNumber,
                    roomName = room.roomNumber,
                    buildingName = building.name,
                    landlordName = landlord.fullName,
                    landlordPhoneNumber = landlord.phoneNumber
                )
            }

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "enrichRequestsWithDetails: COMPLETE - Enriched ${result.size} requests in ${totalTime}ms")

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error enriching requests", e)
            return emptyList()
        }
    }

    private fun updateUiWithFilteredRequests(requests: List<FullRequestInfo>) {
        val pending = requests
            .filter { it.request.status == RequestStatus.PENDING }
            .sortedByDescending { it.request.createdAt }
        val inProgress = requests
            .filter { it.request.status == RequestStatus.IN_PROGRESS }
            .sortedByDescending { it.request.createdAt }
        val done = requests
            .filter { it.request.status == RequestStatus.DONE }
            .sortedByDescending { it.request.createdAt }

        val finalData = FilteredRequestData(
            pending = pending,
            inProgress = inProgress,
            done = done
        )
        _requestListUiState.value = UiState.Success(finalData)
    }

    fun refreshRequests() {
        requestListenerJob?.cancel()
        loadUserRoleAndData()
    }

    fun completeRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val updateFields = mapOf(
                    "status" to RequestStatus.DONE,
                    "completionDate" to currentDateTime
                )
                requestRepository.updateFields(requestId, updateFields)
            } catch (e: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        requestListenerJob?.cancel()
    }

    companion object {
        private const val TAG = "RequestListVM"
    }
}
