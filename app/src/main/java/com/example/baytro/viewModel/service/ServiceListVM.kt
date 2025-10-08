package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
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
            _serviceListUiState.value = UiState.Loading
            try {
                val buildings = buildingRepo.getBuildingsByUserId(userId)
                _serviceListFormState.value = _serviceListFormState.value.copy(
                    availableBuildings = buildings
                )
                if (buildings.isNotEmpty()) {
                    onBuildingChange(buildings[0])
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
                        Log.e(TAG, "Error from upstream Flow", e)
                        _serviceListUiState.value = UiState.Error(e.message ?: "Error listening to services")
                    }
                    .collect { services ->
                        Log.d(TAG, "Received real-time update: ${services.size} services")
                        _serviceListFormState.value = _serviceListFormState.value.copy(availableServices = services)
                        _serviceListUiState.value = UiState.Success(services)
                    }
            } catch (e: CancellationException) {
                Log.i(TAG, "listenToServicesRealtime was cancelled as expected.")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred in listenToServicesRealtime", e)
                _serviceListUiState.value =
                    UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun onBuildingChange(building: Building) {
        val current = _serviceListFormState.value.selectedBuilding

        if (current?.id == building.id) return

        Log.d(TAG, "onBuildingChange: ${building.name} - clearing old services")

        _serviceListUiState.value = UiState.Loading

        _serviceListFormState.value =
            _serviceListFormState.value.copy(
                selectedBuilding = building,
                availableServices = emptyList()
            )

        if (building.id.isNotBlank()) {
            listenToServicesRealtime(building.id)
        }
    }

    fun onEditService(service: Service) {
        _serviceListFormState.value = _serviceListFormState.value.copy(selectedService = service)
        // TODO: Navigate to edit screen
    }

    fun onDeleteService(service: Service) {
        viewModelScope.launch {
            try {
                _serviceListUiState.value = UiState.Loading
                val building = _serviceListFormState.value.selectedBuilding
                if (building == null) {
                    _serviceListUiState.value = UiState.Error("No building selected")
                    return@launch
                }

                val updatedServices = building.services.toMutableList()

                val removed = updatedServices.removeAll {
                    it.name == service.name &&
                            it.price == service.price &&
                            it.metric == service.metric
                }

                if (!removed) {
                    Log.w(TAG, "Service not found in building's service list")
                    _serviceListUiState.value = UiState.Error("Service not found")
                    return@launch
                }

                buildingRepo.updateFields(building.id, mapOf("services" to updatedServices))

                _serviceListFormState.value = _serviceListFormState.value.copy(
                    selectedBuilding = building.copy(services = updatedServices)
                )

                Log.d(TAG, "Service deleted successfully: ${service.name}")
                _serviceListUiState.value = UiState.Success(updatedServices)
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
}
