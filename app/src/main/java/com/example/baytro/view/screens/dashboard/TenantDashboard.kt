package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.Utils
import com.example.baytro.utils.Utils.formatOrdinal
import com.example.baytro.view.components.TenantDashboardSkeleton
import com.example.baytro.viewModel.dashboard.TenantDashboardVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TenantDashboard(
    viewModel: TenantDashboardVM = koinViewModel(),
    onNavigateToEmptyContract: () -> Unit = {},
    onNavigateToContractDetails: (String) -> Unit = {},
    onNavigateToRequestList: () -> Unit = {},
    onNavigateToMeterReading: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToMeterHistory: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    Log.d("TenantDashboard", "Composable recomposed - isLoading: ${uiState.isLoading}, hasError: ${uiState.error != null}, hasContract: ${uiState.contract != null}, hasUser: ${uiState.user != null}")

    LaunchedEffect(uiState.contract, uiState.isLoading) {
        if (!uiState.isLoading && uiState.contract == null) {
            Log.d("TenantDashboard", "No active contract found, navigating to empty contract screen")
            onNavigateToEmptyContract()
        }
    }

    if (uiState.isLoading) {
        Log.d("TenantDashboard", "Showing skeleton")
        TenantDashboardSkeleton()
        return
    }

    if (uiState.error != null && uiState.contract == null) {
        Log.d("TenantDashboard", "Waiting for navigation to empty contract screen")
        return
    }

    val contract = uiState.contract
    val user = uiState.user

    if (contract != null && user != null) {
        Log.d("TenantDashboard", "Showing content for user: ${user.fullName}, contract: ${contract.id}")
        TenantDashboardContent(
            username = user.fullName,
            month = uiState.monthsStayed,
            days = uiState.daysStayed,
            status = contract.status,
            billPaymentDeadline = uiState.billPaymentDeadline,
            rentalFee = contract.rentalFee,
            deposit = contract.deposit,
            contractId = contract.id,
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
                onNavigateToMeterReading(contract.id, contract.roomId, contract.landlordId)
            },
            onMeterHistoryClick = {
                Log.d("TenantDashboard", "Navigating to meter reading history")
                onNavigateToMeterHistory(contract.id)
            }
        )
    } else {
        Log.w("TenantDashboard", "Not showing content - contract is null: ${contract == null}, user is null: ${user == null}")
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TenantDashboardContent(
    username: String,
    month: Int = 0,
    days: Int = 0,
    status: Status = Status.ACTIVE,
    billPaymentDeadline: Int = 0,
    rentalFee: Int = 0,
    deposit: Int = 0,
    contractId: String = "",
    onViewDetailsClick: (String) -> Unit = {},
    onRequestMaintenanceClick: () -> Unit = {},
    onMeterReadingClick: () -> Unit = {},
    onMeterHistoryClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("TenantDashboardContent", "LaunchedEffect triggered, setting isVisible = true")
        isVisible = true
    }

    Log.d("TenantDashboardContent", "Rendering content - isVisible: $isVisible")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {

                // Welcome Header
                item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(tween(600)) + slideInVertically(
                            initialOffsetY = { -30 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
                    ) {
                        WelcomeHeader(username)
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
                            month = month,
                            days = days,
                            status = status,
                            onViewDetailsClick = {
                                if (contractId.isNotEmpty()) {
                                    onViewDetailsClick(contractId)
                                }
                            }
                        )
                    }
                }

                // Quick Actions
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

                // Payment Info
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
                            billPaymentDeadline = billPaymentDeadline,
                            rentalFee = rentalFee,
                            deposit = deposit
                        )
                    }
                }

                // Action Buttons
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
                            onMeterHistoryClick = onMeterHistoryClick
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    )
}

@Composable
fun WelcomeHeader(username: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledTonalButton(
                    onClick = onViewDetailsClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("View Details")
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
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestMaintenanceClick() }
                    .padding(16.dp),
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Report issues or request repairs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun PaymentSection(
    billPaymentDeadline: Int,
    rentalFee: Int,
    deposit: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Payment Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Deadline
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Payment Deadline",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Every ${formatOrdinal(billPaymentDeadline)} of the month",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
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
                        modifier = Modifier.weight(1f)
                    )
                    FeeCard(
                        icon = Icons.Default.AccountBalance,
                        label = "Deposit",
                        value = Utils.formatCurrency(deposit.toString()),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Utilities
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Utilities",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    UtilityCard(
                        icon = Icons.Default.ElectricBolt,
                        label = "Electricity",
                        rate = "4.000 VND/kWH",
                        usage = "100 kWH",
                        lastUpdate = "9 Sep 2025",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                    UtilityCard(
                        icon = Icons.Default.Water,
                        label = "Water",
                        rate = "18.000 VND/m³",
                        usage = "100 m³",
                        lastUpdate = "9 Sep 2025",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                color = contentColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
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
    contentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                color = contentColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = rate,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = usage,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = lastUpdate,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun ActionButtonsSection(
    onMeterReadingClick: () -> Unit = {},
    onMeterHistoryClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = { },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Payments", fontWeight = FontWeight.SemiBold)
            }
            FilledTonalButton(
                onClick = {
                    Log.d("ActionButtonsSection", "History button clicked!")
                    onMeterHistoryClick()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("History", fontWeight = FontWeight.SemiBold)
            }
        }

        Button(
            onClick = onMeterReadingClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Submit Meter Reading",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
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