package com.example.baytro.navigation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.example.baytro.utils.LocalAvatarCache
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold (
    navHostController: NavHostController,
    onDrawerClicked: () -> Unit
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
                    Screens.LandlordBills.route,
                )
                val isTopLevel = currentRoute in topLevelRoutes
                val titleText = when (currentRoute) {
                    Screens.PersonalInformation.route -> "User Profile"
                    Screens.ViewPersonalInformation.route -> "View Personal Information"
                    Screens.EditPersonalInformation.route -> "Edit Personal Information"
                    Screens.ChangePassword.route -> "Change Password"
                    Screens.PoliciesAndTerms.route -> "Policies & Terms"
                    Screens.BuildingList.route -> "Buildings"
                    Screens.BuildingAdd.route -> "Add Building"
                    Screens.BuildingEdit.route -> "Edit Building"
                    Screens.TenantList.route -> "Tenants"
                    Screens.BillList.route -> "Bills"
                    Screens.ContractList.route -> "Contracts"
                    Screens.MaintenanceRequestList.route -> "Maintenance"
                    Screens.Dashboard.route -> "BayTro"
                    Screens.ServiceList.route -> "Services"
                    Screens.PendingMeterReadings.route -> "Meter Readings"
                    Screens.LandlordBills.route -> "Bills Dashboard"
                    Screens.TenantBill.route -> "My Bill"
                    Screens.BillDetails.route -> "Bill Details"
                    Screens.BillingHistory.route -> "Billing History"
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
                    },
                    actions = {
                        if (isTopLevel && currentRoute != Screens.PersonalInformation.route) {
                            val avatarCache = LocalAvatarCache.current
                            val uid = FirebaseAuth.getInstance().currentUser?.uid

                            LaunchedEffect(uid) {
                                if (uid != null) {
                                    avatarCache.loadAvatar(uid)
                                } else {
                                    avatarCache.clearCache()
                                }
                            }

                            IconButton(onClick = { navHostController.navigate(Screens.PersonalInformation.route) }) {
                                val url = avatarCache.avatarUrl
                                if (!url.isNullOrBlank()) {
                                    val context = LocalContext.current
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(context)
                                                .data(url)
                                                .crossfade(false)
                                                .build()
                                        ),
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
