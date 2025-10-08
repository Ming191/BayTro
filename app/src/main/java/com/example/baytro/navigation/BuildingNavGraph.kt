package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.building.BuildingListScreen
import com.example.baytro.view.screens.building.AddBuildingScreen
import com.example.baytro.view.screens.building.EditBuildingScreen

fun NavGraphBuilder.buildingNavGraph(navController: NavHostController) {
    composable(Screens.BuildingList.route) {
        BuildingListScreen(navController = navController)
    }

    composable(Screens.BuildingAdd.route) {
        AddBuildingScreen(navController = navController)
    }

    composable(
        route = Screens.BuildingEdit.route,
        arguments = Screens.BuildingEdit.arguments
    ) { backStackEntry ->
        val buildingId = backStackEntry.arguments?.getString(Screens.ARG_BUILDING_ID) ?: ""
        EditBuildingScreen(navController = navController, buildingId = buildingId)
    }
}

