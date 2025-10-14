package com.example.baytro.view.screens.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Error
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
import com.example.baytro.view.components.CameraOnlyPhotoCapture
import com.example.baytro.view.components.LoadingOverlay
import com.example.baytro.viewModel.dashboard.MeterReadingVM
import com.example.baytro.viewModel.dashboard.MeterType
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

    var isNavigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(contractId, roomId, landlordId) {
        viewModel.initialize(contractId, roomId, landlordId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Submit Meter Reading") },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            enabled = !uiState.isSubmitting && !isNavigatingBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            MeterReadingContent(
                uiState = uiState,
                onElectricityReadingChange = { viewModel.updateElectricityReading(it) },
                onWaterReadingChange = { viewModel.updateWaterReading(it) },
                onPhotosSelected = { photos -> viewModel.setSelectedPhotos(photos) },
                onElectricityPhotoCapture = { uri ->
                    viewModel.processImageFromUri(uri, MeterType.ELECTRICITY, context)
                },
                onWaterPhotoCapture = { uri ->
                    viewModel.processImageFromUri(uri, MeterType.WATER, context)
                },
                onSubmit = {
                    isNavigatingBack = true
                    viewModel.submitReadings(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Meter readings submitted successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            onNavigateBack()
                        }
                    )
                },
                onDismissError = { viewModel.clearError() },
                modifier = Modifier.padding(paddingValues)
            )
        }

        if (uiState.isSubmitting || isNavigatingBack) {
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
    uiState: com.example.baytro.viewModel.dashboard.MeterReadingUiState,
    onElectricityReadingChange: (String) -> Unit,
    onWaterReadingChange: (String) -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
    onElectricityPhotoCapture: (Uri) -> Unit,
    onWaterPhotoCapture: (Uri) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
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
            visible = uiState.error != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(onClick = onDismissError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Instructions Card
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600)) + slideInVertically(
                initialOffsetY = { -30 },
                animationSpec = tween(600)
            )
        ) {
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

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 100)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(600)
            )
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

                // Electricity Meter Photo
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Electricity Meter",
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
                            selectedPhoto = uiState.selectedPhotos.getOrNull(0),
                            onPhotoConfirmed = { uri ->
                                val newPhotos = uiState.selectedPhotos.toMutableList()
                                if (newPhotos.isEmpty()) {
                                    newPhotos.add(uri)
                                } else {
                                    newPhotos[0] = uri
                                }
                                onPhotosSelected(newPhotos)
                                onElectricityPhotoCapture(uri) // Notify parent composable
                            },
                            onPhotoDeleted = {
                                val newPhotos = uiState.selectedPhotos.toMutableList()
                                if (newPhotos.isNotEmpty()) {
                                    newPhotos.removeAt(0)
                                }
                                onPhotosSelected(newPhotos)
                            }
                        )
                    }
                }

                // Water Meter Photo
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Water Meter",
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
                            selectedPhoto = uiState.selectedPhotos.getOrNull(1),
                            onPhotoConfirmed = { uri ->
                                val newPhotos = uiState.selectedPhotos.toMutableList()
                                while (newPhotos.isEmpty()) {
                                    newPhotos.add(Uri.EMPTY)
                                }
                                if (newPhotos.size < 2) {
                                    newPhotos.add(uri)
                                } else {
                                    newPhotos[1] = uri
                                }
                                onPhotosSelected(newPhotos.filter { it != Uri.EMPTY })
                                onWaterPhotoCapture(uri) // Notify parent composable
                            },
                            onPhotoDeleted = {
                                val newPhotos = uiState.selectedPhotos.toMutableList()
                                if (newPhotos.size > 1) {
                                    newPhotos.removeAt(1)
                                }
                                onPhotosSelected(newPhotos)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(600)
            )
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
                        value = uiState.electricityReading,
                        onValueChange = onElectricityReadingChange,
                        label = { Text("Electricity (kWh)") },
                        leadingIcon = {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = uiState.waterReading,
                        onValueChange = onWaterReadingChange,
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

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(600, delayMillis = 300)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(600)
            )
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isProcessing &&
                        uiState.selectedPhotos.isNotEmpty() &&
                        (uiState.electricityReading.isNotEmpty() || uiState.waterReading.isNotEmpty())
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
