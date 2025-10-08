package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.splash.SplashScreen
import com.example.baytro.view.screens.splash.NewLandlordUserScreen
import com.example.baytro.view.screens.splash.NewTenantUserScreen
import com.example.baytro.view.screens.splash.UploadIdCardScreen

fun NavGraphBuilder.splashNavGraph(navController: NavHostController) {
    composable(Screens.SplashScreen.route) {
        SplashScreen(
            navigateToLandlordLogin = {
                navController.navigate(Screens.NewLandlordUser.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            },
            navigateToTenantLogin = {
                navController.navigate(Screens.UploadIdCard.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }

    composable(Screens.NewLandlordUser.route) {
        NewLandlordUserScreen(
            onComplete = {
                navController.navigate(Screens.MainScreen.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable(Screens.UploadIdCard.route) {
        UploadIdCardScreen(
            onNavigateToTenantForm = {
                navController.navigate(Screens.NewTenantUser.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }

    composable(Screens.NewTenantUser.route) {
        NewTenantUserScreen(
            onComplete = {
                navController.navigate(Screens.TenantEmptyContract.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
}

