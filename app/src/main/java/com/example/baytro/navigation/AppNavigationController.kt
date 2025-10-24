package com.example.baytro.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.baytro.MainScreen

@SuppressLint("RestrictedApi")
@Composable
fun AppNavigationController(
    navHostController: NavHostController,
    startDestination: String
) {
    LaunchedEffect(Unit) {
        navHostController.addOnDestinationChangedListener { controller, _, _ ->
            val routes = controller
                .currentBackStack.value.joinToString(", ") {
                    it.destination.route ?: it.destination.id.toString()
                }

            Log.d("BackStackLog", "BackStack: $routes")
        }
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination
    ) {
        // Authentication flow
        authNavGraph(navHostController)

        // Splash/Onboarding flow
        splashNavGraph(navHostController)

        // Building management
        buildingNavGraph(navHostController)

        // Contract management
        contractNavGraph(navHostController)

        // Room management
        roomNavGraph(navHostController)

        // Service management
        serviceNavGraph(navHostController)

        // Dashboard and main features
        dashboardNavGraph(navHostController)

        //Profile manager
        profileNavGraph(navHostController)

        // Request management
        requestNavGraph(navHostController)

        // Billing management
        billingNavGraph(navHostController)

        // Main screen
        composable(Screens.MainScreen.route) { MainScreen() }
    }
}