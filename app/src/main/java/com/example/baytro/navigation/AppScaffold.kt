package com.example.baytro.navigation

import androidx.compose.animation.AnimatedVisibility
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
fun AppScaffold (
    navigationType: NavigationType,
    navHostController: NavHostController,
    onDrawerClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        AnimatedVisibility(
            visible = navigationType == NavigationType.NavigationRail
        ) {
            TODO("Navigation Rail")
        }
        Scaffold(
            topBar = {
			val currentRoute = navHostController.currentBackStackEntryAsState().value?.destination?.route
			val titleText = when (currentRoute) {
				Screens.BuildingList.route -> "Buildings"
				Screens.BuildingAdd.route -> "Add building"
                Screens.BuildingEdit.route -> "Edit building"
				Screens.TenantList.route -> "Tenants"
				Screens.BillList.route -> "Bills"
				Screens.ContractList.route -> "Contracts"
				Screens.MaintenanceRequestList.route -> "Maintenance"
				Screens.Dashboard.route -> "BayTro"
				else -> "BayTro"
			}
			CenterAlignedTopAppBar(
				title = { Text(titleText) },
                navigationIcon = {
                    val canBack = navHostController.previousBackStackEntry != null
                    if (canBack) {
                        IconButton(onClick = { navHostController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        IconButton(onClick = onDrawerClicked) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
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
            bottomBar = {
                AnimatedVisibility(
                    visible = navigationType == NavigationType.NavigationBottom
                ) {
                    TODO("Bottom Navigation")
                }
            }
        )
    }
}



