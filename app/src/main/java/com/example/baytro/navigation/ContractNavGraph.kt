package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.contract.AddContractScreen
import com.example.baytro.view.screens.contract.ContractDetailsScreen
import com.example.baytro.view.screens.contract.ContractListScreen
import com.example.baytro.view.screens.contract.EditContractScreen
import com.example.baytro.view.screens.contract.TenantEmptyContractView

fun NavGraphBuilder.contractNavGraph(navController: NavHostController) {
    composable(Screens.ContractList.route) {
        ContractListScreen(
            onContractClick = { contractId ->
                navController.navigate(Screens.ContractDetails.createRoute(contractId))
            }
        )
    }
    composable(
        route = Screens.ContractDetails.route,
        arguments = Screens.ContractDetails.arguments
    ) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString(Screens.ARG_CONTRACT_ID) ?: ""
        ContractDetailsScreen(
            contractId = contractId,
            onEditContract = { contractId ->
                navController.navigate(Screens.EditContract.createRoute(contractId))
            },
            navigateBack = { navController.popBackStack() }
        )
    }
    composable(
        Screens.AddContract.route,
        arguments = Screens.AddContract.arguments
    ) {
        AddContractScreen(
            navigateToDetails = { contractId ->
                navController.navigate(Screens.ContractDetails.createRoute(contractId)) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
    composable(Screens.TenantEmptyContract.route) {
        TenantEmptyContractView(
            onContractConfirmed = {
                navController.navigate(Screens.TenantDashboard.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
    composable(
        Screens.EditContract.route,
        arguments = Screens.EditContract.arguments
    ) { backStackEntry ->
        val contractId = backStackEntry.arguments?.getString(Screens.ARG_CONTRACT_ID) ?: ""
        EditContractScreen(
            navigateBack = { navController.popBackStack() },
            contractId = contractId
        )
    }
}