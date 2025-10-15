package com.example.baytro.view.screens.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baytro.data.meter_reading.MeterType
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
                onAction = { action ->
                    when (action) {
                        is MeterReadingAction.ProcessImage -> viewModel.onAction(
                            MeterReadingAction.ProcessImage(action.meterType, action.uri, context)
                        )
                        else -> viewModel.onAction(action)
                    }
                },
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
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(
                initialOffsetY = { -30 },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        ) {
            InstructionsCard()
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 100)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(600, delayMillis = 100, easing = FastOutSlowInEasing)
                    )
        ) {
            MeterPhotosSection(
                selectedPhotos = uiState.selectedPhotos,
                onAction = onAction
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 200)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                    )
        ) {
            MeterReadingsSection(
                readings = uiState.readings,
                onAction = onAction
            )
        }

        val isButtonEnabled = !uiState.isProcessing &&
                uiState.selectedPhotos.isNotEmpty() &&
                uiState.readings.any { it.value.isNotEmpty() }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 300)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(600, delayMillis = 300, easing = FastOutSlowInEasing)
                    )
        ) {
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
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
                text = "Please capture photos of both electricity and water meters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MeterPhotosSection(
    selectedPhotos: Map<MeterType, Uri>,
    onAction: (MeterReadingAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Meter Photos (Required)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MeterPhotoCapture(
            meterType = MeterType.ELECTRICITY,
            label = "Electricity Meter",
            selectedPhoto = selectedPhotos[MeterType.ELECTRICITY],
            onAction = onAction
        )

        MeterPhotoCapture(
            meterType = MeterType.WATER,
            label = "Water Meter",
            selectedPhoto = selectedPhotos[MeterType.WATER],
            onAction = onAction
        )
    }
}

@Composable
private fun MeterPhotoCapture(
    meterType: MeterType,
    label: String,
    selectedPhoto: Uri?,
    onAction: (MeterReadingAction) -> Unit
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
                    onAction(MeterReadingAction.SelectPhoto(meterType, uri))
                    onAction(MeterReadingAction.ProcessImage(meterType, uri, context))
                },
                onPhotoDeleted = {
                    // TODO: Handle photo deletion if needed
                }
            )
        }
    }
}

@Composable
private fun MeterReadingsSection(
    readings: Map<MeterType, String>,
    onAction: (MeterReadingAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
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
                value = readings[MeterType.ELECTRICITY] ?: "",
                onValueChange = { onAction(MeterReadingAction.UpdateReading(MeterType.ELECTRICITY, it)) },
                label = { Text("Electricity (kWh)") },
                leadingIcon = {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = readings[MeterType.WATER] ?: "",
                onValueChange = { onAction(MeterReadingAction.UpdateReading(MeterType.WATER, it)) },
                label = { Text("Water (m³)") },
                leadingIcon = {
                    Icon(Icons.Default.Water, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
