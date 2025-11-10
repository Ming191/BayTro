package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.BuildingStatus
import com.example.baytro.data.service.Service
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ServiceListVM(
    private val buildingRepo: BuildingRepository,
    private val auth: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ServiceListVM"
    }

    private val _serviceListUiState = MutableStateFlow<UiState<List<Service>>>(UiState.Idle)
    val serviceListUiState: StateFlow<UiState<List<Service>>> = _serviceListUiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _serviceListFormState = MutableStateFlow(ServiceListFormState())
    val serviceListFormState: StateFlow<ServiceListFormState> = _serviceListFormState

    init {
        Log.d(TAG, "init: fetching buildings for current user")
        fetchBuildings()
    }

    private fun fetchBuildings() {
        val currentUser = auth.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "fetchBuildings: no authenticated user")
            return
        }
        val userId = currentUser.uid
        viewModelScope.launch {
            // Only show loading if we don't have buildings yet
            val hasExistingBuildings = _serviceListFormState.value.availableBuildings.isNotEmpty()
            if (!hasExistingBuildings) {
                _serviceListUiState.value = UiState.Loading
            }

            try {
                val buildings = buildingRepo.getBuildingsByUserId(userId)
                val buildingsNoArchived = buildings.filter { BuildingStatus.ARCHIVED != it.status }
                val currentSelectedBuilding = _serviceListFormState.value.selectedBuilding

                _serviceListFormState.value = _serviceListFormState.value.copy(
                    availableBuildings = buildingsNoArchived
                )

                if (buildingsNoArchived.isNotEmpty()) {
                    // Try to preserve the currently selected building
                    val buildingToSelect = if (currentSelectedBuilding != null) {
                        // Check if the currently selected building still exists in the new list
                        buildingsNoArchived.find { it.id == currentSelectedBuilding.id } ?: buildingsNoArchived[0]
                    } else {
                        // No building was selected before, select the first one
                        buildingsNoArchived[0]
                    }

                    // Only trigger onBuildingChange if the building is different
                    if (currentSelectedBuilding?.id != buildingToSelect.id) {
                        onBuildingChange(buildingToSelect)
                    }
                } else {
                    _serviceListUiState.value = UiState.Success(emptyList())
                    onBuildingChange(buildingsNoArchived[0])
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetchBuildings error", e)
                _serviceListUiState.value =
                    UiState.Error(e.message ?: "Error while fetching buildings")
            }
        }
    }

    private fun listenToServicesRealtime(buildingId: String) {
        viewModelScope.launch {
            try {
                buildingRepo.listenToBuildingServices(buildingId)
                    .catch { e ->
                        _serviceListUiState.value = UiState.Error(e.message ?: "Error listening to services")
                    }
                    .collect { services ->
                        _serviceListFormState.value = _serviceListFormState.value.copy(availableServices = services)
                        _serviceListUiState.value = UiState.Success(services)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _serviceListUiState.value =
                    UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun onBuildingChange(building: Building) {
        val current = _serviceListFormState.value.selectedBuilding

        if (current?.id == building.id) return

        Log.d(TAG, "onBuildingChange: ${building.name} - clearing old services")

        _serviceListFormState.value =
            _serviceListFormState.value.copy(
                selectedBuilding = building,
                availableServices = emptyList()
            )

        if (building.id.isNotBlank()) {
            listenToServicesRealtime(building.id)
        } else {
            _serviceListUiState.value = UiState.Success(emptyList())
        }
    }

    fun onEditService(service: Service) {
        _serviceListFormState.value = _serviceListFormState.value.copy(selectedService = service)
        // TODO: Navigate to edit screen
    }


    fun onDeleteService(service: Service) {
        viewModelScope.launch {
            try {
                val building = _serviceListFormState.value.selectedBuilding
                if (building == null) {
                    _serviceListUiState.value = UiState.Error("No building selected")
                    return@launch
                }

                buildingRepo.deleteServiceFromBuilding(building.id, service.id)

                Log.d(TAG, "Service deleted successfully: ${service.name}")
            } catch (e: Exception) {
                Log.e(TAG, "deleteService error", e)
                _serviceListUiState.value =
                    UiState.Error(e.message ?: "Error while deleting service")
            }
        }
    }


    fun clearError() {
        _serviceListUiState.value = UiState.Idle
    }

    fun refresh() {
        Log.d(TAG, "refresh: re-fetching buildings")
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchBuildings()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
