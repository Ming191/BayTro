package com.example.baytro.view.screens.dashboard

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.baytro.view.components.CameraOnlyPhotoCapture
import com.example.baytro.view.components.LoadingOverlay
import com.example.baytro.viewModel.dashboard.MeterReadingAction
import com.example.baytro.viewModel.dashboard.MeterReadingEvent
import com.example.baytro.viewModel.dashboard.MeterReadingUiState
import com.example.baytro.viewModel.dashboard.MeterReadingVM
import org.koin.compose.viewmodel.koinViewModel

// =====================================================================
//                       MAIN SCREEN COMPOSABLE
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(
    contractId: String,
    roomId: String,
    buildingId: String,
    landlordId: String,
    roomName: String,
    buildingName: String,
    viewModel: MeterReadingVM = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize ViewModel
    LaunchedEffect(contractId, roomId, buildingId, landlordId) {
        viewModel.initialize(contractId, roomId, buildingId, landlordId, roomName, buildingName)
    }

    // Error handling
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Event handling
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
                    title = { Text("Meter Reading") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) { paddingValues ->
            MeterReadingContent(
                uiState = uiState,
                onAction = viewModel::onAction,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Loading overlay
        AnimatedVisibility(
            visible = uiState.isSubmitting,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            LoadingOverlay(progress = uploadProgress)
        }
    }
}

// =====================================================================
//                       CONTENT SECTION
// =====================================================================

@Composable
fun MeterReadingContent(
    uiState: MeterReadingUiState,
    onAction: (MeterReadingAction) -> Unit,
    viewModel: MeterReadingVM,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated entrance for each section
        AnimatedSection(delayMillis = 0) {
            InstructionsBanner()
        }

        AnimatedSection(delayMillis = 100) {
            SubmissionProgressCard(uiState = uiState)
        }

        AnimatedSection(delayMillis = 200) {
            MeterPhotosSection(
                electricityPhotoUri = uiState.electricityPhotoUri,
                waterPhotoUri = uiState.waterPhotoUri,
                onAction = onAction,
                viewModel = viewModel
            )
        }

        AnimatedSection(delayMillis = 300) {
            MeterReadingsSection(
                electricityReading = uiState.electricityReading,
                waterReading = uiState.waterReading,
                electricityPhotoUri = uiState.electricityPhotoUri,
                waterPhotoUri = uiState.waterPhotoUri,
                onAction = onAction
            )
        }

        AnimatedSection(delayMillis = 400) {
            SubmitButton(
                enabled = !uiState.isSubmitting &&
                        uiState.electricityReading.isNotBlank() &&
                        uiState.waterReading.isNotBlank(),
                onClick = { onAction(MeterReadingAction.SubmitReadings) }
            )
        }
    }
}

// =====================================================================
//                       ANIMATED SECTION WRAPPER
// =====================================================================

@Composable
private fun AnimatedSection(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
            initialOffsetY = { it / 3 }
        )
    ) {
        content()
    }
}

// =====================================================================
//                       INSTRUCTIONS BANNER
// =====================================================================

@Composable
private fun InstructionsBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing icon animation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(scale)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Important",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Take clear photos of both meters and enter accurate readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// =====================================================================
//                       PROGRESS CARD
// =====================================================================

