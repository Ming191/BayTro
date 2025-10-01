package com.example.baytro.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.baytro.MainScreen
import com.example.baytro.view.screens.AddBuildingScreen
import com.example.baytro.view.screens.BillListScreen
import com.example.baytro.view.screens.BuildingListScreen
import com.example.baytro.view.screens.ContractListScreen
import com.example.baytro.view.screens.DashboardScreen
import com.example.baytro.view.screens.MaintenanceScreen
import com.example.baytro.view.screens.TenantListScreen
import com.example.baytro.view.screens.auth.SignInScreen
import com.example.baytro.view.screens.auth.SignUpScreen
import com.example.baytro.view.screens.room.AddRoomScreen
import com.example.baytro.view.screens.room.EditRoomScreen
import com.example.baytro.view.screens.room.RoomListScreen
import com.example.baytro.view.screens.splash.NewLandlordUserScreen
import com.example.baytro.view.screens.splash.SplashScreen

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
            BuildingListScreen(
                navController = navHostController,
                onViewBuilding = { buildingName ->
                    Log.d("DEBUG", "Navigate with buildingId=$buildingName")
                    navHostController.navigate(Screens.RoomList.createRoute(buildingName))
                }
            )
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
        composable(
            route = Screens.RoomList.route, // route có {buildingName} bên trong
            arguments = listOf(navArgument("buildingName") { type = NavType.StringType})
        ) { entry ->
            // Lấy tham số từ navigation
            val buildingName = entry.arguments?.getString("buildingName") ?: ""
            Log.d("DEBUG", "RoomListScreen received buildingName=$buildingName")
            // Gọi screen, truyền buildingName vào
            RoomListScreen(
                navController = navHostController,
                buildingId = buildingName
            )
        }
        composable(
            Screens.EditRoom.route
        ) {
            EditRoomScreen()
        }
        composable(
            Screens.AddRoom.route
        ) {
            AddRoomScreen(navHostController)
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
                navigateToTenantLogin = {},
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
    }
}