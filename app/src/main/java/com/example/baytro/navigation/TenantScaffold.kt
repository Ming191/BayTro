package com.example.baytro.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
fun TenantScaffold(
    navHostController: NavHostController,
) {
    val currentRoute = navHostController.currentBackStackEntryAsState().value?.destination?.route
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    LaunchedEffect(currentRoute) {
        scrollBehavior.state.heightOffset = 0f
        scrollBehavior.state.contentOffset = 0f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                val titleText = when (currentRoute) {
                    Screens.PersonalInformation.route -> "User profile"
                    Screens.TenantDashboard.route -> "Dashboard"
                    Screens.TenantEmptyContract.route -> "Contract"
                    Screens.ContractDetails.route -> "Contract Details"
                    Screens.UpdateRequest.route -> "Update Request"
                    Screens.MaintenanceRequestList.route -> "Maintenance"
                    Screens.BillList.route -> "Bills"
                    Screens.TenantBillScreen.route -> "My Bill"
                    Screens.MeterReadingHistory.route -> "Reading History"
                    Screens.AddRequest.route -> "New Request"
                    Screens.MeterReading.route -> "Meter Reading"
                    else -> "BayTro"
                }

                CenterAlignedTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = { Text(titleText) },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        scrolledContainerColor = TopAppBarDefaults.topAppBarColors().containerColor
                    ),
                    navigationIcon = {
                        if (navHostController.previousBackStackEntry != null) {
                            IconButton(onClick = { navHostController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        val isMainScreen = currentRoute in listOf(
                            Screens.TenantDashboard.route,
                            Screens.BillList.route,
                            Screens.MaintenanceRequestList.route,
                            Screens.TenantEmptyContract.route
                        )
                        if (isMainScreen && currentRoute != Screens.PersonalInformation.route) {
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
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                AppNavigationController(
                    navHostController = navHostController,
                    startDestination = Screens.TenantDashboard.route
                )
            }
        }
    }
}
