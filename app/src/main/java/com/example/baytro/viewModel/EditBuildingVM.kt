package com.example.baytro.viewModel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditBuildingVM(
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _building = MutableStateFlow<Building?>(null)
    val building: StateFlow<Building?> = _building

    private val _editUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val editUIState: StateFlow<AuthUIState> = _editUIState

    fun load(id: String) {
        viewModelScope.launch {
            _editUIState.value = AuthUIState.Loading
            try {
                val b = buildingRepository.getById(id)
                _building.value = b
                _editUIState.value = AuthUIState.Idle
            } catch (e: Exception) {
                _editUIState.value = AuthUIState.Error(e.message ?: "Failed to load building")
            }
        }
    }

    fun update(building: Building) {
        viewModelScope.launch {
            _editUIState.value = AuthUIState.Loading
            try {
                val user = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found") // Or handle error differently
                buildingRepository.update(building.id, building)
                _editUIState.value = AuthUIState.Success(user)
            } catch (e: Exception) {
                _editUIState.value = AuthUIState.Error(e.message ?: "Failed to update building")
            }
        }
    }


    fun updateWithImages(building: Building, imageUris: List<Uri>) {
        viewModelScope.launch {
            _editUIState.value = AuthUIState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                val limited = imageUris.take(3)
                val urls = mutableListOf<String>()
                for (uri in limited) {
                    val url = mediaRepository.uploadBuildingImage(currentUser.uid, building.id, uri)
                    urls.add(url)
                }
                val merged = if (urls.isEmpty()) building.imageUrls else urls
                buildingRepository.update(building.id, building.copy(imageUrls = merged))
                _editUIState.value = AuthUIState.Success(currentUser)
            } catch (e: Exception) {
                _editUIState.value = AuthUIState.Error(e.message ?: "Failed to update building")
            }
        }
    }
}
