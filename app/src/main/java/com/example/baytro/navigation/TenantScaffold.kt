package com.example.baytro.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantScaffold(
    navHostController: NavHostController,
    onDrawerClicked: () -> Unit,
) {
    val currentRoute = navHostController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = {
            val titleText = when (currentRoute) {
                Screens.TenantDashboard.route -> "Dashboard"
                Screens.TenantEmptyContract.route -> "Contract"
                Screens.ContractDetails.route -> "Contract Details"
                Screens.AddRequest.route -> "Add Request"
                Screens.UpdateRequest.route -> "Update Request"
                Screens.MaintenanceRequestList.route -> "Maintenance"
                Screens.BillList.route -> "Bills"
                else -> "BayTro"
            }

            CenterAlignedTopAppBar(
                title = { Text(titleText) },
                navigationIcon = {
                    val isMainScreen = currentRoute in listOf(
                        Screens.TenantDashboard.route,
                        Screens.BillList.route,
                        Screens.MaintenanceRequestList.route,
                        Screens.TenantEmptyContract.route
                    )

                    if (!isMainScreen && navHostController.previousBackStackEntry != null) {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onDrawerClicked) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
