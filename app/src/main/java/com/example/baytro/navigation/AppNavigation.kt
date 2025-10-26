package com.example.baytro.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.baytro.data.user.UserRoleState
import com.example.baytro.view.components.ExitConfirmationDialog

@Composable
fun AppNavigation(
    startDestination: String = Screens.SignIn.route,
    onExit : () -> Unit
) {

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitConfirmationDialog(
            onConfirm = onExit,
            onDismiss = { showExitDialog = false }
        )
    }

    val navController = rememberNavController()

    val authState = LocalAuthState.current
    val userRole by UserRoleState.userRole.collectAsState()

    LaunchedEffect(authState.currentUser, userRole) {
        when {
            authState.currentUser == null &&
                    navController.currentDestination?.route != Screens.SignIn.route -> {
                navController.navigate(Screens.SignIn.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            authState.currentUser != null &&
                    userRole != null &&
                    navController.currentDestination?.route != Screens.MainScreen.route -> {
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