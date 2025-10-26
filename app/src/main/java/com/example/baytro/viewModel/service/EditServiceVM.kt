package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditServiceVM(
    private val buildingRepo: BuildingRepository,
    private val roomRepo: RoomRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serviceId: String? = savedStateHandle["serviceId"]
    private val buildingId: String? = savedStateHandle["buildingId"]
    private val roomId: String? = savedStateHandle["roomId"]

    private val _uiState = MutableStateFlow<UiState<Service>>(UiState.Loading)
    val uiState: StateFlow<UiState<Service>> = _uiState

    private val _formState = MutableStateFlow(EditServiceFormState())
    val formState: StateFlow<EditServiceFormState> = _formState

    private var originalService: Service? = null

    init {
        viewModelScope.launch { loadService() }
        Log.d("EditServiceVM", "roomId: $roomId, buildingId: $buildingId, serviceId: $serviceId")
    }

    // ==========================
    //       LOAD SERVICE
    // ==========================
    private suspend fun loadService() {
        _uiState.value = UiState.Loading
        try {
            when {
                roomId != null && serviceId != null -> loadFromRoom(roomId, serviceId)
                buildingId != null && serviceId != null -> loadFromBuilding(buildingId, serviceId)
                else -> throw Exception("No building or room specified")
            }
            _uiState.value = UiState.Idle
        } catch (e: Exception) {
            Log.e("EditServiceVM", "Error loading service", e)
            _uiState.value = UiState.Error(e.message ?: "Error loading service")
        }
    }

    private suspend fun loadFromRoom(roomId: String, serviceId: String) {
        val room = roomRepo.getById(roomId) ?: throw Exception("Room not found")
        val building = buildingRepo.getById(room.buildingId) ?: throw Exception("Building not found")

        // Lấy service từ subcollection: rooms/{roomId}/services/{serviceId}
        val service = roomRepo.getExtraServiceById(roomId, serviceId)
            ?: throw Exception("Service not found in room subcollection")

        originalService = service

        _formState.value = EditServiceFormState(
            name = service.name,
            price = service.price.toString(),
            metrics = service.metric,
            isDefault = service.isDefault,
            availableRooms = listOf(room),
            selectedRooms = setOf(room.id),
            selectedBuilding = building,
            availableBuildings = listOf(building)
        )
    }


    private suspend fun loadFromBuilding(buildingId: String, serviceId: String?) {
        val building = buildingRepo.getById(buildingId) ?: throw Exception("Building not found")
        val service = serviceId?.let { buildingRepo.getServiceById(buildingId, it) }
            ?: throw Exception("Service not found")

        originalService = service

        val rooms = roomRepo.getRoomsByBuildingId(building.id)
        val selectedRooms = rooms.filter { room ->
            room.extraService.any { it.id == serviceId }
        }.map { it.id }.toSet()

        _formState.value = EditServiceFormState(
            name = service.name,
            price = service.price.toString(),
            metrics = service.metric,
            isDefault = service.isDefault,
            selectedBuilding = building,
            availableBuildings = listOf(building),
            availableRooms = rooms,
            selectedRooms = selectedRooms
        )
    }

    //       FORM EVENTS
    fun onNameChange(value: String) =
        _formState.update { it.copy(name = value) }

    fun onPriceChange(value: String) =
        _formState.update { it.copy(price = value) }

    fun onUnitChange(value: Metric) =
        _formState.update { it.copy(metrics = value) }

    fun onToggleRoom(roomId: String) {
        val current = _formState.value.selectedRooms.toMutableSet()
        if (!current.add(roomId)) current.remove(roomId)
        _formState.update { it.copy(selectedRooms = current) }
    }

    fun onToggleSelectAll() {
        val allRooms = _formState.value.availableRooms.map { it.id }.toSet()
        _formState.update {
            if (it.selectedRooms.size == allRooms.size)
                it.copy(selectedRooms = emptySet())
            else
                it.copy(selectedRooms = allRooms)
        }
    }

    fun onSearchTextChange(value: String) =
        _formState.update { it.copy(searchText = value) }


    fun onConfirm() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val state = _formState.value
            val building = state.selectedBuilding ?: return@launch
            val oldService = originalService ?: return@launch

            val updatedService = oldService.copy(
                name = state.name,
                price = state.price.toIntOrNull() ?: 0,
                metric = state.metrics
            )

            try {
                val allRooms = state.availableRooms
                val selectedRooms = state.selectedRooms

                // CASE 1: Tất cả hoặc không chọn phòng nào → service thuộc BUILDING
                if (selectedRooms.isEmpty() || selectedRooms.size == allRooms.size) {
                    //  Cập nhật service ở building
                    buildingRepo.updateServiceInBuilding(building.id, updatedService)

                    //  Xóa service khỏi tất cả room (nếu có)
                    allRooms.forEach { room ->
                        roomRepo.removeExtraServiceFromRoom(room.id, oldService.id)
                    }

                    Log.d("EditServiceVM", "Service updated at building level")
                }
                //  CASE 2: Chỉ chọn 1 số phòng → service thuộc ROOM
                else {
                    //  Xóa service khỏi building
                    buildingRepo.deleteServiceFromBuilding(building.id, oldService.id)

                    //  Với mỗi phòng:
                    allRooms.forEach { room ->
                        val hasService = roomRepo.hasExtraService(room.id, oldService.id)
                        val isSelected = selectedRooms.contains(room.id)

                        when {
                            isSelected && !hasService -> {
                                // Thêm mới
                                roomRepo.addExtraServiceToRoom(room.id, updatedService)
                            }

                            isSelected && hasService -> {
                                // Cập nhật
                                roomRepo.updateExtraServiceInRoom(room.id, updatedService)
                            }

                            !isSelected && hasService -> {
                                // Gỡ bỏ
                                roomRepo.removeExtraServiceFromRoom(room.id, oldService.id)
                            }
                        }
                    }

                    Log.d("EditServiceVM", "Service moved to selected rooms")
                }

                _uiState.value = UiState.Success(updatedService)
            } catch (e: Exception) {
                Log.e("EditServiceVM", "Error updating service", e)
                _uiState.value = UiState.Error(e.message ?: "Error updating service")
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }

    // ==========================
    //       HELPERS
    // ==========================
    private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
        value = block(value)
    }
}
