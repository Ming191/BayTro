package com.example.baytro.view.screens.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.LandlordDashboardSkeleton
import com.example.baytro.viewModel.dashboard.LandlordDashboardUiState
import com.example.baytro.viewModel.dashboard.LandlordDashboardVM
import com.example.baytro.viewModel.dashboard.UiRevenueDataPoint
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboard(
    viewModel: LandlordDashboardVM = koinViewModel(),
    onNavigateToPendingReadings: () -> Unit = {},
    onNavigateToTenantList: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToBuildings: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToImportBuildingRoom: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading && uiState.username.isBlank()) {
        LandlordDashboardSkeleton()
    } else {
        LandlordDashboardContent(
            uiState = uiState,
            onNavigateToPendingReadings = onNavigateToPendingReadings,
            onNavigateToTenantList = onNavigateToTenantList,
            onNavigateToBuildings = onNavigateToBuildings,
            onNavigateToBills = onNavigateToBills,
            onNavigateToMaintenance = onNavigateToMaintenance,
            onNavigateToImportBuildingRoom = onNavigateToImportBuildingRoom,
            onRefresh = { viewModel.refresh() }
        )
    }
}

@Composable
fun QuickActionsSection(
    pendingReadingsCount: Int,
    newJoinRequestsCount: Int,
    overdueBillsCount: Int,
    totalBuildings: Int,
    totalTenants: Int,
    onNavigateToPendingReadings: () -> Unit,
    onNavigateToTenantList: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBuildings: () -> Unit
) {
    val onPendingReadingsClick = remember { onNavigateToPendingReadings }
    val onTenantListClick = remember { onNavigateToTenantList }
    val onBillsClick = remember { onNavigateToBills }
    val onBuildingsClick = remember { onNavigateToBuildings }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics(mergeDescendants = false) {
            contentDescription = "Quick Actions Section"
        }
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (pendingReadingsCount > 0 || newJoinRequestsCount > 0 || overdueBillsCount > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendingReadingsCount > 0) {
                    ActionCard(
                        icon = Icons.Default.ElectricBolt,
                        title = "Pending Meter Readings",
                        count = pendingReadingsCount,
                        description = "Review and approve readings",
                        color = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = onPendingReadingsClick
                    )
                }

                if (newJoinRequestsCount > 0) {
                    ActionCard(
                        icon = Icons.Default.PersonAdd,
                        title = "New Join Requests",
                        count = newJoinRequestsCount,
                        description = "Review tenant applications",
                        color = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = onTenantListClick
                    )
                }

                if (overdueBillsCount > 0) {
                    ActionCard(
                        icon = Icons.Default.Receipt,
                        title = "Overdue Bills",
                        count = overdueBillsCount,
                        description = "Follow up on payments",
                        color = MaterialTheme.colorScheme.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = onBillsClick
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Home,
                label = "Buildings",
                count = totalBuildings,
                onClick = onBuildingsClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.People,
                label = "Tenants",
                count = totalTenants,
                onClick = onTenantListClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Receipt,
                label = "Bills",
                onClick = onBillsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    count: Int,
    description: String,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    val semanticDescription = "$title: $count pending, $description"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    count: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticDescription = if (count != null && count > 0) {
        "$label: $count items"
    } else {
        label
    }

    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            if (count != null && count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LandlordDashboardContent(
    uiState: LandlordDashboardUiState,
    modifier: Modifier = Modifier,
    onNavigateToPendingReadings: () -> Unit = {},
    onNavigateToTenantList: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToBuildings: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToImportBuildingRoom: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val onPendingReadingsClick = remember { onNavigateToPendingReadings }
    val onTenantListClick = remember { onNavigateToTenantList }
    val onBillsClick = remember { onNavigateToBills }
    val onBuildingsClick = remember { onNavigateToBuildings }
    val onMaintenanceClick = remember { onNavigateToMaintenance }
    val onImportClick = remember { onNavigateToImportBuildingRoom }
    val pullToRefreshState = rememberPullToRefreshState()

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState
    ) {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item(key = "header") {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { -30 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                LandlordWelcomeHeader(uiState.username)
            }
        }

        item(key = "quick_stats") {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 50)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactStatCard(
                        icon = Icons.Default.MeetingRoom,
                        value = "${uiState.occupiedRooms}/${uiState.totalRooms}",
                        label = "Rooms",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        icon = Icons.Default.Percent,
                        value = "${(uiState.occupancyRate * 100).toInt()}%",
                        label = "Occupied",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    CompactStatCard(
                        icon = Icons.Default.Notifications,
                        value = uiState.totalPendingActions.toString(),
                        label = "Pending",
                        color = if (uiState.totalPendingActions > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.totalRooms == 0) {
            item(key = "empty_state") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 100)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Buildings Yet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Get started by importing your buildings and rooms from an Excel file",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = onImportClick,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Import Buildings & Rooms")
                            }
                        }
                    }
                }
            }
        }

        if (uiState.pendingReadingsCount > 0 || uiState.newJoinRequestsCount > 0 ||
            uiState.overdueBillsCount > 0 || uiState.totalRooms > 0) {
            item(key = "quick_actions") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 100)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    QuickActionsSection(
                        pendingReadingsCount = uiState.pendingReadingsCount,
                        newJoinRequestsCount = uiState.newJoinRequestsCount,
                        overdueBillsCount = uiState.overdueBillsCount,
                        totalBuildings = uiState.totalBuildings,
                        totalTenants = uiState.totalTenants,
                        onNavigateToPendingReadings = onPendingReadingsClick,
                        onNavigateToTenantList = onTenantListClick,
                        onNavigateToBills = onBillsClick,
                        onNavigateToBuildings = onBuildingsClick
                    )
                }
            }
        }

        // Additional stats section
        if (uiState.pendingRequestsCount > 0 || uiState.upcomingDeadlines > 0) {
            item(key = "additional_alerts") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 120)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.pendingRequestsCount > 0) {
                            ActionCard(
                                icon = Icons.Default.Build,
                                title = "Pending Requests",
                                count = uiState.pendingRequestsCount,
                                description = "Review and assign requests",
                                color = MaterialTheme.colorScheme.tertiary,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = onMaintenanceClick
                            )
                        }

                        if (uiState.upcomingDeadlines > 0) {
                            ActionCard(
                                icon = Icons.Default.CalendarToday,
                                title = "Billing Deadlines",
                                count = uiState.upcomingDeadlines,
                                description = "Bills due soon",
                                color = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = onBillsClick
                            )
                        }
                    }
                }
            }
        }

        // Overview stats section
        item(key = "overview_stats") {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600, delayMillis = 130)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CompactStatCard(
                            icon = Icons.Default.CheckCircle,
                            value = "${uiState.activeContracts}",
                            label = "Active",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        CompactStatCard(
                            icon = Icons.Default.People,
                            value = "${uiState.totalTenants}",
                            label = "Tenants",
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.recentPayments > 0) {
                            CompactStatCard(
                                icon = Icons.Default.AttachMoney,
                                value = "${uiState.recentPayments}",
                                label = "Recent",
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.revenueHistory.isNotEmpty()) {
            item(key = "revenue_chart") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 150)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    BentoRevenueChart(revenueHistory = uiState.revenueHistory)
                }
            }
        }

        if (uiState.monthlyRevenue > 0 || uiState.unpaidBalance > 0) {
            item(key = "financial_cards") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 200)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoFinancialCard(
                            icon = Icons.Default.AttachMoney,
                            label = "Monthly Revenue",
                            value = uiState.monthlyRevenue,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onBillsClick
                        )
                        BentoFinancialCard(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Collected",
                            value = uiState.monthlyRevenue - uiState.unpaidBalance,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = {}
                        )
                    }
                }
            }
        }

        if ((uiState.totalRooms > 0 && uiState.occupiedRooms > 0) || uiState.revenueHistory.size >= 2) {
            item(key = "rent_growth_cards") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 250)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.occupiedRooms > 0 && uiState.monthlyRevenue > 0) {
                            BentoAverageRentCard(
                                monthlyRevenue = uiState.monthlyRevenue,
                                occupiedRooms = uiState.occupiedRooms,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (uiState.revenueHistory.size >= 2) {
                            BentoGrowthCard(
                                revenueHistory = uiState.revenueHistory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.monthlyRevenue > 0 || uiState.unpaidBalance > 0) {
            item(key = "payment_status") {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(600, delayMillis = 300)) +
                            slideInVertically(
                                initialOffsetY = { 40 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                ) {
                    BentoPaymentStatus(
                        monthlyRevenue = uiState.monthlyRevenue,
                        unpaidBalance = uiState.unpaidBalance
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CompactStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val semanticDescription = "$label: $value"

    Card(
        modifier = modifier
            .height(100.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LandlordWelcomeHeader(username: String) {
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
fun BentoFinancialCard(
    icon: ImageVector,
    label: String,
    value: Double,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val formattedValue = remember(value) { Utils.formatCompactCurrency(value) }
    val fullFormattedValue = remember(value) { Utils.formatCurrency(value.toString()) }
    val semanticDescription = "$label: $fullFormattedValue"
    val onClickMemoized = remember { onClick }

    Card(
        modifier = modifier
            .height(120.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClickMemoized)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
        containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BentoAverageRentCard(
    monthlyRevenue: Double,
    occupiedRooms: Int,
    modifier: Modifier = Modifier
) {
    val averageRent = remember(monthlyRevenue, occupiedRooms) {
        if (occupiedRooms > 0) monthlyRevenue / occupiedRooms else 0.0
    }
    val formattedRent = remember(averageRent) { Utils.formatCompactCurrency(averageRent) }
    val fullFormattedRent = remember(averageRent) { Utils.formatCurrency(averageRent.toString()) }
    val semanticDescription = "Average rent: $fullFormattedRent per room per month, $occupiedRooms occupied rooms"

    Card(
        modifier = modifier
            .height(180.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avg Rent",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Average rent",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formattedRent,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "per room/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$occupiedRooms occupied rooms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BentoGrowthCard(
    revenueHistory: List<UiRevenueDataPoint>,
    modifier: Modifier = Modifier
) {
    val growthPercentage = remember(revenueHistory) {
        if (revenueHistory.size >= 2) {
            val latest = revenueHistory.last().revenue.toDouble()
            val previous = revenueHistory[revenueHistory.size - 2].revenue.toDouble()
            if (previous > 0) ((latest - previous) / previous) * 100 else 0.0
        } else 0.0
    }

    val isPositive = growthPercentage >= 0
    val formattedGrowth = remember(growthPercentage, isPositive) {
        "${if (isPositive) "+" else ""}${String.format("%.1f", growthPercentage)}%"
    }
    val semanticDescription = "Revenue growth: $formattedGrowth compared to last month"

    Card(
        modifier = modifier
            .height(180.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Growth",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = if (isPositive) "Growth trending up" else "Growth trending down",
                    tint = if (isPositive)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formattedGrowth,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "vs last month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun BentoPaymentStatus(
    monthlyRevenue: Double,
    unpaidBalance: Double
) {
    val paidAmount = remember(monthlyRevenue, unpaidBalance) {
        monthlyRevenue - unpaidBalance
    }
    val paidPercentage = remember(monthlyRevenue, paidAmount) {
        if (monthlyRevenue > 0) paidAmount / monthlyRevenue else 0.0
    }
    val formattedPaid = remember(paidAmount) { Utils.formatCompactCurrency(paidAmount) }
    val formattedUnpaid = remember(unpaidBalance) { Utils.formatCompactCurrency(unpaidBalance) }
    val fullFormattedPaid = remember(paidAmount) { Utils.formatCurrency(paidAmount.toString()) }
    val fullFormattedUnpaid = remember(unpaidBalance) { Utils.formatCurrency(unpaidBalance.toString()) }
    val semanticDescription = "Payment status: $fullFormattedPaid paid, $fullFormattedUnpaid unpaid out of ${Utils.formatCurrency(monthlyRevenue.toString())} total"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = semanticDescription
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payment Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(paidPercentage.toFloat())
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Paid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedPaid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Unpaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedUnpaid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun BentoRevenueChart(revenueHistory: List<UiRevenueDataPoint>) {
    if (revenueHistory.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Revenue Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${revenueHistory.size}M",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LineChart(revenueHistory = revenueHistory)
        }
    }
}

@Composable
fun LineChart(revenueHistory: List<UiRevenueDataPoint>) {
    val maxRevenue = remember(revenueHistory) {
        revenueHistory.maxOfOrNull { it.revenue.toDouble() } ?: 1.0
    }
    val minRevenue = remember(revenueHistory) {
        revenueHistory.minOfOrNull { it.revenue.toDouble() } ?: 0.0
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val semanticDescription = remember(revenueHistory) {
        "Revenue trend chart showing ${revenueHistory.size} months of data"
    }

    Column(
        modifier = Modifier.semantics {
            contentDescription = semanticDescription
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width - 40.dp.toPx()
                val chartHeight = size.height - 40.dp.toPx()
                val spacing = chartWidth / (revenueHistory.size - 1).coerceAtLeast(1)
                val startX = 20.dp.toPx()
                val startY = 10.dp.toPx()

                for (i in 0..4) {
                    val y = startY + (chartHeight * i / 4)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.1f),
                        start = Offset(startX, y),
                        end = Offset(startX + chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = revenueHistory.mapIndexed { index, dataPoint ->
                    val x = startX + index * spacing
                    val normalizedValue = ((dataPoint.revenue.toDouble() - minRevenue) /
                            (maxRevenue - minRevenue).coerceAtLeast(1.0)).toFloat()
                    val y = startY + chartHeight * (1 - normalizedValue)
                    Offset(x, y)
                }

                val path = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points[0].x, points[0].y)
                        for (i in 0 until points.size - 1) {
                            val currentPoint = points[i]
                            val nextPoint = points[i + 1]
                            val controlX1 = currentPoint.x + (nextPoint.x - currentPoint.x) / 3
                            val controlY1 = currentPoint.y
                            val controlX2 = currentPoint.x + 2 * (nextPoint.x - currentPoint.x) / 3
                            val controlY2 = nextPoint.y
                            cubicTo(controlX1, controlY1, controlX2, controlY2, nextPoint.x, nextPoint.y)
                        }
                    }
                }

                val fillPath = Path().apply {
                    addPath(path)
                    if (points.isNotEmpty()) {
                        lineTo(points.last().x, startY + chartHeight)
                        lineTo(points.first().x, startY + chartHeight)
                        close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.4f),
                            primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.7f),
                            primaryColor,
                            primaryColor.copy(alpha = 0.9f)
                        )
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, point ->
                    val isSelected = selectedIndex == index
                    drawCircle(
                        color = primaryColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                        center = point
                    )
                }

                selectedIndex?.let { index ->
                    if (index in points.indices) {
                        val point = points[index]
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.2f),
                            radius = 12.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            revenueHistory.forEachIndexed { index, dataPoint ->
                val formattedRevenue = remember(dataPoint.revenue) {
                    Utils.formatCompactCurrency(dataPoint.revenue.toDouble())
                }
                val onClick = remember(index) {
                    { selectedIndex = if (selectedIndex == index) null else index }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                        .semantics {
                            contentDescription = "${dataPoint.monthLabel}: ${Utils.formatCurrency(dataPoint.revenue.toString())}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dataPoint.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedIndex == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedRevenue,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (selectedIndex == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}