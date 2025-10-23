package com.example.baytro.view.screens.building

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.baytro.data.Building
import com.example.baytro.data.service.Service
import com.example.baytro.navigation.Screens
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.viewModel.AddBuildingVM
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    navController: NavHostController? = null,
    viewModel: AddBuildingVM = koinViewModel()
) {
    val uiState by viewModel.addBuildingUIState.collectAsState()
    val addBuildingFormState by viewModel.addBuildingFormState.collectAsState()
    val context = LocalContext.current
    val name: (String) -> Unit = viewModel::onNameChange
    val floor: (String) -> Unit = viewModel::onFloorChange
    val address: (String) -> Unit = viewModel::onAddressChange
    val status: (String) -> Unit = viewModel::onStatusChange
    val billingDate: (String) -> Unit = viewModel::onBillingDateChange
    val paymentStart: (String) -> Unit = viewModel::onPaymentStartChange
    val paymentDue: (String) -> Unit = viewModel::onPaymentDueChange

    val buildingServices by viewModel.buildingServices.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(navController) {
        val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<String>("newService")?.observe(lifecycleOwner) { json ->
            val service = Json.decodeFromString<Service>(json)
            Log.d("ServiceListScreen", "Received new service: $service")
            // xử lý thêm service vào danh sách hoặc cập nhật UI
            viewModel.onBuildingServicesChange(service)
            savedStateHandle.remove<String>("newService")
        }
    }

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
                    value = addBuildingFormState.name,
                    onValueChange = {
                        name(it)
                        val res = BuildingValidator.validateName(addBuildingFormState.name)
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
                    value = addBuildingFormState.floor,
                    onValueChange = {
                        floor(it)
                        val res = BuildingValidator.validateFloor(addBuildingFormState.floor)
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
                    value = addBuildingFormState.address,
                    onValueChange = {
                        address(it)
                        val res = BuildingValidator.validateAddress(addBuildingFormState.address)
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
                    options = listOf("Active", "Inactive"),
                    selectedOption = addBuildingFormState.status,
                    onOptionSelected = status ,
                    modifier = Modifier.fillMaxWidth()
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
                    value = addBuildingFormState.billingDate,
                    onValueChange = {
                        billingDate(it)
                        val res = BuildingValidator.validateBillingDate(addBuildingFormState.billingDate)
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
                            value = addBuildingFormState.paymentStart,
                            onValueChange = {
                                paymentStart(it)
                                val res =
                                    BuildingValidator.validatePaymentStart(addBuildingFormState.paymentStart)
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
                            value = addBuildingFormState.paymentDue,
                            onValueChange = {
                                paymentDue(it)
                                val res =
                                    BuildingValidator.validatePaymentDue(addBuildingFormState.paymentDue)
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
                DividerWithSubhead(subhead = "Services")
                if (buildingServices.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .verticalScroll(rememberScrollState()) // Scroll độc lập
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        buildingServices.forEach { service ->
                            ServiceCard(
                                service = service,
                                onEdit = null,
                                onDelete = null
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.
                                padding(end = 8.dp)
                                    .clickable {
                                        navController?.navigate(Screens.AddService.createRoute("","",true))
                                        //onAddServiceClick("",building?.id.toString())
                                    }
                            )
                            Text("Add service here")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                                .clickable {
                                    //onAddServiceClick(null.toString(),building?.id.toString())
                                }
                        )
                        Text("Add service here")
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
                            "name" to BuildingValidator.validateName(addBuildingFormState.name),
                            "floor" to BuildingValidator.validateFloor(addBuildingFormState.floor),
                            "address" to BuildingValidator.validateAddress(addBuildingFormState.address),
                            "billing" to BuildingValidator.validateBillingDate(addBuildingFormState.billingDate),
                            "start" to BuildingValidator.validatePaymentStart(addBuildingFormState.paymentStart),
                            "due" to BuildingValidator.validatePaymentDue(addBuildingFormState.paymentDue),
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
                            val floorInt = addBuildingFormState.floor.toInt()
                            val billingDateInt = addBuildingFormState.billingDate.toInt()
                            val paymentStartInt = addBuildingFormState.paymentStart.toInt()
                            val paymentDueInt = addBuildingFormState.paymentDue.toInt()
                            val building = Building(
                                id = "",
                                name = addBuildingFormState.name,
                                floor = floorInt,
                                address = addBuildingFormState.address,
                                status = addBuildingFormState.status,
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
