package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.baytro.data.MeterStatus
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.view.components.PhotoCarousel
import com.example.baytro.viewModel.meter_reading.MeterReadingHistoryVM
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingHistoryScreen(
    contractId: String,
    viewModel: MeterReadingHistoryVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(contractId) {
        viewModel.loadReadings(contractId)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold{
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                uiState.readings.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No readings yet",
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
                        items(uiState.groupedReadings) { group ->
                            MergedReadingCard(group = group)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MergedReadingCard(group: com.example.baytro.viewModel.meter_reading.MeterReadingGroup) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Meter Reading",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = dateFormat.format(Date(group.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            group.electricityReading?.let { reading ->
                ReadingSection(
                    icon = Icons.Default.ElectricBolt,
                    label = "Electricity",
                    reading = reading
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            group.waterReading?.let { reading ->
                ReadingSection(
                    icon = Icons.Default.Water,
                    label = "Water",
                    reading = reading
                )
            }
            val images = listOfNotNull(
                group.electricityReading?.imageUrl,
                group.waterReading?.imageUrl
            )

            if (images.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Photos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val imageUris = images.mapNotNull { imageUrl ->
                try {
                        imageUrl.toUri()
                    } catch (_: Exception) {
                        null
                    }
                }

                PhotoCarousel(
                    selectedPhotos = imageUris,
                    onPhotosSelected = { /* Read-only, do nothing */ },
                    maxSelectionCount = 2,
                    showDeleteButton = false // Hide delete buttons
                )
            }
        }
    }
}

@Composable
fun ReadingSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    reading: MeterReading
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with icon and status
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
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusChip(status = reading.status)
        }

        // Reading value and consumption
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Reading",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${reading.value}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            reading.consumption?.let { consumption ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Consumption",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$consumption",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Cost if approved
        reading.cost?.let { cost ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cost:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "₫${String.format(Locale.US, "%,d", cost.toLong())}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Decline reason if declined
        reading.declineReason?.let { reason ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Decline Reason:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: MeterStatus) {
    val (color, text) = when (status) {
        MeterStatus.METER_PENDING -> MaterialTheme.colorScheme.secondaryContainer to "Pending"
        MeterStatus.METER_APPROVED -> MaterialTheme.colorScheme.primaryContainer to "Approved"
        MeterStatus.METER_DECLINED -> MaterialTheme.colorScheme.errorContainer to "Declined"
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
