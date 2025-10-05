package com.example.baytro.view.screens.building

import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.app.Activity
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.baytro.data.Building
import com.example.baytro.utils.BuildingValidator
import com.example.baytro.view.AuthUIState
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.FullScreenImageViewer
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
    val existingImages = remember { mutableStateListOf<String>() }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }
    
    // Image viewer state
    var showImageViewer by remember { mutableStateOf(false) }
    var imageViewerIndex by remember { mutableIntStateOf(0) }
    
    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedExistingImages = remember { mutableStateListOf<Int>() }
    val selectedNewImages = remember { mutableStateListOf<Int>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
        existingImages.clear()
        existingImages.addAll(b.imageUrls)
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

    // UCrop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val croppedUri = UCrop.getOutput(intent)
                croppedUri?.let { uri ->
                    if (selectedImages.size + existingImages.size < 3) {
                        selectedImages.add(uri)
                    }
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                if (selectedImages.size + existingImages.size < 3) {
                    val destinationUri = Uri.fromFile(
                        File(context.cacheDir, "building_camera_cropped_${System.currentTimeMillis()}.jpg")
                    )
                    
                    val uCropIntent = UCrop.of(uri, destinationUri)
                        .withAspectRatio(16f, 9f)
                        .withMaxResultSize(1080, 608)
                        .getIntent(context)
                    
                    cropLauncher.launch(uCropIntent)
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            if (selectedImages.size + existingImages.size < 3) {
                val destinationUri = Uri.fromFile(
                    File(context.cacheDir, "building_edit_cropped_${System.currentTimeMillis()}.jpg")
                )

                val uCropIntent = UCrop.of(selectedUri, destinationUri)
                    .withAspectRatio(16f, 9f) // Tỷ lệ 16:9 cho ảnh building
                    .withMaxResultSize(1080, 608) // Max size tương ứng
                    .getIntent(context)

                cropLauncher.launch(uCropIntent)
            }
        }
    }

    val nameFocus = remember { FocusRequester() }
    val floorFocus = remember { FocusRequester() }
    val addressFocus = remember { FocusRequester() }
    val billingFocus = remember { FocusRequester() }
    val startFocus = remember { FocusRequester() }
    val dueFocus = remember { FocusRequester() }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            try {
                val imageFile = File(context.cacheDir, "building_camera_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )
                cameraImageUri.value = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera not available: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

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
                    onValueChange = {
                        floor = it
                        val res = BuildingValidator.validateFloor(floor)
                        floorError = res.isError; floorErrorMsg = res.message
                    },
                    label = { Text("Floor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { addressFocus.requestFocus() }
                    ),
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { billingFocus.requestFocus() }
                    ),
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { startFocus.requestFocus() }
                    ),
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { dueFocus.requestFocus() }
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { dueFocus.freeFocus() }
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
                    // Header with image count and multi-select controls
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = if (isMultiSelectMode) "Select Images to Delete" else "Building Images",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isMultiSelectMode) 
                                    "${selectedExistingImages.size + selectedNewImages.size} selected" 
                                else 
                                    "${selectedImages.size + existingImages.size}/3 images",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isMultiSelectMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Delete selected button
                                if (selectedExistingImages.isNotEmpty() || selectedNewImages.isNotEmpty()) {
                                    IconButton(
                                        onClick = { showDeleteConfirmDialog = true }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete selected",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                // Cancel multi-select
                                TextButton(
                                    onClick = {
                                        isMultiSelectMode = false
                                        selectedExistingImages.clear()
                                        selectedNewImages.clear()
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                    
                    // Image action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        // Gallery button
                        OutlinedButton(
                            onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = selectedImages.size + existingImages.size < 3,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gallery")
                        }
                        
                        // Camera button
                        OutlinedButton(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            enabled = selectedImages.size + existingImages.size < 3,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Camera")
                        }
                    }
                    // Image grid with clickable images
                    if (existingImages.isNotEmpty() || selectedImages.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Existing images
                            itemsIndexed(existingImages) { index, imageUrl ->
                                Card(
                                    modifier = Modifier.size(120.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .combinedClickable(
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        // Toggle selection
                                                        if (selectedExistingImages.contains(index)) {
                                                            selectedExistingImages.remove(index)
                                                        } else {
                                                            selectedExistingImages.add(index)
                                                        }
                                                    } else {
                                                        // Open image viewer
                                                        imageViewerIndex = index
                                                        showImageViewer = true
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isMultiSelectMode) {
                                                        isMultiSelectMode = true
                                                        selectedExistingImages.add(index)
                                                    }
                                                }
                                            )
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        // Selection indicator or delete button
                                        if (isMultiSelectMode) {
                                            // Selection indicator
                                            Icon(
                                                imageVector = if (selectedExistingImages.contains(index)) 
                                                    Icons.Default.CheckCircle 
                                                else 
                                                    Icons.Default.RadioButtonUnchecked,
                                                contentDescription = if (selectedExistingImages.contains(index)) 
                                                    "Selected" 
                                                else 
                                                    "Not selected",
                                                tint = if (selectedExistingImages.contains(index)) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                            )
                                        } else {
                                            // Delete button in top-left corner
                                            IconButton(
                                                onClick = {
                                                    existingImages.removeAt(index)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.6f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove existing image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // New selected images
                            itemsIndexed(selectedImages) { index, uri ->
                                Card(
                                    modifier = Modifier.size(120.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .combinedClickable(
                                                onClick = {
                                                    if (isMultiSelectMode) {
                                                        // Toggle selection
                                                        if (selectedNewImages.contains(index)) {
                                                            selectedNewImages.remove(index)
                                                        } else {
                                                            selectedNewImages.add(index)
                                                        }
                                                    } else {
                                                        // Open image viewer
                                                        imageViewerIndex = existingImages.size + index
                                                        showImageViewer = true
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isMultiSelectMode) {
                                                        isMultiSelectMode = true
                                                        selectedNewImages.add(index)
                                                    }
                                                }
                                            )
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        // Selection indicator or delete button
                                        if (isMultiSelectMode) {
                                            // Selection indicator
                                            Icon(
                                                imageVector = if (selectedNewImages.contains(index)) 
                                                    Icons.Default.CheckCircle 
                                                else 
                                                    Icons.Default.RadioButtonUnchecked,
                                                contentDescription = if (selectedNewImages.contains(index)) 
                                                    "Selected" 
                                                else 
                                                    "Not selected",
                                                tint = if (selectedNewImages.contains(index)) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                            )
                                        } else {
                                            // Delete button in top-left corner
                                            IconButton(
                                                onClick = {
                                                    selectedImages.removeAt(index)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .size(32.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.6f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove new image",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state for images
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Add building photos",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                imageUrls = existingImages.toList()
                            )
                            
                            Log.d("EditBuildingScreen", "Existing images: ${existingImages.size}, New images: ${selectedImages.size}")
                            
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
    
    // Image Viewer
    if (showImageViewer) {
        val allImages = existingImages.toList() + selectedImages.toList()
        FullScreenImageViewer(
            images = allImages,
            initialIndex = imageViewerIndex,
            onDismiss = { showImageViewer = false }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Images") },
            text = { 
                Text("Are you sure you want to delete ${selectedExistingImages.size + selectedNewImages.size} selected image(s)?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Delete selected existing images (in reverse order to maintain indices)
                        selectedExistingImages.sortedDescending().forEach { index ->
                            if (index < existingImages.size) {
                                existingImages.removeAt(index)
                            }
                        }
                        
                        // Delete selected new images (in reverse order to maintain indices)
                        selectedNewImages.sortedDescending().forEach { index ->
                            if (index < selectedImages.size) {
                                selectedImages.removeAt(index)
                            }
                        }
                        
                        // Reset multi-select mode
                        isMultiSelectMode = false
                        selectedExistingImages.clear()
                        selectedNewImages.clear()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}