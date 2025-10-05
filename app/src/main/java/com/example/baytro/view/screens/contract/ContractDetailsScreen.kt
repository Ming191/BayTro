package com.example.baytro.view.screens.contract

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.baytro.data.contract.Status
import com.example.baytro.data.qr_session.PendingQrSession
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.User
import com.example.baytro.view.components.AddFirstTenantPrompt
import com.example.baytro.view.components.ContractCard
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.QrCodeDialog
import com.example.baytro.view.components.TenantsSection
import com.example.baytro.viewModel.contract.ContractDetailsFormState
import com.example.baytro.viewModel.contract.ContractDetailsVM
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel


@Preview
@Composable
fun ContractDetailsScreenPreview() {
    val sampleFormState = remember {
        ContractDetailsFormState(
            contractNumber = "123456",
            buildingName = "Sunrise Apartments",
            roomNumber = "A101",
            startDate = "2023-01-01",
            endDate = "2023-12-31",
            rentalFee = "15000000",
            deposit = "30000000",
            status = Status.ACTIVE,
            tenantList = listOf(
                User(
                    id = "1",
                    fullName = "Alice Johnson",
                    email = "",
                    profileImgUrl = "",
                    phoneNumber = "",
                    role = Role.Tenant(
                        occupation = "",
                        idCardNumber = "https://randomuser.me/api/portraits/men/73.jpg",
                        idCardImageFrontUrl = "https://randomuser.me/api/portraits/men/73.jpg",
                        idCardImageBackUrl = "https://randomuser.me/api/portraits/men/73.jpg",
                        idCardIssueDate = "https://randomuser.me/api/portraits/men/73.jpg",
                        emergencyContact = "https://randomuser.me/api/portraits/men/73.jpg"
                    ),
                    dateOfBirth = "",
                    gender = Gender.MALE,
                    address = "",
                    fcmToken = null,
                )
            )
        )
    }
    val samplePendingSessions = remember {
        listOf(
            PendingQrSession(
                tenantName = "Bob Smith",
                tenantAvatarUrl = "https://randomuser.me/api/portraits/men/73.jpg",
                sessionId = "session_1",
                tenantId = "2"
            )
        )
    }
    ContractDetailsContent(
        formState = sampleFormState,
        pendingSessions = samplePendingSessions,
        confirmingIds = emptySet(),
        decliningIds = emptySet(),
        onAddTenant = {},
        onConfirmTenant = {},
        onDeclineTenant = {},
        isLandlord = true
    )
}

@Composable
fun ContractDetailsScreen(
    viewModel: ContractDetailsVM = koinViewModel(),
    contractId: String
) {
    LaunchedEffect(contractId) {
        viewModel.loadContract(contractId)
    }
    val qrState by viewModel.qrState
    val formState by viewModel.formState.collectAsState()
    val pendingSessions by viewModel.pendingSessions.collectAsState()
    val confirmingIds by viewModel.confirmingSessionIds.collectAsState()
    val decliningIds by viewModel.decliningSessionIds.collectAsState()
    val error by viewModel.actionError.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val isLandlord by viewModel.isLandlord.collectAsState()

    var indicatorVisible by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(loading) {
        if (!loading) {
            indicatorVisible = false
        } else {
            indicatorVisible = true
            contentVisible = false
        }
    }

    LaunchedEffect(indicatorVisible) {
        if (!indicatorVisible) {
            delay(300)
            contentVisible = true
        } else {
            contentVisible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = indicatorVisible,
            exit = fadeOut(),
            enter = fadeIn()
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn()
        ) {
            LaunchedEffect(error) {
                error?.let { viewModel.clearActionError() }
            }
            QrCodeDialog(
                state = qrState,
                onDismissRequest = viewModel::clearQrCode,
                onRetry = viewModel::generateQrCode
            )
            ContractDetailsContent(
                formState = formState,
                pendingSessions = pendingSessions,
                confirmingIds = confirmingIds,
                decliningIds = decliningIds,
                isLandlord = isLandlord,
                onAddTenant = viewModel::generateQrCode,
                onConfirmTenant = viewModel::confirmTenant,
                onDeclineTenant = viewModel::declineTenant
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
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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

            item {
                DividerWithSubhead(
                    subhead = "Services",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
                AnimatedVisibility(
                    visible = pendingSessions.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    PendingRequestsSection(
                        sessions = pendingSessions,
                        confirmingIds = confirmingIds,
                        decliningIds = decliningIds,
                        onConfirm = onConfirmTenant,
                        onDecline = onDeclineTenant
                    )
                }
            }

            item {
                DividerWithSubhead(
                    subhead = "Tenants",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                AnimatedVisibility(
                    visible = formState.isPendingContract && !formState.hasActiveTenants && pendingSessions.isEmpty() && isLandlord,
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
                        showAddButton = isLandlord
                    )
                }
            }

            // Animated ActionButtonsRow
            item {
                AnimatedVisibility(
                    visible = formState.isActiveContract && isLandlord,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Spacer(Modifier.height(16.dp))
                    ActionButtonsRow()
                }
            }
        }
    }
}

@Composable
fun PendingRequestsSection(
    sessions: List<PendingQrSession>,
    confirmingIds: Set<String>,
    decliningIds: Set<String>,
    onConfirm: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DividerWithSubhead(
            subhead = "Pending Requests",
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )

        sessions.forEach { session ->
            val isConfirming = session.sessionId in confirmingIds
            val isDeclining = session.sessionId in decliningIds
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
                            enabled = !isLoading, // Vô hiệu hóa khi đang xử lý
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
    }
}

@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ActionButton(text = "Export", icon = Icons.Default.UploadFile, modifier = Modifier.weight(1f), isPrimary = true)
        ActionButton(text = "Edit", icon = Icons.Default.Edit, modifier = Modifier.weight(1f), isPrimary = false)
        ActionButton(text = "End", icon = Icons.Default.Delete, modifier = Modifier.weight(1f), isPrimary = false)
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit = {}
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
