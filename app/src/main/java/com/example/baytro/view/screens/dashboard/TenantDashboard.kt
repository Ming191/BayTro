package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.baytro.data.Building
import com.example.baytro.data.contract.Status
import com.example.baytro.data.meter_reading.MeterReading
import com.example.baytro.data.service.Metric
import com.example.baytro.data.service.Service
import com.example.baytro.utils.Utils
import com.example.baytro.utils.Utils.formatOrdinal
import com.example.baytro.view.components.TenantDashboardSkeleton
import com.example.baytro.viewModel.dashboard.TenantDashboardEvent
import com.example.baytro.viewModel.dashboard.TenantDashboardUiState
import com.example.baytro.viewModel.dashboard.TenantDashboardVM
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDashboard(
    viewModel: TenantDashboardVM = koinViewModel(),
    onNavigateToEmptyContract: () -> Unit = {},
    onNavigateToContractDetails: (String) -> Unit = {},
    onNavigateToRequestList: () -> Unit = {},
    onNavigateToMeterReading: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToMeterHistory: (String) -> Unit = {},
    onNavigateToPayment: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
                is TenantDashboardEvent.NavigateToEmptyContract -> {
                    Log.d("TenantDashboard", "Navigating to empty contract screen")
                    onNavigateToEmptyContract()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        if (uiState.isLoading) {
            TenantDashboardSkeleton()
        } else if (uiState.contract != null && uiState.user != null) {
            TenantDashboardContent(
                uiState = uiState,
                onViewDetailsClick = { contractId ->
                    Log.d("TenantDashboard", "Navigating to contract details: $contractId")
                    onNavigateToContractDetails(contractId)
                },
                onRequestMaintenanceClick = {
                    Log.d("TenantDashboard", "Navigating to maintenance request list")
                    onNavigateToRequestList()
                },
                onMeterReadingClick = {
                    Log.d("TenantDashboard", "Navigating to meter reading screen")
                    onNavigateToMeterReading(
                        uiState.contract!!.id,
                        uiState.contract!!.roomId,
                        uiState.contract!!.buildingId,
                        uiState.contract!!.landlordId,
                        uiState.room?.roomNumber ?: "N/A",
                        uiState.building?.name ?: "N/A"
                    )
                },
                onMeterHistoryClick = {
                    Log.d("TenantDashboard", "Navigating to meter reading history")
                    onNavigateToMeterHistory(uiState.contract!!.id)
                },
                onNavigateToPayment = {
                    Log.d("TenantDashboard", "Navigating to payment screen")
                    onNavigateToPayment()
                }
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TenantDashboardContent(
    uiState: TenantDashboardUiState,
    modifier: Modifier = Modifier,
    onViewDetailsClick: (String) -> Unit = {},
    onRequestMaintenanceClick: () -> Unit = {},
    onMeterReadingClick: () -> Unit = {},
    onMeterHistoryClick: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { -30 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                WelcomeHeader(uiState.user?.fullName ?: "")
            }
        }

        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 100)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                ContractStatusCard(
                    month = uiState.monthsStayed,
                    days = uiState.daysStayed,
                    status = uiState.contract?.status ?: Status.PENDING,
                    onViewDetailsClick = {
                        if (uiState.contract != null) {
                            onViewDetailsClick(uiState.contract.id)
                        }
                    }
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                QuickActionsSection(onRequestMaintenanceClick = onRequestMaintenanceClick)
            }
        }

        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 300)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                PaymentSection(
                    billPaymentDeadline = uiState.building?.paymentDue ?: 5,
                    rentalFee = uiState.contract?.rentalFee ?: 0,
                    deposit = uiState.contract?.deposit ?: 0,
                    lastReading = uiState.lastApprovedReading,
                    building = uiState.building,
                    fixedServices = uiState.fixedServices
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 400)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                ActionButtonsSection(
                    onMeterReadingClick = onMeterReadingClick,
                    onMeterHistoryClick = onMeterHistoryClick,
                    onNavigateToPayment = onNavigateToPayment
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun WelcomeHeader(username: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = username,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ContractStatusCard(
    month: Int,
    days: Int,
    status: Status,
    onViewDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Contract Status",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = statusColor(status),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = status.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            if (month > 0) {
                                append("$month month${if (month != 1) "s" else ""}")
                            }
                            if (month > 0 && days > 0) {
                                append(", ")
                            }
                            if (days > 0 || month == 0) {
                                append("$days day${if (days != 1) "s" else ""}")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                FilledTonalButton(
                    onClick = onViewDetailsClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("View Details", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onRequestMaintenanceClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestMaintenanceClick() }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Request Maintenance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Report issues or request repairs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PaymentSection(
    billPaymentDeadline: Int,
    rentalFee: Int,
    deposit: Int,
    lastReading: MeterReading? = null,
    building: Building? = null,
    fixedServices: List<Service> = emptyList()
) {
    // Debug logging
    Log.d("PaymentSection", "Building: $building")
    Log.d("PaymentSection", "Services: ${building?.services}")
    Log.d("PaymentSection", "Services size: ${building?.services?.size}")
    Log.d("PaymentSection", "Fixed Services: $fixedServices")
    Log.d("PaymentSection", "Fixed Services size: ${fixedServices.size}")
    fixedServices.forEachIndexed { index, service ->
        Log.d("PaymentSection", "Fixed Service $index: name=${service.name}, price=${service.price}, metric=${service.metric}")
    }

    val electricityService = building?.services?.find {
        Log.d("PaymentSection", "Checking service: ${it.name}, metric: ${it.metric}, price: ${it.price}")
        it.metric == Metric.KWH
    }

    val waterService = building?.services?.find {
        it.metric == Metric.M3
    }

    Log.d("PaymentSection", "Electricity service: $electricityService")
    Log.d("PaymentSection", "Water service: $waterService")

    val electricityRate = electricityService?.price?: 0
    val waterRate = waterService?.price?: 0

    Log.d("PaymentSection", "Electricity rate: $electricityRate")
    Log.d("PaymentSection", "Water rate: $waterRate")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Payment Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Deadline
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payment Deadline",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Every ${formatOrdinal(billPaymentDeadline)} of the month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Fee cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeeCard(
                        icon = Icons.Default.House,
                        label = "Rental Fee",
                        value = Utils.formatCurrency(rentalFee.toString()),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconBackgroundColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    FeeCard(
                        icon = Icons.Default.AccountBalance,
                        label = "Deposit",
                        value = Utils.formatCurrency(deposit.toString()),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Utilities
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Utilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    UtilityCard(
                        icon = Icons.Default.ElectricBolt,
                        label = "Electricity",
                        rate = "${Utils.formatCurrency(electricityRate.toString())}/kWh",
                        usage = "${lastReading?.electricityConsumption ?: 0} kWh",
                        lastUpdate = lastReading?.createdAt?.let { Utils.formatTimestamp(it) } ?: "N/A",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        iconColor = MaterialTheme.colorScheme.tertiary
                    )
                    UtilityCard(
                        icon = Icons.Default.Water,
                        label = "Water",
                        rate = "${Utils.formatCurrency(waterRate.toString())}/m³",
                        usage = "${lastReading?.waterConsumption ?: 0} m³",
                        lastUpdate = lastReading?.createdAt?.let { Utils.formatTimestamp(it) } ?: "N/A",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        iconColor = MaterialTheme.colorScheme.secondary
                    )

                    fixedServices.forEach { service ->
                        FixedServiceCard(
                            icon = Icons.Default.Build,
                            label = service.name,
                            rate = Utils.formatCurrency(service.price.toString()),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeeCard(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    iconBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                color = iconBackgroundColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun UtilityCard(
    icon: ImageVector,
    label: String,
    rate: String,
    usage: String,
    lastUpdate: String,
    containerColor: Color,
    contentColor: Color,
    iconColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    color = iconColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = rate,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = usage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = lastUpdate,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun FixedServiceCard(
    icon: ImageVector,
    label: String,
    rate: String,
    containerColor: Color,
    contentColor: Color,
    iconColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    color = iconColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = "Extra service",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = rate,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun ActionButtonsSection(
    onMeterReadingClick: () -> Unit = {},
    onMeterHistoryClick: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onNavigateToPayment,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Payments",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            FilledTonalButton(
                onClick = {
                    Log.d("ActionButtonsSection", "History button clicked!")
                    onMeterHistoryClick()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "History",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Button(
            onClick = onMeterReadingClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(vertical = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Submit Meter Reading",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun statusColor(status: Status): Color {
    return when (status) {
        Status.ACTIVE -> MaterialTheme.colorScheme.primary
        Status.OVERDUE -> MaterialTheme.colorScheme.error
        Status.PENDING -> MaterialTheme.colorScheme.tertiary
        Status.ENDED -> MaterialTheme.colorScheme.outline
    }
}