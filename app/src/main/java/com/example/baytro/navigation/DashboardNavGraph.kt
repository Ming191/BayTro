package com.example.baytro.navigation

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.SettingsScreen
import com.example.baytro.view.screens.billing.LandlordBillsScreen
import com.example.baytro.view.screens.dashboard.TenantDashboard
import com.example.baytro.view.screens.contract.TenantEmptyContractView
import com.example.baytro.view.screens.request.RequestListScreen
import com.example.baytro.view.screens.dashboard.MeterReadingScreen
import com.example.baytro.view.screens.dashboard.PendingMeterReadingsScreen
import com.example.baytro.view.screens.dashboard.MeterReadingHistoryScreen
import com.google.firebase.auth.FirebaseAuth

fun NavGraphBuilder.dashboardNavGraph(navController: NavHostController) {
    composable(Screens.Dashboard.route) {
        DashboardScreen()
    }

    composable(Screens.Settings.route) {
        SettingsScreen()
    }

    composable(Screens.TenantDashboard.route) {
        TenantDashboard(
            onNavigateToEmptyContract = {
                navController.navigate(Screens.TenantEmptyContract.route) {
                    popUpTo(Screens.TenantDashboard.route) { inclusive = true }
                }
            },
            onNavigateToContractDetails = { contractId ->
                navController.navigate("contract_details_screen/$contractId") {
                    launchSingleTop = true
                }
            },
            onNavigateToRequestList = {
                navController.navigate(Screens.MaintenanceRequestList.route) {
                    popUpTo(Screens.TenantDashboard.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToMeterReading = { contractId, roomId, buildingId, landlordId, roomName, buildingName ->
                navController.navigate(Screens.MeterReading.createRoute(contractId, roomId, buildingId, landlordId, roomName, buildingName)) {
                    launchSingleTop = true
                }
            },
            onNavigateToMeterHistory = { contractId ->
                Log.d("DashboardNavGraph", "onNavigateToMeterHistory called with contractId: $contractId")
                val route = Screens.MeterReadingHistory.createRoute(contractId)
                Log.d("DashboardNavGraph", "Navigating to route: $route")
                navController.navigate(route) {
                    launchSingleTop = true
                }
            },
            onNavigateToPayment = {
                navController.navigate(Screens.TenantBillScreen.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(Screens.TenantEmptyContract.route) {
        TenantEmptyContractView(
            onContractConfirmed = {
                navController.navigate(Screens.TenantDashboard.route) {
                    popUpTo(Screens.TenantEmptyContract.route) { inclusive = true }
                }
            }
        )
    }

    composable(Screens.TenantList.route) {
        TenantListScreen()
    }

    composable(Screens.BillList.route) {
        LandlordBillsScreen(
            navController = navController,
        )
    }

    composable(Screens.MaintenanceRequestList.route) {
        RequestListScreen(
            onAddRequest = {
                navController.navigate(Screens.AddRequest.route)
            },
            onAssignRequest = { requestId ->
                navController.navigate(Screens.AssignRequest.createRoute(requestId))
            },
            onUpdateRequest = { requestId ->
                navController.navigate(Screens.UpdateRequest.createRoute(requestId))
            }
        )
    }

    composable(
        route = Screens.MeterReading.route,
        arguments = Screens.MeterReading.arguments
    ) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString(Screens.ARG_CONTRACT_ID) ?: ""
        val roomId = backStackEntry.arguments?.getString(Screens.ARG_ROOM_ID) ?: ""
        val buildingId = backStackEntry.arguments?.getString(Screens.ARG_BUILDING_ID) ?: ""
        val landlordId = backStackEntry.arguments?.getString(Screens.ARG_LANDLORD_ID) ?: ""
        val roomName = backStackEntry.arguments?.getString(Screens.ARG_ROOM_NAME) ?: ""
        val buildingName = backStackEntry.arguments?.getString(Screens.ARG_BUILDING_NAME) ?: ""

        MeterReadingScreen(
            contractId = contractId,
            roomId = roomId,
            buildingId = buildingId,
            landlordId = landlordId,
            roomName = roomName,
            buildingName = buildingName,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(Screens.PendingMeterReadings.route) {
        PendingMeterReadingsScreen()
    }

    composable(
        route = Screens.MeterReadingHistory.route,
        arguments = Screens.MeterReadingHistory.arguments
    ) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString(Screens.ARG_CONTRACT_ID) ?: ""

        MeterReadingHistoryScreen(
            contractId = contractId
        )
    }
}
