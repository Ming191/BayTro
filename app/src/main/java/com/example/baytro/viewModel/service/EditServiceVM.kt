package com.example.baytro.viewModel.service

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
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

    private val _uiState = MutableStateFlow<UiState<Service>>(UiState.Loading)
    val uiState: StateFlow<UiState<Service>> = _uiState

    private val _formState = MutableStateFlow(AddServiceFormState())
    val formState: StateFlow<AddServiceFormState> = _formState

    private var originalService: Service? = null

    init {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                // Load building + service
                val building = buildingRepo.getById(buildingId ?: return@launch)
                val service = building?.services?.find { it.id == serviceId }
                    ?: throw Exception("Service not found")

                originalService = service
                _formState.value = AddServiceFormState(
                    name = service.name,
                    price = service.price,
                    metrics = service.metric,
                    selectedBuilding = building,
                    availableBuildings = listOf(building)
                )

                // Load rooms của building
                val rooms = roomRepo.getRoomsByBuildingId(building.id)
                _formState.value = _formState.value.copy(availableRooms = rooms)

                // Tìm phòng nào đang có service này
                val selectedRooms = rooms.filter { room ->
                    room.extraService.any { it.id == serviceId }
                }.map { it.id }.toSet()

                _formState.value = _formState.value.copy(selectedRooms = selectedRooms)

                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error loading service")
            }
        }
    }

    // Reuse các hàm từ AddServiceVM
    fun onNameChange(value: String) {
        _formState.value = _formState.value.copy(name = value)
    }
    fun onPriceChange(value: String) {
        _formState.value = _formState.value.copy(price = value)
    }
    fun onUnitChange(value: Metric) {
        _formState.value = _formState.value.copy(metrics = value)
    }
    fun onToggleRoom(roomId: String) {
        val current = _formState.value.selectedRooms.toMutableSet()
        if (current.contains(roomId)) current.remove(roomId) else current.add(roomId)
        _formState.value = _formState.value.copy(selectedRooms = current)
    }
    fun onToggleSelectAll() {
        val allRooms = _formState.value.availableRooms.map { it.id }.toSet()
        _formState.value = if (_formState.value.selectedRooms.size == allRooms.size) {
            _formState.value.copy(selectedRooms = emptySet())
        } else {
            _formState.value.copy(selectedRooms = allRooms)
        }
    }
    fun onSearchTextChange(value: String) {
        _formState.value = _formState.value.copy(searchText = value)
    }
    fun onConfirm() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val state = _formState.value
            val building = state.selectedBuilding ?: return@launch
            val oldService = originalService ?: return@launch

            val updatedService = oldService.copy(
                name = state.name,
                price = state.price,
                metric = state.metrics
            )

            _uiState.value = UiState.Loading

            try {
                // Cập nhật service trong building
                val updatedList = building.services.map {
                    if (it.id == oldService.id) updatedService else it
                }
                buildingRepo.updateFields(building.id, mapOf("services" to updatedList))

                // Cập nhật trong room
                val rooms = state.availableRooms
                for (room in rooms) {
                    val hasService = room.extraService.any { it.id == oldService.id }
                    val shouldHaveService = state.selectedRooms.contains(room.id)

                    if (hasService && !shouldHaveService) {
                        // remove
                        val updatedExtra = room.extraService.filterNot { it.id == oldService.id }
                        roomRepo.update(room.id, room.copy(extraService = updatedExtra))
                    } else if (!hasService && shouldHaveService) {
                        // add
                        val updatedExtra = room.extraService + updatedService
                        roomRepo.update(room.id, room.copy(extraService = updatedExtra))
                    } else if (hasService && shouldHaveService) {
                        // update existing
                        val updatedExtra = room.extraService.map {
                            if (it.id == oldService.id) updatedService else it
                        }
                        roomRepo.update(room.id, room.copy(extraService = updatedExtra))
                    }
                }

                _uiState.value = UiState.Success(updatedService)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error updating service")
            }
        }
    }

    fun clearError() { _uiState.value = UiState.Idle }
}