package com.example.baytro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.auth.SignInScreen
import com.example.baytro.view.screens.auth.SignUpScreen
import com.example.baytro.view.screens.auth.ForgotPasswordScreen

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    composable(Screens.SignIn.route) {
        SignInScreen(
            onFirstTimeUser = {
                navController.navigate(Screens.SplashScreen.route) {
                    popUpTo(0) { inclusive = false }
                }
            },
            onSignInSuccess = {
                navController.navigate(Screens.MainScreen.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onTenantWithContract = {
                navController.navigate(Screens.TenantDashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onTenantNoContract = {
                navController.navigate(Screens.TenantEmptyContract.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onTenantPendingSession = {
                navController.navigate(Screens.TenantEmptyContract.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToSignUp = {
                navController.navigate(Screens.SignUp.route)
            },
            onNavigateToForgotPassword = {
                navController.navigate(Screens.ForgotPassword.route)
            }
        )
    }

    composable(Screens.SignUp.route) {
        SignUpScreen(
            onNavigateToSignIn = {
                navController.popBackStack()
            }
        )
    }

    composable(Screens.ForgotPassword.route) {
        ForgotPasswordScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToSignIn = {
                navController.popBackStack()
            }
        )
    }
}