@Composable
private fun SubmissionProgressCard(uiState: MeterReadingUiState) {
    val hasElectricityPhoto = uiState.electricityPhotoUri != null
    val hasWaterPhoto = uiState.waterPhotoUri != null
    val hasElectricityReading = uiState.electricityReading.isNotBlank()
    val hasWaterReading = uiState.waterReading.isNotBlank()

    val completedSteps = listOf(
        hasElectricityPhoto,
        hasWaterPhoto,
        hasElectricityReading,
        hasWaterReading
    ).count { it }

    val totalSteps = 4
    val targetProgress = completedSteps.toFloat() / totalSteps

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Submission Progress",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                AnimatedContent(
                    targetState = completedSteps,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { -it } + fadeIn()).togetherWith(
                                slideOutVertically { it } + fadeOut()
                            )
                        } else {
                            (slideInVertically { it } + fadeIn()).togetherWith(
                                slideOutVertically { -it } + fadeOut()
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "stepCounter"
                ) { steps ->
                    Text(
                        text = "$steps / $totalSteps",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressCheckItem(
                    label = "Electricity photo",
                    completed = hasElectricityPhoto,
                    modifier = Modifier.weight(1f)
                )
                ProgressCheckItem(
                    label = "Water photo",
                    completed = hasWaterPhoto,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgressCheckItem(
                    label = "Electricity value",
                    completed = hasElectricityReading,
                    modifier = Modifier.weight(1f)
                )
                ProgressCheckItem(
                    label = "Water value",
                    completed = hasWaterReading,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressCheckItem(
    label: String,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    // Animate completion state
    val scale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    Row(
        modifier = modifier.scale(scale),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = completed,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn() togetherWith scaleOut() + fadeOut()
            },
            label = "checkIcon"
        ) { isCompleted ->
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (completed)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

// =====================================================================
//                       METER PHOTOS SECTION
// =====================================================================

@Composable
private fun MeterPhotosSection(
    electricityPhotoUri: Uri?,
    waterPhotoUri: Uri?,
    onAction: (MeterReadingAction) -> Unit,
    viewModel: MeterReadingVM
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meter Photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Required",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        MeterPhotoCapture(
            icon = Icons.Default.ElectricBolt,
            label = "Electricity Meter",
            selectedPhoto = electricityPhotoUri,
            onPhotoSelected = { uri -> onAction(MeterReadingAction.SelectElectricityPhoto(uri)) },
            onProcessImage = { uri, context -> onAction(MeterReadingAction.ProcessImage(uri, true, context)) },
            onPhotoDeleted = { viewModel.deleteElectricityPhoto() }
        )

        MeterPhotoCapture(
            icon = Icons.Default.Water,
            label = "Water Meter",
            selectedPhoto = waterPhotoUri,
            onPhotoSelected = { uri -> onAction(MeterReadingAction.SelectWaterPhoto(uri)) },
            onProcessImage = { uri, context -> onAction(MeterReadingAction.ProcessImage(uri, false, context)) },
            onPhotoDeleted = { viewModel.deleteWaterPhoto() }
        )
    }
}

@Composable
private fun MeterPhotoCapture(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedPhoto: Uri?,
    onPhotoSelected: (Uri) -> Unit,
    onProcessImage: (Uri, Context) -> Unit,
    onPhotoDeleted: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                AnimatedVisibility(
                    visible = selectedPhoto != null,
                    enter = scaleIn(
                        initialScale = 0.3f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Photo added",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            CameraOnlyPhotoCapture(
                selectedPhoto = selectedPhoto,
                onPhotoConfirmed = { uri ->
                    onPhotoSelected(uri)
                    onProcessImage(uri, context)
                },
                onPhotoDeleted = onPhotoDeleted
            )
        }
    }
}

// =====================================================================
//                       METER READINGS SECTION
// =====================================================================

@Composable
private fun MeterReadingsSection(
    electricityReading: String,
    waterReading: String,
    electricityPhotoUri: Uri?,
    waterPhotoUri: Uri?,
    onAction: (MeterReadingAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meter Values",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "Required",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        MeterReadingInput(
            icon = Icons.Default.ElectricBolt,
            label = "Electricity",
            value = electricityReading,
            unit = "kWh",
            hasPhoto = electricityPhotoUri != null,
            onValueChange = { onAction(MeterReadingAction.UpdateElectricityReading(it)) }
        )

        MeterReadingInput(
            icon = Icons.Default.Water,
            label = "Water",
            value = waterReading,
            unit = "m³",
            hasPhoto = waterPhotoUri != null,
            onValueChange = { onAction(MeterReadingAction.UpdateWaterReading(it)) }
        )
    }
}

@Composable
private fun MeterReadingInput(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String,
    hasPhoto: Boolean,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(
                    visible = !hasPhoto,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Enter $label reading") },
                suffix = { Text(unit, style = MaterialTheme.typography.bodyMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }
    }
}

// =====================================================================
//                       SUBMIT BUTTON
// =====================================================================

@Composable
private fun SubmitButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    // Pulse animation when enabled
    val infiniteTransition = rememberInfiniteTransition(label = "button")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(if (enabled) buttonScale else 1f),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Submit Readings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}