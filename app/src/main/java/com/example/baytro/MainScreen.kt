package com.example.baytro

import android.content.res.Configuration
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.baytro.navigation.AppScaffold
import com.example.baytro.navigation.Screens
import com.example.baytro.ui.theme.AppTheme
import com.example.baytro.view.navigationType.NavigationDrawerView
import kotlinx.coroutines.launch

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light")
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    AppTheme {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    NavigationDrawerView (
                        currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                        onDrawerClicked = {
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onPropertyClicked = {
                            navController.navigate(Screens.BuildingList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onTenantClicked = {
                            navController.navigate(Screens.TenantList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onMaintenanceClicked = {
                            navController.navigate(Screens.MaintenanceRequestList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onDashboardClicked = {
                            navController.navigate(Screens.Dashboard.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onBillClicked = {
                            navController.navigate(Screens.BillList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onContractClicked = {
                            navController.navigate(Screens.ContractList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onServiceClicked = {
                            navController.navigate(Screens.ServiceList.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
            },
            drawerState = drawerState
        ) {
            AppScaffold(
                navHostController = navController,
                onDrawerClicked = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )
        }
    }
}