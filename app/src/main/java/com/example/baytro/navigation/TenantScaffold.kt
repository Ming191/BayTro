package com.example.baytro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantScaffold(
    navHostController: NavHostController,
) {
    val currentRoute = navHostController.currentBackStackEntryAsState().value?.destination?.route

    val routesWithOwnScaffold = listOf(
        Screens.MeterReading.route,
        Screens.AddRequest.route
    )
    Box(modifier = Modifier.fillMaxSize()) {
        if (currentRoute in routesWithOwnScaffold) {
            AppNavigationController(
                navHostController = navHostController,
                startDestination = Screens.TenantDashboard.route
            )
        } else {
            Scaffold(
                topBar = {
                    val titleText = when (currentRoute) {
                        Screens.TenantDashboard.route -> "Dashboard"
                        Screens.TenantEmptyContract.route -> "Contract"
                        Screens.ContractDetails.route -> "Contract Details"
                        Screens.UpdateRequest.route -> "Update Request"
                        Screens.MaintenanceRequestList.route -> "Maintenance"
                        Screens.BillList.route -> "Bills"
                        Screens.TenantBillScreen.route -> "My Bill"
                        Screens.MeterReadingHistory.route -> "Reading History"
                        else -> "BayTro"
                    }

                    CenterAlignedTopAppBar(
                        title = { Text(titleText) },
                        navigationIcon = {
                            if (navHostController.previousBackStackEntry != null) {
                                IconButton(onClick = { navHostController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppNavigationController(
                        navHostController = navHostController,
                        startDestination = Screens.TenantDashboard.route
                    )
                }
            }
        }
    }
}
