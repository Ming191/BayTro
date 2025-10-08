package com.example.baytro.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.baytro.MainScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.auth.SignInScreen
import com.example.baytro.view.screens.auth.SignUpScreen
import com.example.baytro.view.screens.building.AddBuildingScreen
import com.example.baytro.view.screens.building.BuildingListScreen
import com.example.baytro.view.screens.building.EditBuildingScreen
import com.example.baytro.view.screens.contract.AddContractScreen
import com.example.baytro.view.screens.contract.ContractDetailsScreen
import com.example.baytro.view.screens.contract.ContractListScreen
import com.example.baytro.view.screens.contract.TenantEmptyContractView
import com.example.baytro.view.screens.room.AddRoomScreen
import com.example.baytro.view.screens.room.EditRoomScreen
import com.example.baytro.view.screens.room.RoomDetailsScreen
import com.example.baytro.view.screens.room.RoomListScreen
import com.example.baytro.view.screens.service.AddServiceScreen
import com.example.baytro.view.screens.service.ServiceListScreen
import com.example.baytro.view.screens.splash.NewLandlordUserScreen
import com.example.baytro.view.screens.splash.NewTenantUserScreen
import com.example.baytro.view.screens.splash.SplashScreen
import com.example.baytro.view.screens.splash.UploadIdCardScreen
import com.example.baytro.view.screens.dashboard.TenantDashboard

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
            Screens.BuildingEdit.route
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EditBuildingScreen(navController = navHostController, buildingId = id)
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
            ContractListScreen(
                onContractClick = { contractId ->
                    navHostController.navigate(Screens.ContractDetails.passContractId(contractId))
                }
            )
        }
        composable(
            route = Screens.RoomList.route,
        ) {
            RoomListScreen(navController = navHostController,)
        }
        composable(
            route = Screens.RoomDetails.route,
        ) {
            RoomDetailsScreen(navController = navHostController)
        }
        composable(
            route = Screens.EditRoom.route,
        ) {
            EditRoomScreen(navController = navHostController)
        }
        composable(
            route = Screens.AddRoom.route,
        ) {
            AddRoomScreen(navController = navHostController)
        }
        composable(
            Screens.ServiceList.route
        ) {
            ServiceListScreen(navController = navHostController)
        }
        composable(
            Screens.AddService.route
        ) {
            AddServiceScreen(navController = navHostController)
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
                onTenantWithContract = {
                    navHostController.navigate(Screens.TenantDashboard.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onTenantNoContract = {
                    navHostController.navigate(Screens.TenantEmptyContract.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onTenantPendingSession = {
                    navHostController.navigate(Screens.TenantEmptyContract.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToSignUp = {
                    navHostController.navigate(Screens.SignUp.route)
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
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable (
            Screens.AddContract.route
        ) {
            AddContractScreen(
                navigateToDetails = { contractId ->
                    navHostController.navigate(Screens.ContractDetails.passContractId(contractId)) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                },
            )
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
                    navHostController.navigate(Screens.TenantEmptyContract.route) {
                        popUpTo(navHostController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screens.ContractDetails.route,
            arguments = listOf(
                navArgument("contractId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contractId = backStackEntry.arguments?.getString("contractId") ?: ""
            ContractDetailsScreen(
                contractId = contractId
            )
        }

        composable(
            Screens.TenantEmptyContract.route
        ) {
            TenantEmptyContractView()
        }

        composable(
            Screens.TenantDashboard.route
        ) {
            TenantDashboard(navController = navHostController)
        }
    }
}