package com.example.baytro.viewModel.splash

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.user.Gender
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
import java.io.File

class UploadIdCardVM(
    private val context: Context,
    private val mediaRepo: MediaRepository,
    private val auth: AuthRepository,
    private val fptAiService: FptAiService
) : ViewModel() {
    companion object {
        private const val TAG = "UploadIdCardVM"
        private const val ID_CARD_FRONT_IMAGE_NAME = "front"
        private const val ID_CARD_BACK_IMAGE_NAME = "back"
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
                        formState.selectedPhotos.isEmpty() || formState.selectedPhotos.size < 2 -> ValidationResult.Error("Please upload both front and back side photos of your ID card")
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
        val currentUser = auth.getCurrentUser()?: run {
            Log.e(TAG, "onSubmit: no authenticated user found; aborting submit")
            _uploadIdCardUiState.value = UiState.Error("User not authenticated")
            return
        }

        Log.d(TAG, "onSubmit: processing ID card with OCR for ${formState.selectedPhotos.size} photos")
        viewModelScope.launch {
            _uploadIdCardUiState.value = UiState.Loading
            try {
                val processingJobs = formState.selectedPhotos.mapIndexed { index, photoUri ->
                    async { processPhoto(currentUser.uid, photoUri, index) }
                }

                val results = processingJobs.awaitAll()
                val frontResult = results[0]
                val backResult = results[1]

                Log.d(TAG, "onSubmit: combining front and back side information")
                val combinedIdCardInfo = combineIdCardInfo(frontResult.idCardInfo,
                    backResult.idCardInfo
                )

                if (combinedIdCardInfo != null) {
                    Log.d(TAG, "onSubmit: OCR processing completed successfully, combined info: $combinedIdCardInfo")
                    val enhancedIdCardInfo = IdCardInfoWithImages(
                        idCardInfo = combinedIdCardInfo,
                        frontImageUrl = frontResult.photoUrl,
                        backImageUrl = backResult.photoUrl
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

        var compressedFile: File? = null
        var photoUrl: String
        var idCardInfo: IdCardInfo? = null

        try {
            val compressionStartTime = System.nanoTime()
            compressedFile = ImageProcessor.compressImage(context, photoUri, maxWidth = 1440, quality = 90)
            val compressionDuration = (System.nanoTime() - compressionStartTime) / 1_000_000
            Log.d(TAG, "processPhoto: compressed photo $index (size: ${compressedFile.length()} bytes) in $compressionDuration ms")

            val uploadStartTime = System.nanoTime()
            photoUrl = mediaRepo.uploadUserImage(
                userId = userId,
                imageUri = Uri.fromFile(compressedFile),
                subfolder = "idcards",
                imageName = if (index == 0) ID_CARD_FRONT_IMAGE_NAME else ID_CARD_BACK_IMAGE_NAME
            )
            val uploadDuration = (System.nanoTime() - uploadStartTime) / 1_000_000
            Log.d(TAG, "processPhoto: uploaded ID card photo $index -> $photoUrl in $uploadDuration ms")

            val ocrStartTime = System.nanoTime()
            val ocrResult = fptAiService.extractIdCardInfo(photoUrl, context.cacheDir)
            val ocrDuration = (System.nanoTime() - ocrStartTime) / 1_000_000

            ocrResult.fold(
                onSuccess = { info ->
                    Log.d(TAG, "processPhoto: OCR success for photo $index in $ocrDuration ms")
                    idCardInfo = info
                },
                onFailure = { error ->
                    Log.w(TAG, "processPhoto: OCR failed for photo $index in $ocrDuration ms:", error)
                    idCardInfo = null
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "processPhoto: overall error during processing photo $index", e)
            return OcrProcessingResult("", null)
        } finally {
            compressedFile?.let {
                if (it.exists()) {
                    val deleted = it.delete()
                    Log.d(TAG, "processPhoto: Deleted compressed file for photo $index: $deleted (path: ${it.absolutePath})")
                }
            }
        }
        return OcrProcessingResult(photoUrl, idCardInfo)
    }

    private fun combineIdCardInfo(frontSide: IdCardInfo?, backSide: IdCardInfo?): IdCardInfo? {
        Log.d(TAG, "combineIdCardInfo: combining info - front: $frontSide, back: $backSide")

        if (frontSide == null && backSide == null) {
            Log.w(TAG, "combineIdCardInfo: Both sides are null, cannot combine")
            return null
        }

        val combined = IdCardInfo(
            fullName = selectBestValue(frontSide?.fullName, backSide?.fullName),
            idCardNumber = selectBestValue(frontSide?.idCardNumber, backSide?.idCardNumber),
            dateOfBirth = selectBestValue(frontSide?.dateOfBirth, backSide?.dateOfBirth),
            gender = frontSide?.gender ?: backSide?.gender ?: Gender.OTHER,
            permanentAddress = selectBestValue(frontSide?.permanentAddress, backSide?.permanentAddress),
            idCardIssueDate = selectBestValue(backSide?.idCardIssueDate, frontSide?.idCardIssueDate)
        )

        val hasEssentialInfo = combined.fullName.isNotEmpty() ||
                              combined.idCardNumber.isNotEmpty() ||
                              combined.dateOfBirth.isNotEmpty()

        if (!hasEssentialInfo) {
            Log.w(TAG, "combineIdCardInfo: No essential information found in either image")
            return null
        }

        Log.d(TAG, "combineIdCardInfo: final combined result: $combined")
        return combined
    }

    private fun selectBestValue(value1: String?, value2: String?): String {
        return when {
            !value1.isNullOrEmpty() -> value1
            !value2.isNullOrEmpty() -> value2
            else -> ""
        }
    }
}
