package com.example.baytro.viewModel.service

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.building.Building
import com.example.baytro.data.building.BuildingRepository
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.ServiceRepository
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceListVM(
    private val buildingRepo: BuildingRepository,
    private val serviceRepo: ServiceRepository,
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

    private fun fetchServices(buildingId: String) {
        viewModelScope.launch {
            try {
                _serviceListUiState.value = UiState.Loading
                val services = serviceRepo.getServicesByBuildingId(buildingId)
                _serviceListFormState.value =
                    _serviceListFormState.value.copy(availableServices = services)
                _serviceListUiState.value = UiState.Success(services)
            } catch (e: Exception) {
                Log.e(TAG, "fetchServices error", e)
                _serviceListUiState.value =
                    UiState.Error(e.message ?: "Error while fetching services")
            }
        }
    }

    fun onBuildingChange(building: Building) {
        _serviceListFormState.value =
            _serviceListFormState.value.copy(selectedBuilding = building)
        if (building.id.isNotBlank()) {
            fetchServices(building.id)
        }
    }

    fun onEditService(service: Service) {
        _serviceListFormState.value = _serviceListFormState.value.copy(selectedService = service)
    }

    fun onDeleteService(service: Service) {
        viewModelScope.launch {
            try {
                serviceRepo.delete(service.id)
                val current = _serviceListFormState.value.availableServices.toMutableList()
                current.remove(service)
                _serviceListFormState.value =
                    _serviceListFormState.value.copy(availableServices = current)
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
