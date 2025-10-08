package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.dashboard.TenantDashboard

fun NavGraphBuilder.dashboardNavGraph(navController: NavHostController) {
    composable(Screens.Dashboard.route) {
        DashboardScreen()
    }

    composable(Screens.TenantDashboard.route) {
        TenantDashboard(navController = navController)
    }

    composable(Screens.TenantList.route) {
        TenantListScreen()
    }

    composable(Screens.BillList.route) {
        BillListScreen()
    }

    composable(Screens.MaintenanceRequestList.route) {
        MaintenanceScreen()
    }
}

