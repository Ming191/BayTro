package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.dashboard.TenantDashboard
import com.example.baytro.view.screens.contract.TenantEmptyContractView
import com.example.baytro.view.screens.request.RequestListScreen

fun NavGraphBuilder.dashboardNavGraph(navController: NavHostController) {
    composable(Screens.Dashboard.route) {
        DashboardScreen()
    }

    composable(Screens.TenantDashboard.route) {
        TenantDashboard(
            onNavigateToEmptyContract = {
                navController.navigate(Screens.TenantEmptyContract.route) {
                    popUpTo(Screens.TenantDashboard.route) { inclusive = true }
                }
            },
            onNavigateToContractDetails = { contractId ->
                navController.navigate("contract_details_screen/$contractId")
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
        BillListScreen()
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
}
