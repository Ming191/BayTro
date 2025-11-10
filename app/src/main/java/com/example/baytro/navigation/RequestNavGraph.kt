package com.example.baytro.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.request.AddRequestScreen
import com.example.baytro.view.screens.request.AssigningScreen
import com.example.baytro.view.screens.request.UpdateRequestScreen

fun NavGraphBuilder.requestNavGraph(navController: NavHostController) {
    composable(Screens.AddRequest.route) {
        AddRequestScreen(
            onNavigateBack = { requestAdded ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("request_added", requestAdded)
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screens.UpdateRequest.route,
        arguments = Screens.UpdateRequest.arguments
    ) { backStackEntry ->
        val requestId = backStackEntry.arguments?.getString(Screens.ARG_REQUEST_ID) ?: ""
        UpdateRequestScreen(
            requestId = requestId,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screens.AssignRequest.route,
        arguments = Screens.AssignRequest.arguments
    ) { backStackEntry ->
        val requestId = backStackEntry.arguments?.getString(Screens.ARG_REQUEST_ID) ?: ""
        AssigningScreen(
            requestId = requestId,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
