package com.example.baytro.viewModel.splash

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.Gender
import com.example.baytro.data.IdCardInfo
import com.example.baytro.data.IdCardInfoWithImages
import com.example.baytro.data.MediaRepository
import com.example.baytro.service.FptAiService
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UploadIdCardVM(
    private val context: Context,
    private val mediaRepo: MediaRepository,
    private val auth: AuthRepository,
    private val fptAiService: FptAiService
) : ViewModel() {
    companion object {
        private const val TAG = "UploadIdCardVM"
    }

    private val _uploadIdCardUiState = MutableStateFlow<UiState<IdCardInfoWithImages>>(UiState.Idle)
    val uploadIdCardUiState: StateFlow<UiState<IdCardInfoWithImages>> = _uploadIdCardUiState

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

        val photosValidator = when {
            formState.selectedPhotos.isEmpty() || formState.selectedPhotos.size < 2-> ValidationResult.Error("Please upload at both front and back side photos of your ID card")
            else -> ValidationResult.Success
        }

        _uploadIdCardFormState.value = formState.copy(photosError = photosValidator)

        Log.d(TAG, "validateInput: result isValid=${photosValidator is ValidationResult.Success}")
        return photosValidator is ValidationResult.Success
    }

    private data class OcrProcessingResult(
        val photoUrl: String,
        val idCardInfo: IdCardInfo?
    )

    fun onSubmit() {
        Log.d(TAG, "onSubmit: start")
        if (!validateInput()) {
            Log.w(TAG, "onSubmit: validation failed; aborting submit")
            return
        }

        val formState = _uploadIdCardFormState.value
        val currentUser = auth.getCurrentUser()!!

        Log.d(TAG, "onSubmit: processing ID card with OCR for ${formState.selectedPhotos.size} photos")
        viewModelScope.launch {
            _uploadIdCardUiState.value = UiState.Loading
            try {
                val processingJobs = formState.selectedPhotos.mapIndexed { index, photoUri ->
                    async { processPhoto(currentUser.uid, photoUri, index) }
                }

                val (frontResult, backResult) = processingJobs.awaitAll().let {
                    Pair(it.getOrNull(0), it.getOrNull(1))
                }

                Log.d(TAG, "onSubmit: combining front and back side information")
                val combinedIdCardInfo = combineIdCardInfo(frontResult?.idCardInfo, backResult?.idCardInfo)

                if (combinedIdCardInfo != null) {
                    Log.d(TAG, "onSubmit: OCR processing completed successfully, combined info: $combinedIdCardInfo")
                    val enhancedIdCardInfo = IdCardInfoWithImages(
                        idCardInfo = combinedIdCardInfo,
                        frontImageUrl = frontResult?.photoUrl,
                        backImageUrl = backResult?.photoUrl
                    )
                    _uploadIdCardUiState.value = UiState.Success(enhancedIdCardInfo)
                } else {
                    Log.w(TAG, "onSubmit: No valid ID card information extracted from either photo")
                    _uploadIdCardUiState.value = UiState.Error("Could not extract ID card information. Please ensure images are clear and try again.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "onSubmit: error processing ID card", e)
                _uploadIdCardUiState.value = UiState.Error(e.message ?: "An unknown error occurred while processing ID card")
            }
        }
    }

    private suspend fun processPhoto(userId: String, photoUri: Uri, index: Int): OcrProcessingResult {
        Log.d(TAG, "processPhoto: processing ID card photo $index: $photoUri")

        val compressedFile = ImageProcessor.compressImageWithCoil(context, photoUri, maxWidth = 1440, quality = 90)
        Log.d(TAG, "processPhoto: compressed photo $index, file size: ${compressedFile.length()} bytes")

        val photoUrl = mediaRepo.uploadUserImage(
            userId = userId,
            imageUri = Uri.fromFile(compressedFile),
            subfolder = "idcards",
            imageName = if (index == 0) "front" else "back"
        )
        Log.d(TAG, "processPhoto: uploaded ID card photo $index -> $photoUrl")

        val ocrResult = fptAiService.extractIdCardInfo(photoUrl, context.cacheDir)

        return ocrResult.fold(
            onSuccess = { idCardInfo ->
                Log.d(TAG, "processPhoto: OCR success for photo $index")
                OcrProcessingResult(photoUrl, idCardInfo)
            },
            onFailure = { error ->
                Log.w(TAG, "processPhoto: OCR failed for photo $index: ${error.message}")
                OcrProcessingResult(photoUrl, null)
            }
        )
    }

    private fun combineIdCardInfo(frontSide: IdCardInfo?, backSide: IdCardInfo?): IdCardInfo? {
        Log.d(TAG, "combineIdCardInfo: combining info - front: $frontSide, back: $backSide")

        if (frontSide == null || backSide == null) return null


        val combined = IdCardInfo(
            fullName = frontSide.fullName.ifEmpty { "" },
            idCardNumber = frontSide.idCardNumber.ifEmpty {""},
            dateOfBirth = frontSide.dateOfBirth.ifEmpty { "" },
            gender = frontSide.gender,
            permanentAddress = frontSide.permanentAddress.ifEmpty {""},
            idCardIssueDate = backSide.idCardIssueDate.ifEmpty { "" }
        )

        Log.d(TAG, "combineIdCardInfo: final combined result: $combined")
        return combined
    }
}
