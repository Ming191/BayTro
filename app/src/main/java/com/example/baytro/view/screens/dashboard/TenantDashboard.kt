package com.example.baytro.view.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.data.contract.Status
import com.example.baytro.utils.Utils
import com.example.baytro.utils.Utils.formatOrdinal
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.viewModel.dashboard.TenantDashboardVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TenantDashboard(
    navController: NavHostController? = null,
    viewModel: TenantDashboardVM = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.error ?: "An error occurred",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val contract = uiState.contract
    val user = uiState.user

    TenantDashboardContent(
        username = user?.fullName ?: "Guest",
        month = uiState.monthsStayed,
        days = uiState.daysStayed,
        status = contract?.status ?: Status.ACTIVE,
        billPaymentDeadline = uiState.billPaymentDeadline,
        rentalFee = contract?.rentalFee ?: 0,
        deposit = contract?.deposit ?: 0,
        onViewDetailsClick = {
            val contractId = viewModel.getContractId()
            if (contractId != null && navController != null) {
                navController.navigate("contract_details_screen/$contractId")
            }
        }
    )
}

@Composable
fun TenantDashboardContent(
    username: String = "John Doe",
    month: Int = 0,
    days: Int = 0,
    status: Status = Status.ACTIVE,
    billPaymentDeadline: Int = 0,
    rentalFee: Int = 0,
    deposit: Int = 0,
    onViewDetailsClick: () -> Unit = {}
) {
    Scaffold(
        topBar = { /* TopAppBar can be added here */ },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        text = "Welcome back,\n$username!",
                        modifier = Modifier.fillMaxSize(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Contracts Icon",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = "Days stayed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "$month month${if (month != 1 && month != 0) "s" else ""}, $days day${if (days != 1 && days != 0) "s" else ""}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Button(
                                    onClick = onViewDetailsClick,
                                    content = {
                                        Text("View details")
                                    },
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Status:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = statusDotColor(status),
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                                Text(
                                    text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    DividerWithSubhead(
                        subhead = "Maintenance",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = { /* Handle request maintenance action */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Request Maintenance")
                    }
                }
                item {
                    DividerWithSubhead(
                        subhead = "Payments",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Contracts Icon",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Bill payment deadline",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Every month on the ${formatOrdinal(billPaymentDeadline)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                            )
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MeterItem(
                                    icon = Icons.Default.House,
                                    label = "Rental fee",
                                    value = Utils.formatCurrency(rentalFee.toString()),
                                    modifier = Modifier.weight(1f)
                                )
                                MeterItem(
                                    icon = Icons.Default.Money,
                                    label = "Deposit",
                                    value = Utils.formatCurrency(deposit.toString()),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                            )
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MeterItem(
                                    icon = Icons.Default.ElectricBolt,
                                    label = "Electricity",
                                    value = "4.000 VND/kWH",
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MeterValueItem(
                                        icon = Icons.Default.Timer,
                                        value = "100",
                                        lastChecked = "9 Sep 2025"
                                    )
                                }
                            }
                            Row (
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MeterItem(
                                    icon = Icons.Default.Water,
                                    label = "Water",
                                    value = "18.000 VND/m3",
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MeterValueItem(
                                        icon = Icons.Default.Timer,
                                        value = "100",
                                        lastChecked = "9 Sep 2025"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun TenantDashboardPreview() {
    TenantDashboardContent()
}

@Composable
fun statusDotColor(status: Status): Color {
    return when (status) {
        Status.ACTIVE -> MaterialTheme.colorScheme.primary
        Status.OVERDUE -> MaterialTheme.colorScheme.error
        Status.PENDING -> MaterialTheme.colorScheme.tertiary
        Status.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun MeterItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MeterValueItem(
    icon: ImageVector,
    value: String,
    lastChecked: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = modifier.size(40.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Column(
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Last checked:\n$lastChecked",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
