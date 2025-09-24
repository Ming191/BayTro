package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.BuildingListScreen
import com.example.baytro.view.screens.AddBuildingScreen
import com.example.baytro.view.screens.ContractListScreen
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.TenantListScreen

@Composable
fun AppNavigationController(
    contentType: ContentType,
    navHostController: NavHostController,
) {
    NavHost (
        navController = navHostController,
        startDestination = Screens.BuildingList.route
    ) {
        composable(
            Screens.BuildingList.route
        ) {
            BuildingListScreen(navController = navHostController)
        }
        composable(
            Screens.BuildingAdd.route
        ) {
            AddBuildingScreen(navController = navHostController)
        }
        composable(
            Screens.TenantList.route
        ) {
            TenantListScreen()
        }
        composable(
            Screens.Dashboard.route
        ) {
            DashboardScreen()
        }
        composable(
            Screens.MaintenanceRequestList.route
        ) {
            MaintenanceScreen()
        }
        composable(
            Screens.BillList.route
        ) {
            BillListScreen()
        }
        composable(
            Screens.ContractList.route
        ) {
            ContractListScreen()
        }
    }
}