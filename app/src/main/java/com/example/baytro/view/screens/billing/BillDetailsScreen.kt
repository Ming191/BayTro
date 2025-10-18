package com.example.baytro.view.screens.billing

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.baytro.data.billing.*
import com.example.baytro.utils.Utils
import com.example.baytro.viewModel.billing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    navController: NavController,
    viewModel: BillDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { singleEvent ->
            singleEvent.getContentIfNotHandled()?.let { event ->
                when (event) {
                    is BillDetailsEvent.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    is BillDetailsEvent.NavigateBack -> {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isLandlord && uiState.bill?.status != BillStatus.PAID,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { viewModel.onAction(BillDetailsAction.ShowManualChargeDialog) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Manual Charge")
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error!!,
                        onRetry = { /* Add retry action */ }
                    )
                }
                uiState.bill != null -> {
                    BillContentView(
                        bill = uiState.bill!!,
                        qrCodeUrl = uiState.qrCodeUrl,
                        isLandlord = uiState.isLandlord,
                        isActionInProgress = uiState.isActionInProgress,
                        onAction = { action -> viewModel.onAction(action) },
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
                else -> {
                    EmptyState()
                }
            }
        }

        if (uiState.showManualChargeDialog) {
            ManualChargeDialog(
                onDismiss = { viewModel.onAction(BillDetailsAction.HideManualChargeDialog) },
                onConfirm = { description, amount ->
                    viewModel.onAction(BillDetailsAction.AddManualCharge(description, amount))
                }
            )
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading bill details...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No bill information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BillContentView(
    bill: Bill,
    qrCodeUrl: String?,
    isLandlord: Boolean,
    isActionInProgress: Boolean,
    onAction: (BillDetailsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                BillStatusCard(bill = bill)
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
                BillSummaryCard(bill = bill)
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
                LineItemsSection(bill.lineItems)
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
                when {
                    bill.status == BillStatus.PAID -> PaidInfoSection(bill)
                    isLandlord -> LandlordActions(isInProgress = isActionInProgress, onAction = onAction)
                    else -> TenantPaymentView(
                        qrCodeUrl = qrCodeUrl,
                        paymentCode = bill.paymentCode,
                        paymentDetails = bill.paymentDetails,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun BillStatusCard(bill: Bill) {
    if (bill.status == BillStatus.PAID) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Payment Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun BillSummaryCard(bill: Bill) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.roomName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Utils.getMonthName(bill.month)} ${bill.year}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Utils.formatCurrency(bill.totalAmount.toString()),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LineItemsSection(lineItems: List<BillLineItem>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            lineItems.forEachIndexed { index, item ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                LineItemRow(item)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 2.dp
            )

            LineItemRow(
                BillLineItem(
                    description = "Total",
                    totalCost = lineItems.sumOf { it.totalCost }
                ),
                isTotal = true
            )
        }
    }
}

@Composable
fun LineItemRow(item: BillLineItem, isTotal: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.description,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = Utils.formatCurrency(item.totalCost.toString()),
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isTotal) MaterialTheme.colorScheme.primary else Color.Unspecified
        )
    }
}

@Composable
fun TenantPaymentView(
    qrCodeUrl: String?,
    paymentCode: String?,
    paymentDetails: PaymentDetails?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Payment code copied to clipboard",
                    duration = SnackbarDuration.Short
                )
            }
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Payment Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (qrCodeUrl != null) {
                Text(
                    text = "Scan QR Code",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    AsyncImage(
                        model = qrCodeUrl,
                        contentDescription = "Payment QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Bank Transfer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (paymentDetails != null) {
                        Text(
                            text = "${paymentDetails.bankCode} - ${paymentDetails.accountNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No bank transfer information available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (paymentCode != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Transfer Content",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = paymentCode,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            FilledTonalButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Payment Code", paymentCode)
                                    clipboardManager.setPrimaryClip(clip)
                                    copied = true
                                }
                            ) {
                                Icon(
                                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (copied) "Copied!" else "Copy")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordActions(
    isInProgress: Boolean,
    onAction: (BillDetailsAction) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Landlord Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onAction(BillDetailsAction.MarkAsPaid) },
                enabled = !isInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Paid")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onAction(BillDetailsAction.SendReminder) },
                enabled = !isInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Reminder")
            }
        }
    }
}

@Composable
fun PaidInfoSection(bill: Bill) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Payment Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    bill.paymentDate?.let {
                        InfoRow("Payment Date", Utils.formatTimestamp(it))
                    }
                    InfoRow("Amount Paid", Utils.formatCurrency(bill.paidAmount.toString()))
                    bill.paymentMethod?.let {
                        InfoRow("Method", formatPaymentMethod(it))
                    }
                }
            }
        }
    }
}

fun formatPaymentMethod(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.BANK_TRANSFER_MANUAL -> "Bank Transfer (Manual)"
        PaymentMethod.BANK_TRANSFER_AUTO -> "Bank Transfer (Auto)"
        PaymentMethod.CASH -> "Cash"
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ManualChargeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Manual Charge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = null
                    },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Extra cleaning fee, Repairs") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' }
                        amountError = null
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    prefix = { Text("₫ ", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )

                if (amount.toDoubleOrNull() != null && amount.toDouble() > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total charge:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = Utils.formatCurrency(amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    var hasError = false

                    if (description.isBlank()) {
                        descriptionError = "Description required"
                        hasError = true
                    }
                    if (amountValue == null || amountValue <= 0) {
                        amountError = "Valid amount required"
                        hasError = true
                    }

                    if (!hasError) {
                        onConfirm(description, amountValue!!)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Charge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
