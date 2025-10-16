package com.example.baytro.view.screens.billing

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.baytro.data.billing.BillStatus
import com.example.baytro.data.billing.BillSummary
import com.example.baytro.navigation.Screens
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.viewModel.billing.LandlordBillsViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

// =====================================================================
//                       MAIN SCREEN COMPOSABLE
// =====================================================================

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordBillsScreen(
    navController: NavController,
    viewModel: LandlordBillsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let { errorMessage ->
                snackbarHostState.showSnackbar(
                    message = errorMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1.2f)) {
                            DropdownSelectField(
                                label = "Building",
                                options = uiState.buildings,
                                selectedOption = uiState.buildings.find { it.id == uiState.selectedBuildingId },
                                onOptionSelected = { viewModel.selectBuilding(it.id) },
                                optionToString = { it.name },
                                modifier = Modifier.fillMaxWidth(),
                                trailingContent = { building ->
                                    if (building.pendingCount > 0) {
                                        Badge {
                                            Text(text = "${building.pendingCount}")
                                        }
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            MonthSelector(
                                selectedDate = uiState.selectedDate,
                                onPrevious = { viewModel.goToPreviousMonth() },
                                onNext = { viewModel.goToNextMonth() }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics Summary Card
            AnimatedVisibility(
                visible = !uiState.isLoading && uiState.filteredBills.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                BillsStatisticsSummary(
                    bills = uiState.filteredBills,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Pending Readings Banner
            AnimatedVisibility(
                visible = !uiState.isLoading && uiState.pendingReadingsCount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                PendingReadingsBanner(
                    pendingCount = uiState.pendingReadingsCount,
                    onReviewClick = { navController.navigate(Screens.PendingMeterReadings.route) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Status Filter Chips
            StatusFilterChips(
                selectedStatus = uiState.selectedStatus,
                onStatusSelected = { viewModel.selectStatus(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingState(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.filteredBills.isEmpty() -> {
                        EmptyState(
                            monthName = Utils.getMonthName(uiState.selectedDate.first),
                            year = uiState.selectedDate.second,
                            status = uiState.selectedStatus
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            items(
                                items = uiState.filteredBills,
                                key = { it.id }
                            ) { bill ->
                                BillSummaryCard(
                                    bill = bill,
                                    onClick = { navController.navigate(Screens.BillDetails.createRoute(bill.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//                       STATISTICS SUMMARY
// =====================================================================

@Composable
fun BillsStatisticsSummary(
    bills: List<BillSummary>,
    modifier: Modifier = Modifier
) {
    val totalAmount = bills.sumOf { it.totalAmount }
    val paidAmount = bills.filter { it.status == BillStatus.PAID }.sumOf { it.totalAmount }
    val unpaidAmount = totalAmount - paidAmount
    val paidCount = bills.count { it.status == BillStatus.PAID }
    val totalCount = bills.size

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$paidCount / $totalCount paid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatisticItem(
                    label = "Total",
                    amount = totalAmount,
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                StatisticItem(
                    label = "Unpaid",
                    amount = unpaidAmount,
                    icon = Icons.Default.PendingActions,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Utils.formatCurrency(amount.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =====================================================================
//                       FILTER CHIPS
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterChips(
    selectedStatus: BillStatus?,
    onStatusSelected: (BillStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(null) + BillStatus.entries.filter { it != BillStatus.NOT_ISSUED_YET }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.forEach { status ->
            val label = when(status) {
                null -> "All"
                BillStatus.UNPAID -> "Unpaid"
                BillStatus.OVERDUE -> "Overdue"
                BillStatus.PAID -> "Paid"
                else -> ""
            }

            if (label.isNotEmpty()) {
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    label = { Text(label) },
                    leadingIcon = if (selectedStatus == status) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

// =====================================================================
//                       MONTH SELECTOR
// =====================================================================

@Composable
fun MonthSelector(
    selectedDate: Pair<Int, Int>,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val selectedMonthLabel = "${Utils.getMonthName(selectedDate.first, short = true)} '${selectedDate.second.toString().takeLast(2)}"

    val (isPrevEnabled, isNextEnabled) = remember(selectedDate) {
        val current = Calendar.getInstance()
        val currentYear = current.get(Calendar.YEAR)
        val currentMonth = current.get(Calendar.MONTH) + 1

        val selectedYear = selectedDate.second
        val selectedMonth = selectedDate.first
        val monthsDiff = (currentYear - selectedYear) * 12 + (currentMonth - selectedMonth)

        (monthsDiff < 12) to (monthsDiff > 0)
    }

    OutlinedCard(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = isPrevEnabled
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }

            Text(
                text = selectedMonthLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onNext,
                enabled = isNextEnabled
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
    }
}

// =====================================================================
//                       PENDING READINGS BANNER
// =====================================================================

@Composable
fun PendingReadingsBanner(
    pendingCount: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Action Required",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$pendingCount pending reading${if (pendingCount > 1) "s" else ""} to review",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            FilledTonalButton(
                onClick = onReviewClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text("Review")
            }
        }
    }
}

// =====================================================================
//                       BILL SUMMARY CARD
// =====================================================================

@Composable
fun BillSummaryCard(
    bill: BillSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.roomName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Utils.formatCurrency(bill.totalAmount.toString()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(status = bill.status)

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// =====================================================================
//                       STATUS CHIP
// =====================================================================

@Composable
fun StatusChip(status: BillStatus) {
    val (colorTriple, icon) = when (status) {
        BillStatus.PAID -> {
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                "Paid"
            ) to Icons.Default.CheckCircle
        }
        BillStatus.UNPAID -> {
            Triple(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                "Unpaid"
            ) to Icons.Default.Schedule
        }
        BillStatus.OVERDUE -> {
            Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                "Overdue"
            ) to Icons.Default.Error
        }
        BillStatus.NOT_ISSUED_YET -> {
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Pending"
            ) to Icons.Default.HourglassEmpty
        }
    }

    val (backgroundColor, textColor, label) = colorTriple

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// =====================================================================
//                       LOADING & EMPTY STATES
// =====================================================================

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading bills...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(
    monthName: String,
    year: Int,
    status: BillStatus?
) {
    val statusText = when(status) {
        null -> "bills"
        BillStatus.PAID -> "paid bills"
        BillStatus.UNPAID -> "unpaid bills"
        BillStatus.OVERDUE -> "overdue bills"
        else -> "bills"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "No Bills Found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "There are no $statusText for $monthName $year.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}