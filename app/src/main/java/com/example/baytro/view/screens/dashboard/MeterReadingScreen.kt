package com.example.baytro.view.screens.dashboard

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
// DELETE THIS IMPORT: MeterType is no longer used
// import com.example.baytro.data.meter_reading.MeterType
import com.example.baytro.view.components.CameraOnlyPhotoCapture
import com.example.baytro.view.components.LoadingOverlay
import com.example.baytro.viewModel.dashboard.MeterReadingAction
import com.example.baytro.viewModel.dashboard.MeterReadingEvent
import com.example.baytro.viewModel.dashboard.MeterReadingUiState
import com.example.baytro.viewModel.dashboard.MeterReadingVM
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(
    contractId: String,
    roomId: String,
    landlordId: String,
    viewModel: MeterReadingVM = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(contractId, roomId, landlordId) {
        viewModel.initialize(contractId, roomId, landlordId)
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let {
                snackbarHostState.showSnackbar(message = it)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is MeterReadingEvent.SubmissionSuccess -> {
                    Toast.makeText(
                        context,
                        "Meter readings submitted successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    onNavigateBack()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Submit Meter Reading") },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            enabled = !uiState.isSubmitting
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            MeterReadingContent(
                uiState = uiState,
                onAction = viewModel::onAction,
                modifier = Modifier.padding(paddingValues)
            )
        }

        if (uiState.isSubmitting) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                LoadingOverlay(progress = uploadProgress)
            }
        }
    }
}

@Composable
fun MeterReadingContent(
    uiState: MeterReadingUiState,
    onAction: (MeterReadingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InstructionsCard()

        MeterPhotosSection(
            electricityPhotoUri = uiState.electricityPhotoUri,
            waterPhotoUri = uiState.waterPhotoUri,
            onAction = onAction
        )

        MeterReadingsSection(
            electricityReading = uiState.electricityReading,
            waterReading = uiState.waterReading,
            onAction = onAction
        )

        val isButtonEnabled = !uiState.isSubmitting &&
                uiState.electricityReading.isNotBlank() &&
                uiState.waterReading.isNotBlank()

        Button(
            onClick = { onAction(MeterReadingAction.SubmitReadings) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = isButtonEnabled
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Submit Readings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Please enter values for both electricity and water meters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MeterPhotosSection(
    electricityPhotoUri: Uri?,
    waterPhotoUri: Uri?,
    onAction: (MeterReadingAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Meter photos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MeterPhotoCapture(
            label = "Electricity Meter",
            selectedPhoto = electricityPhotoUri,
            onPhotoSelected = { uri -> onAction(MeterReadingAction.SelectElectricityPhoto(uri)) },
            onProcessImage = { uri, context -> onAction(MeterReadingAction.ProcessImage(uri, true, context))}
        )

        MeterPhotoCapture(
            label = "Water Meter",
            selectedPhoto = waterPhotoUri,
            onPhotoSelected = { uri -> onAction(MeterReadingAction.SelectWaterPhoto(uri)) },
            onProcessImage = { uri, context -> onAction(MeterReadingAction.ProcessImage(uri, false, context))}
        )
    }
}

@Composable
private fun MeterPhotoCapture(
    label: String,
    selectedPhoto: Uri?,
    onPhotoSelected: (Uri) -> Unit,
    onProcessImage: (Uri, Context) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CameraOnlyPhotoCapture(
                selectedPhoto = selectedPhoto,
                onPhotoConfirmed = { uri ->
                    onPhotoSelected(uri)
                    onProcessImage(uri, context)
                },
                onPhotoDeleted = { /* Handle photo deletion if needed */ }
            )
        }
    }
}


@Composable
private fun MeterReadingsSection(
    electricityReading: String,
    waterReading: String,
    onAction: (MeterReadingAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Meter Readings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = electricityReading,
                onValueChange = { onAction(MeterReadingAction.UpdateElectricityReading(it)) },
                label = { Text("Electricity (kWh)") },
                leadingIcon = { Icon(Icons.Default.ElectricBolt, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = waterReading,
                onValueChange = { onAction(MeterReadingAction.UpdateWaterReading(it)) },
                label = { Text("Water (m³)") },
                leadingIcon = { Icon(Icons.Default.Water, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}