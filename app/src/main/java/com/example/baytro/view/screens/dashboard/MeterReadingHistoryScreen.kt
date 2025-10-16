package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.baytro.data.MeterStatus
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.viewModel.meter_reading.MeterReadingHistoryAction
import com.example.baytro.viewModel.meter_reading.MeterReadingHistoryVM
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingHistoryScreen(
    contractId: String,
    viewModel: MeterReadingHistoryVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(contractId) {
        viewModel.onAction(MeterReadingHistoryAction.LoadReadings(contractId))
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let {
                snackbarHostState.showSnackbar(message = it)
            }
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index == uiState.readings.size - 1 && !uiState.isLoadingNextPage && !uiState.allReadingsLoaded
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.onAction(MeterReadingHistoryAction.LoadNextPage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.readings.isEmpty() -> {
                    EmptyHistoryState()
                }
                else -> {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.readings,
                            key = { _, reading -> reading.id }
                        ) { index, reading ->
                            HistoryReadingCard(reading = reading)
                        }
                        item {
                            if (uiState.isLoadingNextPage) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No readings yet", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun HistoryReadingCard(reading: MeterReading) {
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
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Meter Submission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                StatusChip(status = reading.status)
            }
            Text(
                text = dateFormat.format(Date(reading.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()

            HistoryReadingSection(
                icon = Icons.Default.ElectricBolt,
                label = "Electricity",
                value = reading.electricityValue,
                consumption = reading.electricityConsumption,
                cost = reading.electricityCost
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            HistoryReadingSection(
                icon = Icons.Default.Water,
                label = "Water",
                value = reading.waterValue,
                consumption = reading.waterConsumption,
                cost = reading.waterCost
            )

            val images = listOfNotNull(
                reading.electricityImageUrl,
                reading.waterImageUrl
            )
            if (images.isNotEmpty()) {
                HorizontalDivider()
                Text("Photos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                PhotoCarousel(
                    selectedPhotos = images.map { it.toUri() },
                    onPhotosSelected = {},
                    maxSelectionCount = 2,
                    showDeleteButton = false
                )
            }
            reading.declineReason?.let { reason ->
                HorizontalDivider()
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Decline Reason:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryReadingSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    consumption: Int?,
    cost: Double?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        // Reading value and consumption
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Reading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            consumption?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Consumption", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Cost
        cost?.let {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "₫${String.format(Locale.US, "%,d", it.toLong())}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: MeterStatus) {
    val (color, textColor, text) = when (status) {
        MeterStatus.PENDING -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "Pending")
        MeterStatus.APPROVED -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "Approved")
        MeterStatus.DECLINED -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Declined")
    }
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}