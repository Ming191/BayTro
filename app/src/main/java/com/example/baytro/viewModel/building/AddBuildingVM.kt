package com.example.baytro.viewModel.building

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Building
import com.example.baytro.data.BuildingRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.Status
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddBuildingVM(
    private val context: Context,
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    
    private val _addBuildingUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val addBuildingUIState: StateFlow<AuthUIState> = _addBuildingUIState

    private fun createDefaultServices(): List<Service> {
        return listOf(
            Service(
                name = "Water",
                price = "18000",
                metric = Metric.M3,
                status = Status.ACTIVE
            ),
            Service(
                name = "Electricity",
                price = "4000",
                metric = Metric.KWH,
                status = Status.ACTIVE
            )
        )
    }

    fun addBuilding(building: Building) {
        viewModelScope.launch {
            _addBuildingUIState.value = AuthUIState.Loading
            try {
                val currentUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("No logged in user found")
                
                val buildingWithDefaults = building.copy(
                    userId = currentUser.uid,
                    services = createDefaultServices()
                )
                val newId = buildingRepository.add(buildingWithDefaults)

                // Update the document to include its own ID
                buildingRepository.updateFields(newId, mapOf("id" to newId))
                
                Log.d("AddBuildingVM", "Building created with default services")

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

                val buildingWithDefaults = building.copy(
                    userId = currentUser.uid,
                    imageUrls = emptyList(),
                    services = createDefaultServices()
                )
                val newId = buildingRepository.add(buildingWithDefaults)

                buildingRepository.updateFields(newId, mapOf("id" to newId))
                Log.d("AddBuildingVM", "Building created with ID: $newId")

                val limitedUris = imageUris.take(3)
                Log.d("AddBuildingVM", "Starting optimized upload of ${limitedUris.size} images")
                val startTime = System.currentTimeMillis()
                
                // Tối ưu: Compress và upload song song cho từng ảnh
                val uploadedUrls = limitedUris.mapIndexed { index, uri ->
                    async {
                        val imageStartTime = System.currentTimeMillis()
                        Log.d("AddBuildingVM", "Processing image $index...")
                        
                        try {
                            val compressedFile = ImageProcessor.compressImage(context, uri)
                            val compressionTime = System.currentTimeMillis() - imageStartTime
                            Log.d("AddBuildingVM", "Image $index compressed in ${compressionTime}ms, size: ${compressedFile.length()} bytes")
                            
                            // Upload ngay
                            val uploadStartTime = System.currentTimeMillis()
                            val compressedUri = Uri.fromFile(compressedFile)
                            val url = mediaRepository.uploadBuildingImage(currentUser.uid, newId, compressedUri)
                            val uploadTime = System.currentTimeMillis() - uploadStartTime
                            Log.d("AddBuildingVM", "Image $index uploaded in ${uploadTime}ms")
                            
                            // Cleanup temp file
                            compressedFile.delete()
                            
                            val totalImageTime = System.currentTimeMillis() - imageStartTime
                            Log.d("AddBuildingVM", "Image $index total time: ${totalImageTime}ms")
                            
                            url
                        } catch (e: Exception) {
                            Log.e("AddBuildingVM", "Failed to process image $index", e)
                            throw e
                        }
                    }
                }.awaitAll()
                
                val totalTime = System.currentTimeMillis() - startTime
                Log.d("AddBuildingVM", "All ${limitedUris.size} images processed in ${totalTime}ms")
                
                // Cập nhật building với URLs
                if (uploadedUrls.isNotEmpty()) {
                    buildingRepository.updateFields(newId, mapOf("imageUrls" to uploadedUrls))
                    Log.d("AddBuildingVM", "Building updated with ${uploadedUrls.size} image URLs")
                }

                _addBuildingUIState.value = AuthUIState.Success(currentUser)
                Log.d("AddBuildingVM", "Add building completed successfully")

            } catch (e: Exception) {
                Log.e("AddBuildingVM", "Failed to add building with images", e)
                _addBuildingUIState.value = AuthUIState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
