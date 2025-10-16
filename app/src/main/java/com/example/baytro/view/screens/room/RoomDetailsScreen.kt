package com.example.baytro.view.screens.room

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.baytro.navigation.Screens
import com.example.baytro.utils.Utils.formatCurrency
import com.example.baytro.view.components.ButtonComponent
import com.example.baytro.view.components.CardComponent
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.ServiceCard
import com.example.baytro.view.components.TenantsSection
import com.example.baytro.viewModel.Room.RoomDetailsVM
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoomDetailsScreen(
    viewModel: RoomDetailsVM = koinViewModel(),
    onAddContractClick: (String) -> Unit,
    onEditRoomOnClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    fun editTextUI(text : String) : String {
        return text
            .lowercase()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
    }

    val room by viewModel.room.collectAsState()
    val contracts by viewModel.contract.collectAsState()
    val tenants by viewModel.tenants.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDeleteOnClicked by viewModel.isDeleteOnClicked.collectAsState()
    val context : Context = LocalContext.current
    Log.d("RoomDetailsScreen", "contractInRoomDetailsScreen: ${contracts.size}")
    if (contracts.isNotEmpty()) {
        Log.d("RoomDetailsScreen", "First contract number: ${contracts[0]}")
    }
    LaunchedEffect(Unit) {
        viewModel.loadRoom()
        viewModel.getRoomContract()
        viewModel.getRoomTenants()
    }
    Log.d("RoomDetailsScreen", "buildingID: ${room?.buildingId}")
    if(isLoading){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()  // Import từ material3
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                DividerWithSubhead(subhead = "Room information")
                CardComponent(
                    infoMap = mapOf(
                        "Status:" to editTextUI(room?.status.toString()),
                        "Floor:" to room?.floor.toString(),
                        "Size:" to room?.size.toString(),
                        "Rental fee:" to formatCurrency(room?.rentalFee.toString()),
                        "Interior:" to editTextUI(room?.interior.toString()),
                    )
                )
            }
            item {
                DividerWithSubhead(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    subhead = "Contract"
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val contract = contracts.firstOrNull()
                    if (contract != null) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Contract# ${contract.contractNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            supportingContent = {
                                Column {
                                    Text(
                                        text = room?.roomNumber.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${contract.startDate} - ${contract.endDate}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "This room has no contract yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = {onAddContractClick(room?.id.toString())}) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Add contract here")
                                }
                            }
                        }
                    }
                }
            }
            item {
                DividerWithSubhead(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    subhead = "Service"
                )
                val services = room?.extraService ?: emptyList()
                if (services.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            services.forEach { service ->
                                ServiceCard(
                                    service = service,
                                    onEdit = {},
                                    onDelete = {}
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No services available",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                DividerWithSubhead(
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    subhead = "Tenants"
                )
                if (tenants.isNotEmpty()) {
                    TenantsSection(
                        tenants = tenants,
                        onAddTenantClick = {},
                        showAddButton = false
                    )
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "There are no tenants in this room.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalAlignment = Alignment.Top
                ) {
                    ButtonComponent(
                        text = "Edit",
                        onButtonClick = { onEditRoomOnClick(room?.id?: "") }
                    )
                    ButtonComponent(
                        text = "Delete",
                        onButtonClick = viewModel::onDeleteClick
                    )
                    if (isDeleteOnClicked) {
                        AlertDialog(
                            onDismissRequest = viewModel::onCancelDelete,
                            title = { Text("Confirm Delete") },
                            text = { Text("Are you sure you want to delete this room?") },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.deleteRoom()
                                    Toast.makeText(
                                        context,
                                        "Room ${room?.roomNumber} deleted successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onBackClick()
                                }) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    viewModel.onCancelDelete()
                                }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

