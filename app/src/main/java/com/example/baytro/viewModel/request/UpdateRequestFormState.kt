package com.example.baytro.viewModel.request

import android.net.Uri
import com.example.baytro.utils.ValidationResult

data class UpdateRequestFormState(
    val title: String = "",
    val titleError: ValidationResult = ValidationResult.Success,

    val description: String = "",
    val descriptionError: ValidationResult = ValidationResult.Success,

    val scheduledDate: String = "",
    val scheduledDateError: ValidationResult = ValidationResult.Success,

    val selectedPhotos: List<Uri> = emptyList(),
    val existingImageUrls: List<String> = emptyList(),
    val photoError: ValidationResult = ValidationResult.Success,

    val isLoading: Boolean = true
)

