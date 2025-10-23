package com.example.baytro.viewModel

import android.net.Uri
import com.example.baytro.data.service.Service

data class AddBuildingFormState(
    val name: String = "",
    val floor: String = "",
    val address: String = "",
    val status: String = "Active",
    val billingDate: String = "",
    val paymentStart: String = "",
    val paymentDue: String = "",
    val buildingServices: List<Service> = emptyList(),
    val selectedImages: List<Uri> = emptyList(),
    val existingImageUrls: List<String> = emptyList(),

    val nameError: String? = null,
    val floorError: String? = null,
    val addressError: String? = null,
    val billingError: String? = null,
    val startError: String? = null,
    val dueError: String? = null
)