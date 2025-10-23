package com.example.baytro.view.screens

import androidx.compose.runtime.Composable
import com.example.baytro.view.screens.dashboard.LandlordDashboard

/**
 * Wrapper for the Landlord Dashboard screen
 * This delegates to the proper LandlordDashboard component in the dashboard package
 */
@Composable
fun DashboardScreen(
    onNavigateToPendingReadings: () -> Unit = {},
    onNavigateToJoinRequests: () -> Unit = {},
    onNavigateToOverdueBills: () -> Unit = {},
    onNavigateToRevenue: () -> Unit = {}
) {
    LandlordDashboard(
        onNavigateToPendingReadings = onNavigateToPendingReadings,
        onNavigateToJoinRequests = onNavigateToJoinRequests,
        onNavigateToOverdueBills = onNavigateToOverdueBills,
        onNavigateToRevenue = onNavigateToRevenue
    )
}

