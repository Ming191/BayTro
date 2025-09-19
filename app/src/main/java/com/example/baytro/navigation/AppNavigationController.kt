package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.baytro.MainScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.ContractListScreen
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.PropertyListScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.auth.SignInScreen
import com.example.baytro.view.screens.auth.SignUpScreen

@Composable
fun AppNavigationController(
    navHostController: NavHostController,
    startDestination: String
) {
    NavHost (
        navController = navHostController,
        startDestination = startDestination
    ) {
        composable(
            Screens.PropertyList.route
        ) {
            PropertyListScreen()
        }
        composable(
            Screens.TenantList.route
        ) {
            TenantListScreen()
        }
        composable(
            Screens.Dashboard.route
        ) {
            DashboardScreen()
        }
        composable(
            Screens.MaintenanceRequestList.route
        ) {
            MaintenanceScreen()
        }
        composable(
            Screens.BillList.route
        ) {
            BillListScreen()
        }
        composable(
            Screens.ContractList.route
        ) {
            ContractListScreen()
        }
        composable (
            Screens.MainScreen.route
        ) {
            MainScreen()
        }
        composable(
            Screens.SignIn.route
        ) {
            SignInScreen(
                onSignInSuccess = {
                    navHostController.navigate(Screens.MainScreen.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToSignUp = {
                    navHostController.navigate(Screens.SignUp.route) {
                        popUpTo(Screens.SignIn.route) {
                            inclusive = false
                        }
                    }
                }
            )
        }
        composable (
            Screens.SignUp.route
        ) {
            SignUpScreen(
                onNavigateToSignIn = {
                    navHostController.popBackStack()
                }
            )
        }
    }
}