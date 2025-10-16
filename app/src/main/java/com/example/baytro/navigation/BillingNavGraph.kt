package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.billing.LandlordBillsScreen
import com.example.baytro.view.screens.billing.TenantBillScreen

fun NavGraphBuilder.billingNavGraph(navController: NavHostController) {

    // Landlord Bills Dashboard
    composable(
        route = Screens.LandlordBills.route,
        arguments = Screens.LandlordBills.arguments
    ) { backStackEntry ->
        val landlordId = backStackEntry.arguments?.getString(Screens.ARG_LANDLORD_ID) ?: ""

        LandlordBillsScreen(
            navController = navController,
        )
    }

    // Tenant Bill View
    composable(
        route = Screens.TenantBill.route,
        arguments = Screens.TenantBill.arguments
    ) { backStackEntry ->
        val tenantId = backStackEntry.arguments?.getString(Screens.ARG_TENANT_ID) ?: ""

        TenantBillScreen(
            navController = navController,
            tenantId = tenantId
        )
    }

    // Bill Details (shared between landlord and tenant)
    composable(
        route = Screens.BillDetails.route,
        arguments = Screens.BillDetails.arguments
    ) { backStackEntry ->
        val billId = backStackEntry.arguments?.getString(Screens.ARG_BILL_ID) ?: ""

        // This will show the bill details - you can create a dedicated screen later
        // For now it navigates to a placeholder
        TenantBillScreen(
            navController = navController,
            tenantId = "" // Bill details can be loaded by billId
        )
    }

    // Billing History for Tenant
    composable(
        route = Screens.BillingHistory.route,
        arguments = Screens.BillingHistory.arguments
    ) { backStackEntry ->
        val tenantId = backStackEntry.arguments?.getString(Screens.ARG_TENANT_ID) ?: ""

        // Placeholder - you can create a BillingHistoryScreen later
        // For now it shows the current bill
        TenantBillScreen(
            navController = navController,
            tenantId = tenantId
        )
    }
}
