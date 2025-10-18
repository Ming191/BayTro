package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.baytro.view.screens.service.ServiceListScreen
import com.example.baytro.view.screens.service.AddServiceScreen
import com.example.baytro.view.screens.service.EditServiceScreen

fun NavGraphBuilder.serviceNavGraph(navController: NavHostController) {
    composable(Screens.ServiceList.route) {
        ServiceListScreen(navController = navController)
    }

    composable(Screens.AddService.route) {
        AddServiceScreen(navController = navController)
    }

    composable(
        route = "edit_service_screen/{buildingId}/{serviceId}",
        arguments = listOf(
            navArgument("buildingId") { type = NavType.StringType },
            navArgument("serviceId") { type = NavType.StringType }
        )
    ) {
        EditServiceScreen(navController = navController)
    }

}

