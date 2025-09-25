package com.example.baytro.navigation

sealed class Screens (val route : String) {
    object Dashboard : Screens("dashboard_screen")
    object PropertyList : Screens("properties_screen")
    object TenantList : Screens("tenants_screen")
    object BillList : Screens("bills_screen")
    object ContractList : Screens("contracts_screen")
    object MaintenanceRequestList : Screens("maintenance_screen")
    object SignIn : Screens("sign_in_screen")
    object SignUp : Screens("sign_up_screen")
    object MainScreen : Screens("main_screen")
}