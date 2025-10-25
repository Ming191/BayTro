package com.example.baytro.navigation

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.auth.AuthRepository
import com.example.baytro.view.screens.auth.ChangePasswordScreen
import com.example.baytro.view.screens.auth.EditPersonalInformationScreen
import com.example.baytro.view.screens.profile.PaymentSettingsScreen
import com.example.baytro.view.screens.profile.ProfileScreen
import com.example.baytro.view.screens.profile.PoliciesAndTermsScreen
import com.example.baytro.view.screens.profile.ViewPersonalInformationScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
    composable(Screens.PersonalInformation.route) {
        val authRepository: AuthRepository = koinInject()

        ProfileScreen(
            onNavigateToChangePassword = {
                navController.navigate(Screens.ChangePassword.route)
            },
            onNavigateToSignOut = {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        authRepository.signOut()
                        Log.d("ProfileNavGraph", "User signed out successfully")
                    } catch (e: Exception) {
                        Log.e("ProfileNavGraph", "Error signing out", e)
                    }
                }
            },
            onNavigateToEditPersonalInformation = {
                navController.navigate(Screens.ViewPersonalInformation.route)
            },
            onNavigateToPoliciesAndTerms = {
                navController.navigate(Screens.PoliciesAndTerms.route)
            },
            onNavigateToPaymentSettings = {
                navController.navigate(Screens.PaymentSettings.route)
            }
        )
    }

    composable(Screens.ViewPersonalInformation.route) {
        ViewPersonalInformationScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToEdit = {
                navController.navigate(Screens.EditPersonalInformation.route)
            }
        )
    }

    composable(Screens.EditPersonalInformation.route) {
        EditPersonalInformationScreen (
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(Screens.PoliciesAndTerms.route) {
        PoliciesAndTermsScreen()
    }

    composable(Screens.ChangePassword.route) {
        ChangePasswordScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToSignOut = {
                navController.navigate(Screens.SignOut.route)
            }
        )
    }

    composable(Screens.PaymentSettings.route) {
        PaymentSettingsScreen(
        )
    }
}