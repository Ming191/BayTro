package com.example.baytro.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.AddRequestScreen

fun NavGraphBuilder.requestNavGraph(navController: NavHostController) {
    composable(Screens.AddRequest.route) {
        AddRequestScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}

