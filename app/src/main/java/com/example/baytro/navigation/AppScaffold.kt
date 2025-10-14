package com.example.baytro.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold (
    navHostController: NavHostController,
    onDrawerClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                val currentRoute = navHostController.currentBackStackEntryAsState().value?.destination?.route
                val topLevelRoutes = setOf(
                    Screens.Dashboard.route,
                    Screens.BuildingList.route,
                    Screens.TenantList.route,
                    Screens.BillList.route,
                    Screens.ContractList.route,
                    Screens.MaintenanceRequestList.route,
                    Screens.ServiceList.route,
                )
                val isTopLevel = currentRoute in topLevelRoutes
                val titleText = when (currentRoute) {
                    Screens.BuildingList.route -> "Buildings"
                    Screens.BuildingAdd.route -> "Add building"
                    Screens.BuildingEdit.route -> "Edit building"
                    Screens.TenantList.route -> "Tenants"
                    Screens.BillList.route -> "Bills"
                    Screens.ContractList.route -> "Contracts"
                    Screens.MaintenanceRequestList.route -> "Maintenance"
                    Screens.Dashboard.route -> "BayTro"
                    Screens.ServiceList.route -> "Services"
                    else -> "BayTro"
                }
                CenterAlignedTopAppBar(
                    title = { Text(titleText) },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = onDrawerClicked) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navHostController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AppNavigationController(
                        navHostController = navHostController,
                        startDestination = Screens.Dashboard.route
                    )
                }
            },
        )
    }
}





