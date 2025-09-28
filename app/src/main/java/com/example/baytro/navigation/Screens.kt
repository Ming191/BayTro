package com.example.baytro.navigation

sealed class Screens (val route : String) {
    object Dashboard : Screens("dashboard_screen")
    object BuildingList : Screens("buildings_screen")
    object BuildingAdd : Screens("building_add_screen")
    object TenantList : Screens("tenants_screen")
    object BillList : Screens("bills_screen")
    object ContractList : Screens("contracts_screen")
    object MaintenanceRequestList : Screens("maintenance_screen")
    object SignIn : Screens("sign_in_screen")
    object SignUp : Screens("sign_up_screen")
    object MainScreen : Screens("main_screen")

    object SplashScreen : Screens("splash_screen")
    object NewLandlordUser : Screens("new_landlord_user_screen")
    object AddContract : Screens("add_contract_screen")

    object UploadIdCard : Screens("upload_id_card_screen")
}