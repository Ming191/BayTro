package com.example.baytro.view.screens.room

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.baytro.utils.Utils.formatCurrency
import com.example.baytro.view.components.ContractCard
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.TenantsSection
import com.example.baytro.viewModel.Room.RoomDetailsVM
import org.koin.compose.viewmodel.koinViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun RoomDetailsScreen(
    viewModel: RoomDetailsVM = koinViewModel(),
    onAddContractClick: (String) -> Unit,
    onViewContractClick: (String) -> Unit,
    onEditRoomOnClick: (String) -> Unit,
    onBackClick: () -> Unit,
    navController: NavHostController? = null
) {
    val room by viewModel.room.collectAsState()
    val buildingServices by viewModel.buildingServices.collectAsState()
    val extraServices by viewModel.extraServices.collectAsState()
    val contracts by viewModel.contract.collectAsState()
    val tenants by viewModel.tenants.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDeleteOnClicked by viewModel.isDeleteOnClicked.collectAsState()
    val isDeletingRoom by viewModel.isDeletingRoom.collectAsState()
    val context: Context = LocalContext.current

    // Handle error events
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Handle success events
    LaunchedEffect(Unit) {
        viewModel.successEvent.collect { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                // Set flag to indicate room was modified (archived)
                navController?.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("room_modified", true)
                onBackClick()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadRoom()
        viewModel.getRoomContract()
        viewModel.getRoomTenants()
    }

    Crossfade(
        targetState = isLoading,
        label = "room_details_crossfade"
    ) { loading ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Loading room details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    Surface(
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onEditRoomOnClick(room?.id.toString()) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Edit Room")
                        }

                        OutlinedButton(
                            onClick = { viewModel.onDeleteClick() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Room Status Badge & Info
                item {
                    RoomHeaderCard(
                        roomNumber = room?.roomNumber ?: "",
                        status = room?.status?.name ?: "",
                        floor = room?.floor?.toString() ?: "",
                        size = room?.size?.toString() ?: "",
                        rentalFee = room?.rentalFee?.toString() ?: "",
                        interior = room?.interior?.name ?: ""
                    )
                }

                // Contract Section
                item {
                    SectionTitle(
                        title = "Contract Information"
                    )
                }

                item {
                    val contract = contracts.firstOrNull()
                    if (contract != null) {
                        ContractCard(
                            contractNumber = contract.contractNumber,
                            roomNumber = room?.roomNumber ?: "",
                            startDate = contract.startDate,
                            endDate = contract.endDate,
                            status = contract.status,
                            onClick = { onViewContractClick(contract.id) }
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Filled.Description,
                            message = "No active contract",
                            actionText = "Add Contract",
                            onActionClick = { onAddContractClick(room?.id.toString()) }
                        )
                    }
                }

                // Building Services Section
                item {
                    SectionTitle(
                        title = "Building Services"
                    )
                }

                item {
                    if (buildingServices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            buildingServices.forEach { service ->
                                ServiceCard(
                                    service = service,
                                    onEdit = null,
                                    onDelete = null
                                )
                            }
                        }
                    } else {
                        EmptyStateCard(
                            icon = Icons.Filled.HomeRepairService,
                            message = "No building services available",
                            actionText = null,
                            onActionClick = null
                        )
                    }
                }

                // Extra Services Section
                item {
                    SectionTitle(
                        title = "Extra Services"
                    )
                }

                item {
                    if (extraServices.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            extraServices.forEach { service ->
                                ServiceCard(
                                    service = service,
                                    onEdit = null,
                                    onDelete = null
                                )
                            }
                        }
                    } else {
                        EmptyStateCard(
                            icon = Icons.Filled.MiscellaneousServices,
                            message = "No extra services in this room",
                            actionText = null,
                            onActionClick = null
                        )
                    }
                }

                // Tenants Section
                item {
                    SectionTitle(
                        title = "Tenants"
                    )
                }

                item {
                    if (tenants.isNotEmpty()) {
                        TenantsSection(
                            tenants = tenants,
                            onAddTenantClick = {},
                            showAddButton = false
                        )
                    } else {
                        EmptyStateCard(
                            icon = Icons.Outlined.PersonOff,
                            message = "No tenants in this room",
                            actionText = null,
                            onActionClick = null
                        )
                    }
                }

                // Bottom padding for bottom bar
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }

        // Loading overlay when deleting room
        if (isDeletingRoom) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Archiving room...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (isDeleteOnClicked) {
        AlertDialog(
                onDismissRequest = {
                    if (!isDeletingRoom) {
                        viewModel.onCancelDelete()
                    }
                },
                icon = {
                    Icon(
                        Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Archive Room?") },
                text = {
                    Text("Are you sure you want to archive Room ${room?.roomNumber}? This room will be hidden but can be restored later. You cannot archive a room with an active contract.")
                },
                confirmButton = {
                    FilledTonalButton(
                        onClick = {
                            viewModel.deleteRoom()
                        },
                        enabled = !isDeletingRoom,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onCancelDelete() },
                        enabled = !isDeletingRoom
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RoomHeaderCard(
    roomNumber: String,
    status: String,
    floor: String,
    size: String,
    rentalFee: String,
    interior: String
) {
    OutlinedCard (
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Room $roomNumber",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (status.uppercase()) {
                        "AVAILABLE" -> MaterialTheme.colorScheme.tertiaryContainer
                        "OCCUPIED" -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = status.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (status.uppercase()) {
                            "AVAILABLE" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "OCCUPIED" -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            // Room details grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoItem(
                        icon = Icons.Filled.Layers,
                        label = "Floor",
                        value = floor,
                        modifier = Modifier.weight(1f)
                    )
                    InfoItem(
                        icon = Icons.Filled.Straighten,
                        label = "Size",
                        value = "$size m²",
                        modifier = Modifier.weight(1f)
                    )
                }

                InfoItem(
                    icon = Icons.Filled.AttachMoney,
                    label = "Rental Fee",
                    value = formatCurrency(rentalFee),
                    modifier = Modifier.fillMaxWidth()
                )

                InfoItem(
                    icon = Icons.Filled.Chair,
                    label = "Interior",
                    value = interior.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp).size(20.dp)
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String
) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        Surface(
//            shape = RoundedCornerShape(8.dp),
//            color = MaterialTheme.colorScheme.secondaryContainer
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.onSecondaryContainer,
//                modifier = Modifier.padding(8.dp)
//            )
//        }
//        Text(
//            text = title,
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.SemiBold,
//            color = MaterialTheme.colorScheme.onSurface
//        )
//    }
    DividerWithSubhead(
        subhead = title,
        modifier = Modifier.fillMaxWidth()
    )
}



@Composable
fun EmptyStateCard(
    icon: ImageVector,
    message: String,
    actionText: String?,
    onActionClick: (() -> Unit)?
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp).size(32.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                OutlinedButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(actionText)
                }
            }
        }
    }
}