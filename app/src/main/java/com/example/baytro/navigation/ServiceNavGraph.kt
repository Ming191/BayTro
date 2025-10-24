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

    composable(
        Screens.AddService.route,
        arguments = Screens.AddService.arguments
    ) {
        AddServiceScreen(navController = navController)
    }

    composable(
        route = Screens.EditService.route,
        arguments = Screens.EditService.arguments
    ) {
        EditServiceScreen(navController = navController)
    }
}

