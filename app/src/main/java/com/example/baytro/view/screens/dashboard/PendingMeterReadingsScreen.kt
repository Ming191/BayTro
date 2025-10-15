package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.viewModel.meter_reading.PendingMeterReadingsAction
import com.example.baytro.viewModel.meter_reading.PendingMeterReadingsVM
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingMeterReadingsScreen(
    viewModel: PendingMeterReadingsVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeclineDialog by remember { mutableStateOf<MeterReading?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // --- LaunchedEffects remain the same ---
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let {
                snackbarHostState.showSnackbar(message = it)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.pendingReadings.isEmpty() -> {
                    Column(
                        modifier = Modifier.padding(16.dp).align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No pending readings",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.pendingReadings,
                            key = { _, reading -> reading.id }
                        ) { index, reading ->
                            val isBeingDismissed = uiState.dismissingReadingIds.contains(reading.id)

                            AnimatedVisibility(
                                visible = !isBeingDismissed,
                                enter = fadeIn(tween(600, delayMillis = index * 100)) +
                                        slideInVertically(
                                            initialOffsetY = { 40 },
                                            animationSpec = tween(600, delayMillis = index * 100, easing = FastOutSlowInEasing)
                                        ),
                                exit = fadeOut(tween(400)) + slideOutVertically(targetOffsetY = { -40 }) + shrinkVertically(tween(400))
                            ) {
                                PendingReadingCard(
                                    reading = reading,
                                    isProcessing = uiState.processingReadingIds.contains(reading.id),
                                    onApprove = {
                                        viewModel.onAction(PendingMeterReadingsAction.ApproveReading(reading.id))
                                    },
                                    onDecline = {
                                        showDeclineDialog = reading
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        showDeclineDialog?.let { reading ->
            DeclineReasonDialog(
                onDismiss = { showDeclineDialog = null },
                onConfirm = { reason ->
                    viewModel.onAction(PendingMeterReadingsAction.DeclineReading(reading.id, reason))
                    showDeclineDialog = null
                }
            )
        }
    }
}

@Composable
fun PendingReadingCard(
    reading: MeterReading,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Meter Reading Submission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Pending",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = dateFormat.format(Date(reading.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Reading Sections
            PendingReadingSection(
                icon = Icons.Default.ElectricBolt,
                label = "Electricity",
                value = reading.electricityValue
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            PendingReadingSection(
                icon = Icons.Default.Water,
                label = "Water",
                value = reading.waterValue
            )

            // Photo Carousel
            val images = listOfNotNull(
                reading.electricityImageUrl,
                reading.waterImageUrl
            )
            if (images.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                PhotoCarousel(
                    selectedPhotos = images.map { it.toUri() },
                    onPhotosSelected = {},
                    maxSelectionCount = 2,
                    showDeleteButton = false
                )
            }

            HorizontalDivider()

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decline")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
fun PendingReadingSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int
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
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeclineReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decline Reading") },
        text = {
            Column {
                Text("Please provide a reason for declining:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Enter reason...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isConfirming = true
                    onConfirm(reason)
                },
                enabled = reason.isNotBlank() && !isConfirming
            ) {
                Text("Decline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
