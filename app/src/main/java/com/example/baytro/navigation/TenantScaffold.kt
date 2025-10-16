package com.example.baytro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantScaffold(
    navHostController: NavHostController,
    onDrawerClicked: () -> Unit,
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
                        Screens.MeterReadingHistory.route -> "Reading History"
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
                        actions = {
                            IconButton(onClick = { navHostController.navigate(Screens.PersonalInformation.route) }) {
                                val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
                                if (photoUrl != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = photoUrl),
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
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

