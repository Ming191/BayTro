package com.example.baytro.viewModel.splash

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.UserRepository
import com.example.baytro.data.User
import com.example.baytro.data.Role
import com.example.baytro.data.Gender
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UploadIdCardVM(
    private val context: Context,
    private val mediaRepo: MediaRepository,
    private val userRepo: UserRepository,
    private val auth: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "UploadIdCardVM"
    }

    private val _uploadIdCardUiState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val uploadIdCardUiState: StateFlow<UiState<String>> = _uploadIdCardUiState

    private val _uploadIdCardFormState = MutableStateFlow(UploadIdCardFormState())
    val uploadIdCardFormState: StateFlow<UploadIdCardFormState> = _uploadIdCardFormState

    fun onPhotosChange(photos: List<Uri>) {
        Log.d(TAG, "onPhotosChange: photosCount=${photos.size}")
        _uploadIdCardFormState.value = _uploadIdCardFormState.value.copy(
            selectedPhotos = photos,
            photosError = ValidationResult.Success
        )
    }

    fun clearError() {
        Log.d(TAG, "clearError: setting UiState to Idle")
        _uploadIdCardUiState.value = UiState.Idle
    }

    private fun validateInput(): Boolean {
        Log.d(TAG, "validateInput: start")
        val formState = _uploadIdCardFormState.value
        val photosValidator = if (formState.selectedPhotos.isEmpty()) {
            ValidationResult.Error("Please upload at least one ID card photo")
        } else if (formState.selectedPhotos.size != 2) {
            ValidationResult.Error("Please upload both front and back of your ID card")
        } else {
            ValidationResult.Success
        }

        val isValid = photosValidator == ValidationResult.Success

        if (!isValid) {
            Log.w(TAG, "validateInput: validation failed -> photos=$photosValidator")
        }

        Log.d(TAG, "validateInput: result isValid=$isValid")
        _uploadIdCardFormState.value = formState.copy(
            photosError = photosValidator
        )
        return isValid
    }

    fun onSubmit() {
        Log.d(TAG, "onSubmit: start")
        if (!validateInput()) {
            Log.w(TAG, "onSubmit: validation failed; aborting submit")
            return
        }

        val formState = _uploadIdCardFormState.value
        val currentUser = auth.getCurrentUser()
        if (currentUser == null) {
            Log.w(TAG, "onSubmit: no authenticated user; aborting submit")
            _uploadIdCardUiState.value = UiState.Error("No authenticated user. Please log in again.")
            return
        }

        Log.d(TAG, "onSubmit: uploading ID card with ${formState.selectedPhotos.size} photos")
        viewModelScope.launch {
            _uploadIdCardUiState.value = UiState.Loading
            try {
                val userId = currentUser.uid
                val photoUrls = mutableListOf<String>()

                // Upload and compress photos
                formState.selectedPhotos.forEachIndexed { index, photoUri ->
                    Log.d(TAG, "onSubmit: processing ID card photo $index")
                    val compressedFile = ImageProcessor.compressImageWithCoil(
                        context = context,
                        uri = photoUri,
                        maxWidth = 1440,
                        quality = 90
                    )

                    val photoUrl = mediaRepo.uploadIdCardPhoto(
                        userId = userId,
                        photoIndex = index,
                        imageFile = compressedFile
                    )
                    photoUrls.add(photoUrl)
                    Log.d(TAG, "onSubmit: uploaded ID card photo $index -> $photoUrl")
                }

                // Create tenant user with ID card photos
                val tenantRole = Role.Tenant(
                    occupation = "Not specified", // TODO: Can be added to form later
                    idCardNumber = "Not specified", // TODO: Can be added to form later
                    idCardImageFrontUrl = if (photoUrls.isNotEmpty()) photoUrls[0] else null,
                    idCardImageBackUrl = if (photoUrls.size > 1) photoUrls[1] else null,
                    idCardIssueDate = "Not specified", // TODO: Can be added to form later
                    emergencyContact = "Not specified" // TODO: Can be added to form later
                )

                val newTenantUser = User(
                    id = userId,
                    email = currentUser.email ?: "",
                    phoneNumber = "Not specified", // TODO: Can be added to form later
                    role = tenantRole,
                    fullName = "Not specified", // TODO: Can be added to form later
                    dateOfBirth = "Not specified", // TODO: Can be added to form later
                    gender = Gender.OTHER, // TODO: Can be added to form later
                    address = "Not specified", // TODO: Can be added to form later
                    profileImgUrl = null
                )

                Log.d(TAG, "onSubmit: creating tenant user with ID: $userId")
                userRepo.addWithId(userId, newTenantUser)
                Log.d(TAG, "onSubmit: tenant user created successfully")

                Log.d(TAG, "onSubmit: ID card upload and tenant creation completed successfully")
                _uploadIdCardUiState.value = UiState.Success("Tenant user created successfully with ID card")

            } catch (e: Exception) {
                Log.e(TAG, "onSubmit: error uploading ID card and creating tenant user", e)
                _uploadIdCardUiState.value = UiState.Error(e.message ?: "An unknown error occurred while uploading ID card and creating tenant user")
            }
        }
    }
}
