package com.example.baytro.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class EditBuildingVM(
    private val context: Context,
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
                Log.d("EditBuildingVM", "Starting parallel upload of ${limited.size} images")
                val startTime = System.currentTimeMillis()
                
                // Step 1: Compress all images in parallel first
                Log.d("EditBuildingVM", "Step 1: Compressing ${limited.size} images in parallel...")
                val compressionStartTime = System.currentTimeMillis()
                
                val compressedFiles = limited.mapIndexed { index, uri ->
                    async {
                        Log.d("EditBuildingVM", "Compressing image $index...")
                        val imageStartTime = System.currentTimeMillis()
                        val compressedFile = ImageProcessor.compressImage(context, uri)
                        val compressionTime = System.currentTimeMillis() - imageStartTime
                        Log.d("EditBuildingVM", "Image $index compressed in ${compressionTime}ms, size: ${compressedFile.length()} bytes")
                        compressedFile
                    }
                }.awaitAll()
                
                val totalCompressionTime = System.currentTimeMillis() - compressionStartTime
                Log.d("EditBuildingVM", "All images compressed in ${totalCompressionTime}ms")
                
                // Step 2: Upload all compressed images in parallel
                Log.d("EditBuildingVM", "Step 2: Uploading ${compressedFiles.size} images in parallel...")
                val uploadStartTime = System.currentTimeMillis()
                
                val urls = compressedFiles.mapIndexed { index, compressedFile ->
                    async {
                        Log.d("EditBuildingVM", "Edit building info")
                        val imageUploadStartTime = System.currentTimeMillis()
                        val compressedUri = Uri.fromFile(compressedFile)
                        val url = mediaRepository.uploadBuildingImage(currentUser.uid, building.id, compressedUri)
                        val uploadTime = System.currentTimeMillis() - imageUploadStartTime
                        Log.d("EditBuildingVM", "Image $index uploaded in ${uploadTime}ms")
                        url
                    }
                }.awaitAll()
                
                val totalUploadTime = System.currentTimeMillis() - uploadStartTime
                Log.d("EditBuildingVM", "All images uploaded in ${totalUploadTime}ms")
                
                val totalTime = System.currentTimeMillis() - startTime
                Log.d("EditBuildingVM", "All ${limited.size} images processed in ${totalTime}ms")
                val merged = if (urls.isEmpty()) building.imageUrls else urls
                buildingRepository.update(building.id, building.copy(imageUrls = merged))
                _editUIState.value = AuthUIState.Success(currentUser)
            } catch (e: Exception) {
                _editUIState.value = AuthUIState.Error(e.message ?: "Failed to update building")
            }
        }
    }
}
