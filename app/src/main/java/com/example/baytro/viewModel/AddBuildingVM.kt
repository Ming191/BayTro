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

class AddBuildingVM(
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _addBuildingUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val addBuildingUIState: StateFlow<AuthUIState> = _addBuildingUIState

    fun addBuilding(building: Building) {
        viewModelScope.launch {
            _addBuildingUIState.value = AuthUIState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                
                val buildingWithUserId = building.copy(userId = currentUser.uid)
                buildingRepository.add(buildingWithUserId)
                _addBuildingUIState.value = AuthUIState.Success(currentUser)
            } catch (e: Exception) {
                _addBuildingUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
    fun addBuildingWithImages(building: Building, imageUris: List<Uri>) {
        viewModelScope.launch {
            _addBuildingUIState.value = AuthUIState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")

                val buildingWithUserId = building.copy(userId = currentUser.uid, imageUrls = emptyList())
                val newId = buildingRepository.add(buildingWithUserId)

                val limitedUris = imageUris.take(3)
                val uploadedUrls = mutableListOf<String>()
                for (uri in limitedUris) {
                    val url = mediaRepository.uploadBuildingImage(currentUser.uid, newId, uri)
                    uploadedUrls.add(url)
                }
                if (uploadedUrls.isNotEmpty()) {
                    buildingRepository.updateFields(newId, mapOf("imageUrls" to uploadedUrls))
                }
                _addBuildingUIState.value = AuthUIState.Success(currentUser)
            } catch (e: Exception) {
                _addBuildingUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
