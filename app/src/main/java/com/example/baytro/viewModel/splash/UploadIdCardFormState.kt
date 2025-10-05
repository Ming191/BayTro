package com.example.baytro.viewModel.splash

import android.net.Uri
import com.example.baytro.utils.ValidationResult

data class UploadIdCardFormState(
    val selectedPhotos: List<Uri> = emptyList(),
    val photosError: ValidationResult = ValidationResult.Success,
)
