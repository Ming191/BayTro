package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.billing.BillDetailsScreen
import com.example.baytro.view.screens.billing.LandlordBillsScreen
import com.example.baytro.view.screens.billing.TenantBillScreen

fun NavGraphBuilder.billingNavGraph(navController: NavHostController) {

    composable(Screens.TenantBillScreen.route) {
        TenantBillScreen(navController = navController)
    }

    composable(
        route = Screens.LandlordBills.route,
        arguments = Screens.LandlordBills.arguments
    ) { backStackEntry ->
        val landlordId = backStackEntry.arguments?.getString(Screens.ARG_LANDLORD_ID) ?: ""

        LandlordBillsScreen(
            navController = navController,
        )
    }

    composable(
        route = Screens.TenantBill.route,
        arguments = Screens.TenantBill.arguments
    ) { backStackEntry ->
        val tenantId = backStackEntry.arguments?.getString(Screens.ARG_TENANT_ID) ?: ""

        TenantBillScreen(
            navController = navController
        )
    }

    composable(
        route = Screens.BillDetails.route,
        arguments = Screens.BillDetails.arguments
    ) { backStackEntry ->
        BillDetailsScreen(
            navController = navController
        )
    }

    composable(
        route = Screens.BillingHistory.route,
        arguments = Screens.BillingHistory.arguments
    ) { backStackEntry ->
        val tenantId = backStackEntry.arguments?.getString(Screens.ARG_TENANT_ID) ?: ""
        TenantBillScreen(
            navController = navController
        )
    }
}
