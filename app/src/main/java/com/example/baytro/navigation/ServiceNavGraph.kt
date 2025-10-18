package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.service.ServiceListScreen
import com.example.baytro.view.screens.service.AddServiceScreen

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
}

