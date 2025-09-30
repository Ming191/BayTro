package com.example.baytro.view.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.baytro.data.Building
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
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
    val selectedImages = remember { mutableStateListOf<Uri>() }

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
        selectedImages.clear()
    }

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is AuthUIState.Success -> {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                navController?.popBackStack()
            }

            is AuthUIState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3),
        onResult = { uris ->
            val merged = (selectedImages.toList() + uris).distinct().take(3)
            selectedImages.clear(); selectedImages.addAll(merged)
        }
    )

    val nameFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val floorFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val addressFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val billingFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val startFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val dueFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                )
            }

            item {
                OutlinedTextField(
                    value = floor,
                    onValueChange = {
                        floor = it
                        val res = BuildingValidator.validateFloor(floor)
                        floorError = res.isError; floorErrorMsg = res.message
                    },
                    label = { Text("Floor") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = floorError,
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
                    modifier = Modifier.fillMaxWidth().focusRequester(addressFocus)
                )
            }

            item {
                OutlinedTextField(
                    value = billingDate,
                    onValueChange = {
                        billingDate = it
                        val res = BuildingValidator.validateBillingDate(billingDate)
                        billingError = res.isError; billingErrorMsg = res.message
                    },
                    label = { Text("Billing date") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = billingError,
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
                    OutlinedTextField(
                        value = paymentStart,
                        onValueChange = {
                            paymentStart = it
                            val res = BuildingValidator.validatePaymentStart(paymentStart)
                            startError = res.isError; startErrorMsg = res.message
                        },
                        label = { Text("Payment start") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        isError = startError,
                        supportingText = {
                            if (startError) Text(
                                text = startErrorMsg ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.weight(1f).focusRequester(startFocus)
                    )
                    OutlinedTextField(
                        value = paymentDue,
                        onValueChange = {
                            paymentDue = it
                            val res = BuildingValidator.validatePaymentDue(paymentDue)
                            dueError = res.isError; dueErrorMsg = res.message
                        },
                        label = { Text("Payment due") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        isError = dueError,
                        supportingText = {
                            if (dueError) Text(
                                text = dueErrorMsg ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.weight(1f).focusRequester(dueFocus)
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Images (optional)")
                        IconButton(onClick = {
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }, enabled = selectedImages.size < 3) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add images")
                        }
                    }
                    if (selectedImages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(selectedImages) { index, uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(96.dp).clip(RectangleShape)
                                    )
                                    IconButton(onClick = {
                                        selectedImages.removeAt(index)
                                    }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                                imageUrls = viewModel.building.value?.imageUrls ?: emptyList()
                            )
                            if (selectedImages.isNotEmpty()) {
                                viewModel.updateWithImages(building, selectedImages)
                            } else {
                                viewModel.update(building)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().requiredHeight(50.dp),
                    enabled = uiState !is AuthUIState.Loading
                ) {
                    if (uiState is AuthUIState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}