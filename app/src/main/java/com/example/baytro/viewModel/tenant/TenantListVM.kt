package com.example.baytro.viewModel.tenant

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.Room
import com.example.baytro.data.room.RoomRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

data class TenantDisplay(
    val tenant: User,
    val room: Room,
    val building: Building
)

class TenantListVM(
    private val authRepository: AuthRepository,
    private val contractRepository: ContractRepository,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _tenantList = MutableStateFlow<List<User>>(emptyList())
    val tenantList: StateFlow<List<User>> = _tenantList

    private val _tenantRoomBuildingMap = MutableStateFlow<Map<String, Pair<Room, Building>>>(emptyMap())
    val tenantRoomBuildingMap: StateFlow<Map<String, Pair<Room, Building>>> = _tenantRoomBuildingMap

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(FlowPreview::class)
    val filteredTenantList: StateFlow<List<TenantDisplay>> =
        combine(
            _tenantList,
            _tenantRoomBuildingMap,
            _searchQuery.debounce(500) // <- delay 0.5s trước khi xử lý
        ) { tenants, roomBuildingMap, query ->
            val normalizedQuery = query.trim().lowercase()
            val combined = tenants.mapNotNull { tenant ->
                val pair = roomBuildingMap[tenant.id]
                if (pair != null) {
                    TenantDisplay(tenant, pair.first, pair.second)
                } else {
                    null
                }
            }

            if (normalizedQuery.isBlank()) combined
            else combined.filter {
                it.tenant.fullName.lowercase().contains(normalizedQuery) ||
                it.room.roomNumber.lowercase().contains(normalizedQuery) ||
                it.building.name.lowercase().contains(normalizedQuery) // Thêm search theo building name
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadTenant() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                Log.d("TenantListVM", "Fetching contracts for user: ${currentUser.uid}")
                val contracts = contractRepository.getContractsByLandlordId(currentUser.uid)
                Log.d("TenantListVM", "Fetched ${contracts.size} contracts for user: ${currentUser.uid}")
                val tenantIds = contracts.flatMap { it.tenantIds }.distinct()
                if (tenantIds.isNotEmpty()) {
                    val tenants = userRepository.getUsersByIds(tenantIds)
                    Log.d("TenantListVM", "Fetched ${tenants.size} tenants for user: ${currentUser.uid}")
                    _tenantList.value = tenants
                } else {
                    _tenantList.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("TenantListVM", "Error loading tenant list", e)
                _tenantList.value = emptyList()
            }
        }
    }

    fun getTenantRoomAndBuilding() {
        viewModelScope.launch {
            try {
                val tenantRoomBuildingMap = mutableMapOf<String, Pair<Room, Building>>()
                Log.d("TenantListVM", "Fetching rooms and buildings for ${_tenantList.value.size} tenants")
                // Dùng async để chạy các request song song
                val results = _tenantList.value.map { tenant ->
                    async {
                        Log.d("TenantListVM", "Fetching contract for tenant: ${tenant.fullName}")
                        val contract = contractRepository.getContractByTenantId(tenant.id)
                        if (contract != null) {
                            Log.d("TenantListVM", "Fetched contract for tenant: ${contract.contractNumber}")

                            val room = roomRepository.getById(contract.roomId)
                            Log.d("TenantListVM", "Fetched room ${room?.roomNumber} for contract: ${contract.contractNumber}")

                            val building = buildingRepository.getById(contract.buildingId)
                            Log.d("TenantListVM", "Fetched building ${building?.name} for contract: ${contract.contractNumber}")

                            if (room != null && building != null) {
                                tenant.id to Pair(room, building)
                            } else {
                                Log.d("TenantListVM", "Room or building not found for contract: ${contract.contractNumber}")
                                null
                            }
                        } else {
                            Log.d("TenantListVM", "No contract found for tenant: ${tenant.fullName}")
                            null
                        }
                    }
                }.awaitAll() // Đợi tất cả coroutine hoàn thành

                // Gộp kết quả vào map
                results.filterNotNull().forEach { (tenantId, pair) ->
                    tenantRoomBuildingMap[tenantId] = pair
                }

                Log.d("TenantListVM", "Built map with ${tenantRoomBuildingMap.size} entries for ${_tenantList.value.size} tenants")
                _tenantRoomBuildingMap.value = tenantRoomBuildingMap

            } catch (e: Exception) {
                Log.e("TenantListVM", "Error loading rooms and buildings", e)
                _tenantRoomBuildingMap.value = emptyMap()
            }
        }
    }

    fun searchingQuery(query: String) {
        _searchQuery.value = query
    }
}