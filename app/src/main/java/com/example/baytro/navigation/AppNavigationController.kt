package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.baytro.MainScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.BuildingListScreen
import com.example.baytro.view.screens.AddBuildingScreen
import com.example.baytro.view.screens.ContractListScreen
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.auth.SignInScreen
import com.example.baytro.view.screens.auth.SignUpScreen
import com.example.baytro.view.screens.contract.AddContractScreen
import com.example.baytro.view.screens.splash.NewLandlordUserScreen
import com.example.baytro.view.screens.splash.NewTenantUserScreen
import com.example.baytro.view.screens.splash.SplashScreen
import com.example.baytro.view.screens.splash.UploadIdCardScreen

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
            Screens.BuildingList.route
        ) {
            BuildingListScreen(navController = navHostController)
        }
        composable(
            Screens.BuildingAdd.route
        ) {
            AddBuildingScreen(navController = navHostController)
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
                onFirstTimeUser = {
                    navHostController.navigate(Screens.SplashScreen.route) {
                        popUpTo(0) {
                            inclusive = false
                        }
                    }
                },
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
        composable (
            Screens.SplashScreen.route
        ) {
            SplashScreen(
                navigateToLandlordLogin = {
                    navHostController.navigate(Screens.NewLandlordUser.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }

                    }
                },
                navigateToTenantLogin = {
                    navHostController.navigate(Screens.UploadIdCard.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                },
            )
        }

        composable(
            Screens.NewLandlordUser.route
        ) {
            NewLandlordUserScreen(
                onComplete = {
                    navHostController.navigate(Screens.MainScreen.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable (
            Screens.AddContract.route
        ) {
            AddContractScreen()
        }

        composable (
            Screens.UploadIdCard.route
        ) {
            UploadIdCardScreen(
                onNavigateToTenantForm = {
                    navHostController.navigate(Screens.NewTenantUser.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable (
            Screens.NewTenantUser.route
        ) {
            NewTenantUserScreen(
                onComplete = {
                    navHostController.navigate(Screens.MainScreen.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
    }
}