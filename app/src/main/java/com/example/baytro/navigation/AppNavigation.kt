package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.baytro.data.user.UserRoleState

@Composable
fun AppNavigation(startDestination: String = Screens.SignIn.route) {
    val navController = rememberNavController()

    val authState = LocalAuthState.current
    val userRole by UserRoleState.userRole.collectAsState()

    // Xử lý chuyển hướng khi auth state thay đổi
    LaunchedEffect(authState.currentUser, userRole) {
        when {
            authState.currentUser == null &&
                    navController.currentDestination?.route != Screens.SignIn.route -> {
                // User đã sign out, chuyển đến màn hình đăng nhập
                navController.navigate(Screens.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            authState.currentUser != null &&
                    userRole != null &&
                    navController.currentDestination?.route != Screens.MainScreen.route -> {
                // User đã đăng nhập và đã có role, chuyển đến màn hình chính
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