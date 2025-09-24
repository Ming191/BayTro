package com.example.baytro.navigation

sealed class Screens (val route : String) {
    object Dashboard : Screens("dashboard_screen")
    object BuildingList : Screens("buildings_screen")
    object BuildingAdd : Screens("building_add_screen")
    object TenantList : Screens("tenants_screen")
    object BillList : Screens("bills_screen")
    object ContractList : Screens("contracts_screen")
    object MaintenanceRequestList : Screens("maintenance_screen")
}