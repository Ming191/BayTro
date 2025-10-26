package com.example.baytro.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screens(val route: String, val title: String) {
    companion object {
        const val ARG_ID = "userId"
        const val ARG_CONTRACT_ID = "contractId"
        const val ARG_BUILDING_ID = "buildingId"
        const val ARG_ROOM_ID = "roomId"
        const val ARG_REQUEST_ID = "requestId"
        const val ARG_LANDLORD_ID = "landlordId"

        const val ARG_IS_FROM_ADD_SCREEN = "isFromAddScreen"
        const val ARG_TENANT_ID = "tenantId"
        const val ARG_BILL_ID = "billId"
        const val ARG_ROOM_NAME = "roomName"
        const val ARG_BUILDING_NAME = "buildingName"

        const val ARG_SERVICE_ID = "serviceId"
    }

    object SplashScreen : Screens("splash_screen", "")
    object SignIn : Screens("sign_in_screen", "Sign In")
    object SignUp : Screens("sign_up_screen", "Sign Up")
    object SignOut : Screens("sign_in_screen", "Sign Out")
    object PersonalInformation : Screens("personal_information_screen", "Personal Information")
    object ViewPersonalInformation : Screens("view_personal_information_screen", "View Personal Information")
    object EditPersonalInformation : Screens("edit_personal_information_screen", "Edit Personal Information")
    object ChangePassword : Screens("change_password_screen", "Change Password")
    object PoliciesAndTerms : Screens("policies_and_terms_screen", "Policies and Terms")
    object PaymentSettings : Screens("payment_settings_screen", "Payment Settings")
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
    object AddService : Screens("add_service_screen/{$ARG_ROOM_ID}/{$ARG_BUILDING_ID}/{$ARG_IS_FROM_ADD_SCREEN}", "Add Service") {
        val arguments = listOf(
            navArgument(ARG_ROOM_ID) { type = NavType.StringType },
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType },
            navArgument(ARG_IS_FROM_ADD_SCREEN) { type = NavType.BoolType; defaultValue = false }

        )
        fun createRoute(roomId: String, buildingId: String, isFromAddScreen: Boolean) = "add_service_screen/$roomId/$buildingId/$isFromAddScreen"
    }

    object EditService : Screens(
        "edit_service_screen/{${ARG_SERVICE_ID}}?${ARG_BUILDING_ID}={${ARG_BUILDING_ID}}&${ARG_ROOM_ID}={${ARG_ROOM_ID}}",
        "Edit Service"
    ) {
        val arguments = listOf(
            navArgument(ARG_BUILDING_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null },
            navArgument(ARG_ROOM_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(ARG_SERVICE_ID) { type = NavType.StringType }
        )

        fun createRoute(buildingId: String, serviceId: String): String {
            return "edit_service_screen/$serviceId?${ARG_BUILDING_ID}=$buildingId"
        }
        //overload used for nav from room
        fun createRouteFromRoom(roomId: String, serviceId: String): String {
            return "edit_service_screen/$serviceId?${ARG_ROOM_ID}=$roomId"
        }
    }
    object NewLandlordUser : Screens("new_landlord_user_screen", "New Landlord")
    object NewTenantUser : Screens("new_tenant_user_screen", "New Tenant")
    object AddContract : Screens("add_contract_screen/{$ARG_ROOM_ID}", "Add Contract") {
        val arguments = listOf(
            navArgument(ARG_ROOM_ID) { type = NavType.StringType }
        )
        fun createRoute(roomId: String) = "add_contract_screen/$roomId"
    }
    object UploadIdCard : Screens("upload_id_card_screen", "Upload ID Card")
    object TenantEmptyContract : Screens("tenant_empty_contract_screen", "Contract")
    object ImportBuildingsRooms : Screens("import_buildings_rooms", "Import Buildings & Rooms")


    // =================================================================
    // SCREEN WITH ARGUMENTS
    // =================================================================
    object MeterReading : Screens("meter_reading_screen/{$ARG_CONTRACT_ID}/{$ARG_ROOM_ID}/{$ARG_BUILDING_ID}/{$ARG_LANDLORD_ID}/{$ARG_ROOM_NAME}/{$ARG_BUILDING_NAME}", "Meter Reading") {
        val arguments = listOf(
            navArgument(ARG_CONTRACT_ID) { type = NavType.StringType },
            navArgument(ARG_ROOM_ID) { type = NavType.StringType },
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType },
            navArgument(ARG_LANDLORD_ID) { type = NavType.StringType },
            navArgument(ARG_ROOM_NAME) { type = NavType.StringType },
            navArgument(ARG_BUILDING_NAME) { type = NavType.StringType }
        )
        fun createRoute(contractId: String, roomId: String, buildingId: String, landlordId: String, roomName: String, buildingName: String) =
            "meter_reading_screen/$contractId/$roomId/$buildingId/$landlordId/$roomName/$buildingName"
    }

    object PendingMeterReadings : Screens("pending_meter_readings_screen", "Pending Readings")
    object MeterReadingHistory : Screens("meter_reading_history/{$ARG_CONTRACT_ID}", "Reading History") {
        val arguments = listOf(
            navArgument(ARG_CONTRACT_ID) { type = NavType.StringType }
        )
        fun createRoute(contractId: String) = "meter_reading_history/$contractId"
    }

    object BuildingEdit : Screens("building_edit_screen/{$ARG_BUILDING_ID}", "Edit Building") {
        val arguments = listOf(
            navArgument(ARG_BUILDING_ID) { type = NavType.StringType }
        )
        fun createRoute(buildingId: String) = "building_edit_screen/$buildingId"
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

    object EditContract : Screens("contract_edit/{$ARG_CONTRACT_ID}", "Edit Contract") {
        val arguments = listOf(
            navArgument(ARG_CONTRACT_ID) { type = NavType.StringType; nullable = true }
        )
        fun createRoute(contractId: String?) = "contract_edit/$contractId"
    }

    object AddRequest : Screens("add_request_screen", "Add Request")
    object UpdateRequest : Screens("update_request_screen/{$ARG_REQUEST_ID}", "Update Request") {
        val arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        )
        fun createRoute(requestId: String) = "update_request_screen/$requestId"
    }
    object AssignRequest : Screens("assign_request_screen/{$ARG_REQUEST_ID}", "Assign Request") {
        val arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        )
        fun createRoute(requestId: String) = "assign_request_screen/$requestId"
    }

    // =================================================================
    // BILLING SCREENS
    // =================================================================
    object TenantBillScreen : Screens("tenant_bill_screen", "My Bill")
    object Settings : Screens("settings_screen", "Settings")
    object LandlordBills : Screens("landlord_bills/{$ARG_LANDLORD_ID}", "Bills Dashboard") {
        val arguments = listOf(
            navArgument(ARG_LANDLORD_ID) { type = NavType.StringType }
        )
        fun createRoute(landlordId: String) = "landlord_bills/$landlordId"
    }

    object TenantBill : Screens("tenant_bill/{$ARG_TENANT_ID}", "My Bill") {
        val arguments = listOf(
            navArgument(ARG_TENANT_ID) { type = NavType.StringType }
        )
        fun createRoute(tenantId: String) = "tenant_bill/$tenantId"
    }

    object BillDetails : Screens("bill_details/{$ARG_BILL_ID}", "Bill Details") {
        val arguments = listOf(
            navArgument(ARG_BILL_ID) { type = NavType.StringType }
        )
        fun createRoute(billId: String) = "bill_details/$billId"
    }

    object BillingHistory : Screens("billing_history/{$ARG_TENANT_ID}", "Billing History") {
        val arguments = listOf(
            navArgument(ARG_TENANT_ID) { type = NavType.StringType }
        )
        fun createRoute(tenantId: String) = "billing_history/$tenantId"
    }
    object Chatbot : Screens("chatbot_screen", "Trợ lý Pháp luật")

    object TenantInfo : Screens("tenant_info_screen/{$ARG_TENANT_ID}", "Tenant Info") {
        val arguments = listOf(
            navArgument(ARG_TENANT_ID) { type = NavType.StringType }
        )
        fun createRoute(tenantId: String) = "tenant_info_screen/$tenantId"
    }
}