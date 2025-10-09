package com.example.baytro.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screens(val route: String, val title: String) {
    companion object {
        const val ARG_ID = "id"
        const val ARG_CONTRACT_ID = "contractId"
        const val ARG_BUILDING_ID = "buildingId"
        const val ARG_ROOM_ID = "roomId"
    }

    object SplashScreen : Screens("splash_screen", "")
    object SignIn : Screens("sign_in_screen", "Sign In")
    object SignUp : Screens("sign_up_screen", "Sign Up")
    object SignOut : Screens("sign_in_screen", "Sign Out")
    object PersonalInformation : Screens("personal_information_screen", "Personal Information")
    object EditPersonalInformation : Screens("edit_personal_information", "Edit Personal Information")
    object ChangePassword : Screens("change_password_screen", "Change Password")
    object ForgotPassword : Screens("forgot_password_screen", "Forgot Password")
    object MainScreen : Screens("main_screen", "BayTro")
    object Dashboard : Screens("dashboard_screen", "Dashboard")
    object TenantDashboard : Screens("tenant_dashboard", "Dashboard")
    object BuildingList : Screens("buildings_screen", "Buildings")
    object BuildingAdd : Screens("building_add_screen", "Add Building")
    object TenantList : Screens("tenants_screen", "Tenants")
    object BillList : Screens("bills_screen", "Bills")
    object ContractList : Screens("contracts_screen", "Contracts")
    object MaintenanceRequestList : Screens("maintenance_screen", "Maintenance")
    object ServiceList : Screens("services_screen", "Services")
    object AddService : Screens("add_service_screen", "Add Service")
    object NewLandlordUser : Screens("new_landlord_user_screen", "New Landlord")
    object NewTenantUser : Screens("new_tenant_user_screen", "New Tenant")
    object AddContract : Screens("add_contract_screen", "Add Contract")
    object UploadIdCard : Screens("upload_id_card_screen", "Upload ID Card")
    object TenantEmptyContract : Screens("tenant_empty_contract_screen", "Contract")


    // =================================================================
    // SCREEN WITH ARGUMENTS
    // =================================================================
    object BuildingEdit : Screens("building_edit_screen/{$ARG_BUILDING_ID}", "Edit Building") {
        val arguments = listOf(
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType }
        )
        fun createRoute(buildingId: String) = "building_edit_screen/$ARG_BUILDING_ID"
    }

    object ContractDetails : Screens("contract_details_screen/{$ARG_CONTRACT_ID}", "Contract Details") {
        val arguments = listOf(
            navArgument(ARG_CONTRACT_ID) { type = NavType.StringType }
        )
        fun createRoute(contractId: String) = "contract_details_screen/$contractId"
    }

    object RoomList : Screens("room_screen/{$ARG_BUILDING_ID}", "Rooms") {
        val arguments = listOf(
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType }
        )
        fun createRoute(buildingId: String) = "room_screen/$buildingId"
    }

    object RoomDetails : Screens("room_details/{$ARG_ROOM_ID}", "Room Details") {
        val arguments = listOf(
            navArgument(ARG_ROOM_ID) { type = NavType.StringType }
        )
        fun createRoute(roomId: String) = "room_details/$roomId"
    }

    object EditRoom : Screens("edit_room/{$ARG_ROOM_ID}", "Edit Room") {
        val arguments = listOf(
            navArgument(ARG_ROOM_ID) { type = NavType.StringType; nullable = true }
        )
        fun createRoute(roomId: String?) = "edit_room/$roomId"
    }

    object AddRoom : Screens("addRoom_screen/{$ARG_BUILDING_ID}", "Add Room") {
        val arguments = listOf(
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType; nullable = true }
        )
        fun createRoute(buildingId: String?) = "addRoom_screen/$buildingId"
    }
}