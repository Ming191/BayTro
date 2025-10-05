package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.building.Building
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.room.RoomRepository
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.ServiceRepository
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddServiceVM(
    private val buildingRepo: BuildingRepository,
    private val roomRepo: RoomRepository,
    private val serviceRepo: ServiceRepository,
    private val auth: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AddServiceVM"
    }

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState

    private val _formState = MutableStateFlow(AddServiceFormState())
    val formState: StateFlow<AddServiceFormState> = _formState

    init {
        Log.d(TAG, "init: fetching buildings for current user")
        fetchBuildings()
    }

    // --- BUILDING ---
    private fun fetchBuildings() {
        val currentUser = auth.getCurrentUser() ?: return
        viewModelScope.launch {
            try {
                val buildings = buildingRepo.getBuildingsByUserId(currentUser.uid)
                _formState.value = _formState.value.copy(availableBuildings = buildings)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error while fetching buildings")
            }
        }
    }

    fun onBuildingSelected(building: Building) {
        _formState.value = _formState.value.copy(
            selectedBuilding = building,
            selectedRooms = emptySet()
        )
        fetchRooms(building.id)
    }

    // --- ROOM ---
    private fun fetchRooms(buildingId: String) {
        viewModelScope.launch {
            try {
                val rooms = roomRepo.getRoomsByBuildingId(buildingId)
                _formState.value = _formState.value.copy(availableRooms = rooms)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error while fetching rooms")
            }
        }
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

    fun onNameChange(value: String) { _formState.value = _formState.value.copy(name = value) }
    fun onDescriptionChange(value: String) { _formState.value = _formState.value.copy(description = value) }
    fun onPriceChange(value: String) { _formState.value = _formState.value.copy(price = value) }
    fun onUnitChange(value: String) { _formState.value = _formState.value.copy(unit = value) }

    fun onSearchTextChange(value: String) {
        _formState.value = _formState.value.copy(searchText = value)
    }

    fun onConfirm() {
        viewModelScope.launch {
            val state = _formState.value
            if (state.name.isBlank() || state.price.isBlank() || state.unit.isBlank() || state.selectedBuilding == null) {
                _uiState.value = UiState.Error("Please fill all required fields")
                return@launch
            }

            _uiState.value = UiState.Loading
            try {
                val newService = Service(
                    id = "",
                    name = state.name,
                    description = state.description,
                    price = state.price,
                    unit = state.unit,
                    icon = state.icon,
                    buildingID = state.selectedBuilding.id
                )
                serviceRepo.add(newService)
                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Error while adding service")
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
