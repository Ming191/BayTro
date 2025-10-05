package com.example.baytro.navigation

sealed class Screens (val route : String) {
    object Dashboard : Screens("dashboard_screen")
    object BuildingList : Screens("buildings_screen")
    object BuildingAdd : Screens("building_add_screen")
    object BuildingEdit : Screens("building_edit_screen/{id}")
    object TenantList : Screens("tenants_screen")
    object BillList : Screens("bills_screen")
    object ContractList : Screens("contracts_screen")
    object MaintenanceRequestList : Screens("maintenance_screen")
    object SignIn : Screens("sign_in_screen")
    object SignUp : Screens("sign_up_screen")
    object MainScreen : Screens("main_screen")
    object ServiceList : Screens("services_screen")
    object AddService : Screens("add_service_screen")
    object SplashScreen : Screens("splash_screen")
    object NewLandlordUser : Screens("new_landlord_user_screen")

    object NewTenantUser : Screens("new_tenant_user_screen")
    object AddContract : Screens("add_contract_screen")
    object UploadIdCard : Screens("upload_id_card_screen")
    object TenantEmptyContract : Screens("tenant_empty_contract_screen")
    object ContractDetails : Screens("contract_details_screen/{contractId}") {
        fun passContractId(contractId: String): String {
            return "contract_details_screen/$contractId"
        }
    }
    object RoomList : Screens("room_screen/{buildingId}") {
        fun createRoute(buildingId: String) = "room_screen/$buildingId"
    }
    object RoomDetails : Screens("room_details/{roomId}") {
        fun createRoute(roomId: String) = "room_details/$roomId"
    }
    object EditRoom : Screens("edit_room/{roomId}") {
        fun createRoute(roomId: String?) = "edit_room/$roomId"
    }
    object AddRoom : Screens("addRoom_screen/{buildingId}") {
        fun createRoute(buildingId: String?) = "addRoom_screen/$buildingId"
    }
}