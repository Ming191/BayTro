package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.building.Building
import com.example.baytro.data.building.BuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BuildingListVM(
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _buildings = MutableStateFlow<List<Building>>(emptyList())
    val buildings: StateFlow<List<Building>> = _buildings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadBuildings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                
                val userBuildings = buildingRepository.getBuildingsByUserId(currentUser.uid)
                _buildings.value = userBuildings
            } catch (e: Exception) {
                // Handle error - could emit error state
                _buildings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
