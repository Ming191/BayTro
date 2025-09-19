package com.example.baytro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    AppNavigationController(
        navHostController = navController,
        startDestination = startDestination
    )
}