package com.example.baytro.navigation

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.auth.ChangePasswordScreen
import com.example.baytro.view.screens.auth.EditPersonalInformationScreen
import com.example.baytro.view.screens.profile.ProfileScreen
import com.example.baytro.view.screens.profile.PoliciesAndTermsScreen
import com.example.baytro.view.screens.profile.ViewPersonalInformationScreen
import com.example.baytro.view.screens.tenant.TenantInfoScreen
import com.google.firebase.auth.FirebaseAuth

fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
    composable(Screens.PersonalInformation.route) {
        ProfileScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToChangePassword = {
                navController.navigate(Screens.ChangePassword.route)
            },
            onNavigateToSignOut = {
                FirebaseAuth.getInstance().signOut()
                Log.d("AuthNAvGraph", "User : ${FirebaseAuth.getInstance().currentUser}")
            },
            onNavigateToEditPersonalInformation = {
                navController.navigate(Screens.ViewPersonalInformation.route)
            },
            onNavigateToPoliciesAndTerms = {
                navController.navigate(Screens.PoliciesAndTerms.route)
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

    composable(Screens.TenantInfo.route) {
        TenantInfoScreen()
    }
}