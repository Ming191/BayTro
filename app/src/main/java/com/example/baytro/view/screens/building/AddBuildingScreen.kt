package com.example.baytro.view.screens.building

import android.net.Uri
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.baytro.data.BuildingStatus
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.building.AddBuildingVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    navController: NavHostController? = null,
    viewModel: AddBuildingVM = koinViewModel()
) {
    val uiState by viewModel.addBuildingUIState.collectAsState()
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(BuildingStatus.ACTIVE) }
    var billingDate by remember { mutableStateOf("") }
    var paymentStart by remember { mutableStateOf("") }
    var paymentDue by remember { mutableStateOf("") }

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

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }
    val selectedImages = remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Handle UI state changes
    LaunchedEffect(key1 = uiState) {
        when (val state = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(
                    context,
                    "Building added successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                navController?.popBackStack()
            }

            is AuthUIState.Error -> {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DividerWithSubhead(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    subhead = "Building Details"
                )
            }
            
            item {
                RequiredTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        val res = BuildingValidator.validateName(name)
                        nameError = res.isError; nameErrorMsg = res.message
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
                OutlinedTextField(
                    value = floor,
                    onValueChange = { newValue ->
                        floor = newValue
                        val res = BuildingValidator.validateFloor(floor)
                        floorError = res.isError; floorErrorMsg = res.message
                    },
                    label = { Text("Floor") },
                    singleLine = true,
                    isError = floorError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { addressFocus.requestFocus() }
                    ),
                    supportingText = {
                        if (floorError) Text(
                            text = floorErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(floorFocus)
                )
            }

            item {
                RequiredTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        val res = BuildingValidator.validateAddress(address)
                        addressError = res.isError; addressErrorMsg = res.message
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
                    options = BuildingStatus.entries.map { it.name },
                    selectedOption = status.name,
                    onOptionSelected = { status = BuildingStatus.valueOf(it) },
                    modifier = Modifier.fillMaxWidth(),
                    optionToString = { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                )
            }

            item {
                DividerWithSubhead(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    subhead = "Payment Schedule"
                )
            }

            item {
                OutlinedTextField(
                    value = billingDate,
                    onValueChange = { newValue ->
                        billingDate = newValue
                        val res = BuildingValidator.validateBillingDate(billingDate)
                        billingError = res.isError; billingErrorMsg = res.message
                    },
                    label = { Text("Billing date") },
                    singleLine = true,
                    isError = billingError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { startFocus.requestFocus() }
                    ),
                    supportingText = {
                        if (billingError) Text(
                            text = billingErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(billingFocus)
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = paymentStart,
                            onValueChange = { newValue ->
                                paymentStart = newValue
                                val res =
                                    BuildingValidator.validatePaymentStart(paymentStart)
                                startError = res.isError; startErrorMsg = res.message
                            },
                            label = { Text("Payment start") },
                            singleLine = true,
                            isError = startError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { dueFocus.requestFocus() }
                            ),
                            supportingText = {
                                if (startError) Text(
                                    text = startErrorMsg ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(startFocus)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = paymentDue,
                            onValueChange = { newValue ->
                                paymentDue = newValue
                                val res =
                                    BuildingValidator.validatePaymentDue(paymentDue)
                                dueError = res.isError; dueErrorMsg = res.message
                            },
                            label = { Text("Payment due") },
                            singleLine = true,
                            isError = dueError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { dueFocus.freeFocus() }
                            ),
                            supportingText = {
                                if (dueError) Text(
                                    text = dueErrorMsg ?: "",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(dueFocus)
                        )
                    }
                }
            }

            item {
                DividerWithSubhead(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    subhead = "Building Images"
                )
            }
            
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header with image count
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "${selectedImages.value.size}/3 images selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Use PhotoCarousel component
                    PhotoCarousel(
                        selectedPhotos = selectedImages.value,
                        onPhotosSelected = { newPhotos ->
                            selectedImages.value = newPhotos
                        },
                        maxSelectionCount = 3,
                        imageWidth = 150.dp,
                        imageHeight = 200.dp,
                        aspectRatioX = 3f,
                        aspectRatioY = 4f,
                        maxResultWidth = 1080,
                        maxResultHeight = 1080
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (uiState is AuthUIState.Loading) return@Button
                        // Reset error
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
                                    nameError = true; nameErrorMsg =
                                        firstInvalid.second.message; nameFocus.requestFocus()
                                }

                                "floor" -> {
                                    floorError = true; floorErrorMsg =
                                        firstInvalid.second.message; floorFocus.requestFocus()
                                }

                                "address" -> {
                                    addressError = true; addressErrorMsg =
                                        firstInvalid.second.message; addressFocus.requestFocus()
                                }

                                "billing" -> {
                                    billingError = true; billingErrorMsg =
                                        firstInvalid.second.message; billingFocus.requestFocus()
                                }

                                "start" -> {
                                    startError = true; startErrorMsg =
                                        firstInvalid.second.message; startFocus.requestFocus()
                                }

                                "due" -> {
                                    dueError = true; dueErrorMsg =
                                        firstInvalid.second.message; dueFocus.requestFocus()
                                }
                            }
                        }

                        val allGood = firstInvalid == null
                        if (allGood) {
                            val floorInt = floor.toInt()
                            val billingDateInt = billingDate.toInt()
                            val paymentStartInt = paymentStart.toInt()
                            val paymentDueInt = paymentDue.toInt()
                            val building = Building(
                                id = "",
                                name = name,
                                floor = floorInt,
                                address = address,
                                status = status,
                                billingDate = billingDateInt,
                                paymentStart = paymentStartInt,
                                paymentDue = paymentDueInt,
                                imageUrls = emptyList(),
                            )
                            val images = selectedImages.value
                            if (images.isEmpty()) {
                                viewModel.addBuilding(building)
                            } else {
                                viewModel.addBuildingWithImages(building, images)
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
                                text = "Add building info",
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
