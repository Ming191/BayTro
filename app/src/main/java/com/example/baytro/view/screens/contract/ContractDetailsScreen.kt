package com.example.baytro.view.screens.contract

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.utils.Utils
import com.example.baytro.view.components.AddFirstTenantPrompt
import com.example.baytro.view.components.ContractCard
import com.example.baytro.view.components.ContractDetailsSkeleton
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.QrCodeDialog
import com.example.baytro.view.components.TenantsSection
import com.example.baytro.viewModel.contract.ContractDetailsFormState
import com.example.baytro.viewModel.contract.ContractDetailsVM
import com.example.baytro.viewModel.contract.EndContractState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ContractDetailsScreen(
    viewModel: ContractDetailsVM = koinViewModel(),
    onEditContract: (String) -> Unit = {},
    contractId: String,
    navigateBack: () -> Unit = {}
) {
    LaunchedEffect(contractId) {
        viewModel.loadContract(contractId)
    }

    val formState by viewModel.formState.collectAsState()
    val qrState by viewModel.qrState.collectAsState()
    val endContractState by viewModel.endContractState.collectAsState()
    val pendingSessions by viewModel.pendingSessions.collectAsState()
    val confirmingIds by viewModel.confirmingSessionIds.collectAsState()
    val decliningIds by viewModel.decliningSessionIds.collectAsState()
    val error by viewModel.actionError.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isLandlord by viewModel.isLandlord.collectAsState()

    var hasLoadedOnce by remember { mutableStateOf(false) }
    var showEndContractDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let { errorMessage ->
            scope.launch {
                snackbarHostState.showSnackbar(message = errorMessage)
                viewModel.clearActionError()
            }
        }
    }

    LaunchedEffect(endContractState) {
        when (val state = endContractState) {
            is EndContractState.Warning -> {
                showEndContractDialog = true
            }
            is EndContractState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = state.response.message +
                            (state.response.warnings?.let { "\n${it.joinToString("\n")}" } ?: "")
                    )
                    viewModel.resetEndContractState()
                    navigateBack()
                }
            }
            is EndContractState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(message = state.message)
                    viewModel.resetEndContractState()
                }
            }
            else -> {}
        }
    }

    LaunchedEffect(loading) {
        if (!loading && !hasLoadedOnce) {
            hasLoadedOnce = true
        }
    }

    if (showEndContractDialog) {
        when (val state = endContractState) {
            is EndContractState.Warning -> {
                AlertDialog(
                    onDismissRequest = {
                        showEndContractDialog = false
                        viewModel.resetEndContractState()
                    },
                    title = { Text("Unpaid Bills Warning") },
                    text = {
                        Column {
                            Text(state.response.message)
                            state.response.unpaidBillsCount?.let { count ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Unpaid bills: $count")
                            }
                            state.response.totalUnpaidAmount?.let { amount ->
                                Text("Total unpaid: ${Utils.formatCurrency(amount.toString())}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Do you want to force end this contract anyway?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showEndContractDialog = false
                                viewModel.endContract(forceEnd = true)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Force End")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showEndContractDialog = false
                                viewModel.resetEndContractState()
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            else -> {
                showEndContractDialog = false
            }
        }
    }

    QrCodeDialog(
        state = qrState,
        onDismissRequest = viewModel::clearQrCode,
        onRetry = viewModel::generateQrCode
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        if (!hasLoadedOnce && loading) {
            Surface(modifier = Modifier.fillMaxSize()) {
                ContractDetailsSkeleton()
            }
        } else {
            ContractDetailsContent(
                formState = formState,
                pendingSessions = pendingSessions,
                confirmingIds = confirmingIds,
                decliningIds = decliningIds,
                isLandlord = isLandlord,
                onAddTenant = viewModel::generateQrCode,
                onConfirmTenant = viewModel::confirmTenant,
                onDeclineTenant = viewModel::declineTenant,
                onEditContract = { onEditContract(contractId) },
                onEndContract = { viewModel.endContract() }
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContractDetailsContent(
    formState: ContractDetailsFormState,
    pendingSessions: List<PendingQrSession>,
    confirmingIds: Set<String>,
    decliningIds: Set<String>,
    isLandlord: Boolean,
    onAddTenant: () -> Unit,
    onConfirmTenant: (String) -> Unit,
    onDeclineTenant: (String) -> Unit,
    onEditContract: () -> Unit,
    onEndContract: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { -it / 2 }
                ),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    DividerWithSubhead(
                        subhead = "Contract number #${formState.contractNumber}",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    )
                    ContractCard(
                        infoMap = mapOf(
                            "Building name:" to formState.buildingName,
                            "Room number:" to formState.roomNumber,
                            "Num.Tenants:" to (formState.tenantCount).toString(),
                            "Start Date:" to formState.startDate,
                            "End Date:" to formState.endDate,
                            "Rental fee:" to "${formState.formattedRentalFee} /month",
                            "Deposit:" to formState.formattedDeposit,
                        )
                    )
                }
            }
        }

        pendingRequestsSection(
            sessions = pendingSessions,
            confirmingIds = confirmingIds,
            decliningIds = decliningIds,
            onConfirm = onConfirmTenant,
            onDecline = onDeclineTenant
        )

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) + slideInVertically(
                    animationSpec = tween(300, delayMillis = 200),
                    initialOffsetY = { it / 3 }
                ),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    DividerWithSubhead(
                        subhead = "Tenants",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    AnimatedVisibility(
                        visible = formState.shouldShowAddFirstTenantPrompt(
                            pendingSessions, confirmingIds, decliningIds, isLandlord
                        ),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        AddFirstTenantPrompt(onClick = onAddTenant)
                    }

                    AnimatedVisibility(
                        visible = formState.hasActiveTenants,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        TenantsSection(
                            tenants = formState.tenantList,
                            onAddTenantClick = onAddTenant,
                            showAddButton = isLandlord && formState.isActiveContract
                        )
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = visible && formState.isActiveContract && isLandlord,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 300)) + slideInVertically(
                    animationSpec = tween(300, delayMillis = 300),
                    initialOffsetY = { it / 2 }
                ),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    ActionButtonsRow(
                        onEdit = onEditContract,
                        onEnd = onEndContract,
                        onExport = { /* TODO */ }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.pendingRequestsSection(
    sessions: List<PendingQrSession>,
    confirmingIds: Set<String>,
    decliningIds: Set<String>,
    onConfirm: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    if (sessions.isNotEmpty()) {
        item {
            DividerWithSubhead(
                subhead = "Pending Requests",
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
        }
        items(sessions, key = { it.sessionId }) { session ->
            PendingRequestItem(
                session = session,
                isConfirming = session.sessionId in confirmingIds,
                isDeclining = session.sessionId in decliningIds,
                onConfirm = onConfirm,
                onDecline = onDecline
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PendingRequestItem(
    session: PendingQrSession,
    isConfirming: Boolean,
    isDeclining: Boolean,
    onConfirm: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    val isLoading = isConfirming || isDeclining
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = session.tenantAvatarUrl,
                contentDescription = session.tenantName,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(session.tenantName, modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(session.sessionId) },
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    if (isConfirming) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Confirm")
                    }
                }
                OutlinedButton(
                    onClick = { onDecline(session.sessionId) },
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    if (isDeclining) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Decline")
                    }
                }
            }
        }
    }
}


@Composable
fun ActionButtonsRow(
    onEdit: () -> Unit,
    onEnd: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActionButton(text = "Export", icon = Icons.Default.UploadFile, modifier = Modifier.weight(1f), isPrimary = true, onClick = onExport)
        ActionButton(text = "Edit", icon = Icons.Default.Edit, modifier = Modifier.weight(1f), isPrimary = false, onClick = onEdit)
        ActionButton(text = "End", icon = Icons.Default.Delete, modifier = Modifier.weight(1f), isPrimary = false, onClick = onEnd)
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val buttonTextStyle = MaterialTheme.typography.labelMedium
    val iconSize = 16.dp
    val contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    if (isPrimary) {
        Button(onClick = onClick, modifier = modifier, contentPadding = contentPadding) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, style = buttonTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = contentPadding) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, style = buttonTextStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
