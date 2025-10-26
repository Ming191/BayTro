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

    private val _uiState = MutableStateFlow<UiState<Service>>(UiState.Loading)
    val uiState: StateFlow<UiState<Service>> = _uiState

    private val _formState = MutableStateFlow(EditServiceFormState())
    val formState: StateFlow<EditServiceFormState> = _formState

    private var originalService: Service? = null

    init {
        viewModelScope.launch { loadService() }
        Log.d("EditServiceVM", "buildingId: $buildingId, serviceId: $serviceId")
    }

    private suspend fun loadService() {
        _uiState.value = UiState.Loading
        try {
            if (buildingId != null && serviceId != null) {
                loadFromBuilding(buildingId, serviceId)
            } else {
                throw Exception("Building ID or Service ID not provided")
            }
            _uiState.value = UiState.Idle
        } catch (e: Exception) {
            Log.e("EditServiceVM", "Error loading service", e)
            _uiState.value = UiState.Error(e.message ?: "Error loading service")
        }
    }

    private suspend fun loadFromBuilding(buildingId: String, serviceId: String) {
        val building = buildingRepo.getById(buildingId) ?: throw Exception("Building not found")
        val service = buildingRepo.getServiceById(buildingId, serviceId)
            ?: throw Exception("Service not found")

        originalService = service

        // Don't load rooms at all - we're editing a building service, not room services
        // Loading rooms causes unnecessary queries and UI glitches
        Log.d("EditServiceVM", "Building service loaded: ${service.name}")

        _formState.value = EditServiceFormState(
            name = service.name,
            price = service.price.toString(),
            metrics = service.metric,
            isDefault = service.isDefault,
            selectedBuilding = building,
            availableBuildings = listOf(building),
            availableRooms = emptyList(), // Don't load rooms to avoid glitches
            selectedRooms = emptySet()
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
                // Building service - only update at building level
                buildingRepo.updateServiceInBuilding(building.id, updatedService)
                Log.d("EditServiceVM", "Building service updated: ${updatedService.name}")

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

    private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
        value = block(value)
    }
}
