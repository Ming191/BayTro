package com.example.baytro.view.screens.room

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.baytro.view.components.ButtonComponent
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.viewModel.Room.RoomDetailsVM
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.baytro.navigation.Screens
import com.example.baytro.view.components.CardComponent
import java.text.NumberFormat
import java.util.Locale


@Composable
fun RoomDetailsScreen(
    navController: NavController,
    viewModel: RoomDetailsVM = koinViewModel(),
) {
    fun formatCurrency(amount: String): String {
        return try {
            val numericAmount = amount.toDoubleOrNull() ?: 0.0
            val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
            formatter.format(numericAmount)
        } catch (e: Exception) {
            amount
        }
    }

    val room by viewModel.room.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadRoom()
    }
    Log.d("RoomDetailsScreen", "RoomID: ${room?.buildingId}")
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        DividerWithSubhead(subhead = "Room information")
        CardComponent(
            infoMap = mapOf(
                "Status" to room?.status.toString(),
                "Floor" to room?.floor.toString(),
                "Size" to room?.size.toString(),
                "Rental fee" to formatCurrency(room?.rentalFee.toString()),
                "Interior" to room?.interior.toString()
            )
        )
        DividerWithSubhead(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp), subhead = "Contract")
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Contract#1234",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = "Room 101-Gay Town",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "4/2025-4/2026",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
        DividerWithSubhead(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp), subhead = "Service")
        DividerWithSubhead(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp), subhead = "Tenants")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top
        ) {
            ButtonComponent(
                text = "Edit",
                onButtonClick = {
                    val roomId = room?.id
                    navController.navigate(Screens.EditRoom.createRoute(roomId))
            })
            ButtonComponent(
                text = "Delete",
                onButtonClick = {
                    val roomId = room?.id
                    navController.navigate(Screens.EditRoom.createRoute(roomId))
            })
        }
    }
}

