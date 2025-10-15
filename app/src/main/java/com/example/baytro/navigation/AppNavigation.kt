package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(startDestination: String = Screens.SignIn.route) {
    val navController = rememberNavController()

    val authState = LocalAuthState.current

    // Xử lý chuyển hướng khi auth state thay đổi
    LaunchedEffect(authState.currentUser) {
        when {
            authState.currentUser == null &&
                    navController.currentDestination?.route != Screens.SignIn.route -> {
                // User đã sign out, chuyển đến màn hình đăng nhập
                navController.navigate(Screens.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            authState.currentUser != null &&
                    navController.currentDestination?.route != Screens.MainScreen.route -> {
                // User đã đăng nhập, chuyển đến màn hình chính
                navController.navigate(Screens.MainScreen.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
    AppNavigationController(
        navHostController = navController,
        startDestination = startDestination
    )
}