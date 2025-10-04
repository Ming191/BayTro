package com.example.baytro.view.screens.room

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
    Log.d("RoomDetailsScreen", "RoomID: ${room?.buildingName}")
    Column (
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        DividerWithSubhead("Room information", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        CardComponent(
            infoMap = mapOf(
                "Status" to room?.status.toString(),
                "Floor" to room?.floor.toString(),
                "Size" to room?.size.toString(),
                "Rental fee" to formatCurrency(room?.rentalFee.toString()),
                "Interior" to room?.interior.toString()
            )
        )
        DividerWithSubhead("Contract", Modifier.padding(16.dp))
        Card {
            Text("contract")
        }
        DividerWithSubhead("Service", Modifier.padding(16.dp))
        DividerWithSubhead("Tenants", Modifier.padding(16.dp))

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

