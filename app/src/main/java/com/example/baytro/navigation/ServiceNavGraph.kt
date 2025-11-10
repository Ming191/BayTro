package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.service.AddServiceScreen
import com.example.baytro.view.screens.service.EditServiceScreen
import com.example.baytro.view.screens.service.ServiceListScreen

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

