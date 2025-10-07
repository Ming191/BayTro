package com.example.baytro.view.screens.building

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.Building
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.EditBuildingVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBuildingScreen(
    navController: NavHostController? = null,
    buildingId: String,
    viewModel: EditBuildingVM = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.editUIState.collectAsState()
    val buildingState by viewModel.building.collectAsState()

    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }
    var billingDate by remember { mutableStateOf("") }
    var paymentStart by remember { mutableStateOf("") }
    var paymentDue by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }

    var nameError by remember { mutableStateOf(false) }
    var floorError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var billingError by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    var dueError by remember { mutableStateOf(false) }

    var nameErrorMsg by remember { mutableStateOf<String?>(null) }
    var floorErrorMsg by remember { mutableStateOf<String?>(null) }
    var addressErrorMsg by remember { mutableStateOf<String?>(null) }
    var billingErrorMsg by remember { mutableStateOf<String?>(null) }
    var startErrorMsg by remember { mutableStateOf<String?>(null) }
    var dueErrorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(buildingId) {
        viewModel.load(buildingId)
    }

    LaunchedEffect(buildingState) {
        val b = buildingState ?: return@LaunchedEffect
        name = b.name
        floor = b.floor.toString()
        address = b.address
        status = b.status
        billingDate = b.billingDate.toString()
        paymentStart = b.paymentStart.toString()
        paymentDue = b.paymentDue.toString()
        selectedImages = emptyList()
        existingImageUrls = b.imageUrls
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Building updated successfully", Toast.LENGTH_SHORT).show()
                navController?.popBackStack()
            }

            is AuthUIState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Building Details Section
            item {
                DividerWithSubhead(subhead = "Building Details")
            }

            item {
                RequiredTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        val res = BuildingValidator.validateName(name)
                        nameError = res.isError
                        nameErrorMsg = res.message
                    },
                    label = "Building name",
                    isError = nameError,
                    errorMessage = nameErrorMsg,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { floorFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                )
            }

            item {
                RequiredTextField(
                    value = floor,
                    onValueChange = {
                        floor = it
                        val res = BuildingValidator.validateFloor(floor)
                        floorError = res.isError
                        floorErrorMsg = res.message
                    },
                    label = "Floor",
                    isError = floorError,
                    errorMessage = floorErrorMsg,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { addressFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(floorFocus)
                )
            }

            item {
                RequiredTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        val res = BuildingValidator.validateAddress(address)
                        addressError = res.isError
                        addressErrorMsg = res.message
                    },
                    label = "Address",
                    isError = addressError,
                    errorMessage = addressErrorMsg,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { billingFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(addressFocus)
                )
            }

            item {
                DropdownSelectField(
                    label = "Status",
                    options = listOf("Active", "Inactive"),
                    selectedOption = status,
                    onOptionSelected = { status = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Schedule Section
            item {
                DividerWithSubhead(subhead = "Payment Schedule")
            }

            item {
                RequiredTextField(
                    value = billingDate,
                    onValueChange = {
                        billingDate = it
                        val res = BuildingValidator.validateBillingDate(billingDate)
                        billingError = res.isError
                        billingErrorMsg = res.message
                    },
                    label = "Billing date",
                    isError = billingError,
                    errorMessage = billingErrorMsg,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { startFocus.requestFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(billingFocus)
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        RequiredTextField(
                            value = paymentStart,
                            onValueChange = {
                                paymentStart = it
                                val res = BuildingValidator.validatePaymentStart(paymentStart)
                                startError = res.isError
                                startErrorMsg = res.message
                            },
                            label = "Payment start",
                            isError = startError,
                            errorMessage = startErrorMsg,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { dueFocus.requestFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(startFocus)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        RequiredTextField(
                            value = paymentDue,
                            onValueChange = {
                                paymentDue = it
                                val res = BuildingValidator.validatePaymentDue(paymentDue)
                                dueError = res.isError
                                dueErrorMsg = res.message
                            },
                            label = "Payment due",
                            isError = dueError,
                            errorMessage = dueErrorMsg,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { dueFocus.freeFocus() }
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(dueFocus)
                        )
                    }
                }
            }

            // Building Images Section
            item {
                DividerWithSubhead(subhead = "Building Images")
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Use PhotoCarousel component with 16:9 ratio and existing images
                    PhotoCarousel(
                        selectedPhotos = selectedImages,
                        onPhotosSelected = { newPhotos ->
                            selectedImages = newPhotos
                        },
                        existingImageUrls = existingImageUrls,
                        onExistingImagesChanged = { updatedUrls ->
                            existingImageUrls = updatedUrls
                        },
                        maxSelectionCount = 3,
                        imageWidth = 160.dp,
                        imageHeight = 90.dp,
                        aspectRatioX = 16f,
                        aspectRatioY = 9f,
                        maxResultWidth = 1920,
                        maxResultHeight = 1080
                    )
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        if (uiState is AuthUIState.Loading) return@Button

                        // Reset errors
                        nameError = false; nameErrorMsg = null
                        floorError = false; floorErrorMsg = null
                        addressError = false; addressErrorMsg = null
                        billingError = false; billingErrorMsg = null
                        startError = false; startErrorMsg = null
                        dueError = false; dueErrorMsg = null

                        val checks = listOf(
                            "name" to BuildingValidator.validateName(name),
                            "floor" to BuildingValidator.validateFloor(floor),
                            "address" to BuildingValidator.validateAddress(address),
                            "billing" to BuildingValidator.validateBillingDate(billingDate),
                            "start" to BuildingValidator.validatePaymentStart(paymentStart),
                            "due" to BuildingValidator.validatePaymentDue(paymentDue),
                        )
                        val firstInvalid = checks.firstOrNull { it.second.isError }

                        if (firstInvalid != null) {
                            when (firstInvalid.first) {
                                "name" -> {
                                    nameError = true
                                    nameErrorMsg = firstInvalid.second.message
                                    nameFocus.requestFocus()
                                }
                                "floor" -> {
                                    floorError = true
                                    floorErrorMsg = firstInvalid.second.message
                                    floorFocus.requestFocus()
                                }
                                "address" -> {
                                    addressError = true
                                    addressErrorMsg = firstInvalid.second.message
                                    addressFocus.requestFocus()
                                }
                                "billing" -> {
                                    billingError = true
                                    billingErrorMsg = firstInvalid.second.message
                                    billingFocus.requestFocus()
                                }
                                "start" -> {
                                    startError = true
                                    startErrorMsg = firstInvalid.second.message
                                    startFocus.requestFocus()
                                }
                                "due" -> {
                                    dueError = true
                                    dueErrorMsg = firstInvalid.second.message
                                    dueFocus.requestFocus()
                                }
                            }
                        } else {
                            val building = Building(
                                id = buildingId,
                                name = name,
                                floor = floor.toIntOrNull() ?: 0,
                                address = address,
                                status = status,
                                billingDate = billingDate.toIntOrNull() ?: 0,
                                paymentStart = paymentStart.toIntOrNull() ?: 0,
                                paymentDue = paymentDue.toIntOrNull() ?: 0,
                                userId = viewModel.building.value?.userId ?: "",
                                imageUrls = existingImageUrls
                            )
                            
                            Log.d("EditBuildingScreen", "Existing images: ${existingImageUrls.size}, New images: ${selectedImages.size}")

                            if (selectedImages.isNotEmpty()) {
                                viewModel.updateWithImages(building, selectedImages)
                            } else {
                                viewModel.update(building)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(50.dp),
                    enabled = uiState !is AuthUIState.Loading
                ) {
                    if (uiState is AuthUIState.Loading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Edit building info",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}