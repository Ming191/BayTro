package com.example.baytro.view.screens.building

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.baytro.data.BuildingStatus
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.viewModel.building.AddBuildingVM
import kotlinx.coroutines.delay
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
    val formState by viewModel.formState.collectAsState()
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

    var sectionTitleVisible by remember { mutableStateOf(false) }
    var nameFieldVisible by remember { mutableStateOf(false) }
    var floorFieldVisible by remember { mutableStateOf(false) }
    var addressFieldVisible by remember { mutableStateOf(false) }
    var statusFieldVisible by remember { mutableStateOf(false) }
    var paymentTitleVisible by remember { mutableStateOf(false) }
    var billingFieldVisible by remember { mutableStateOf(false) }
    var paymentFieldsVisible by remember { mutableStateOf(false) }
    var imagesTitleVisible by remember { mutableStateOf(false) }
    var imagesFieldVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    // Trigger staggered animations on screen load
    LaunchedEffect(Unit) {
        delay(50)
        sectionTitleVisible = true
        delay(80)
        nameFieldVisible = true
        delay(80)
        floorFieldVisible = true
        delay(80)
        addressFieldVisible = true
        delay(80)
        statusFieldVisible = true
        delay(100)
        paymentTitleVisible = true
        delay(80)
        billingFieldVisible = true
        delay(80)
        paymentFieldsVisible = true
        delay(100)
        imagesTitleVisible = true
        delay(80)
        imagesFieldVisible = true
        delay(80)
        buttonVisible = true
    }

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
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                AnimatedVisibility(
                    visible = sectionTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        subhead = "Building Details"
                    )
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = nameFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = formState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = "Building name",
                        isError = formState.nameError,
                        errorMessage = formState.nameErrorMsg,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { floorFocus.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = floorFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    OutlinedTextField(
                        value = formState.floor,
                        onValueChange = { viewModel.updateFloor(it) },
                        label = { Text("Floor") },
                        singleLine = true,
                        isError = formState.floorError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { addressFocus.requestFocus() }
                        ),
                        supportingText = {
                            if (formState.floorError) Text(
                                text = formState.floorErrorMsg ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(floorFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = addressFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    RequiredTextField(
                        value = formState.address,
                        onValueChange = { viewModel.updateAddress(it) },
                        label = "Address",
                        isError = formState.addressError,
                        errorMessage = formState.addressErrorMsg,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { billingFocus.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(addressFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = statusFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Column {
                        DropdownSelectField(
                            label = "Status",
                            options = BuildingStatus.entries
                                .filter { it != BuildingStatus.ARCHIVED }
                                .map { it.name },
                            selectedOption = formState.status.name,
                            onOptionSelected = { viewModel.updateStatus(BuildingStatus.valueOf(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            optionToString = { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = paymentTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Payment Schedule"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = billingFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    OutlinedTextField(
                        value = formState.billingDate,
                        onValueChange = { viewModel.updateBillingDate(it) },
                        label = { Text("Billing date") },
                        singleLine = true,
                        isError = formState.billingError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { startFocus.requestFocus() }
                        ),
                        supportingText = {
                            if (formState.billingError) Text(
                                text = formState.billingErrorMsg ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(billingFocus)
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = paymentFieldsVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = formState.paymentStart,
                                onValueChange = { viewModel.updatePaymentStart(it) },
                                label = { Text("Payment start") },
                                singleLine = true,
                                isError = formState.startError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { dueFocus.requestFocus() }
                                ),
                                supportingText = {
                                    if (formState.startError) Text(
                                        text = formState.startErrorMsg ?: "",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().focusRequester(startFocus)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = formState.paymentDue,
                                onValueChange = { viewModel.updatePaymentDue(it) },
                                label = { Text("Payment due") },
                                singleLine = true,
                                isError = formState.dueError,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { dueFocus.freeFocus() }
                                ),
                                supportingText = {
                                    if (formState.dueError) Text(
                                        text = formState.dueErrorMsg ?: "",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().focusRequester(dueFocus)
                            )
                        }
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
                AnimatedVisibility(
                    visible = imagesTitleVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    DividerWithSubhead(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp, top = 4.dp),
                        subhead = "Building Images"
                    )
                }
            }
            
            item {
                AnimatedVisibility(
                    visible = imagesFieldVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with image count
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "${formState.selectedImages.size}/3 images selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PhotoCarousel(
                            selectedPhotos = formState.selectedImages,
                            onPhotosSelected = { viewModel.updateSelectedImages(it) },
                            maxSelectionCount = 3,
                            imageWidth = 200.dp,
                            imageHeight = 150.dp,
                            aspectRatioX = 4f,
                            aspectRatioY = 3f,
                            maxResultWidth = 1080,
                            maxResultHeight = 1080
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = buttonVisible,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { -it / 4 }
                    )
                ) {
                    Button(
                        onClick = {
                            if (uiState is AuthUIState.Loading) return@Button
                            viewModel.validateAndSubmit()
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
}
