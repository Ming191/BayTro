package com.example.baytro

import android.content.res.Configuration
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.baytro.navigation.AppScaffold
import com.example.baytro.navigation.NavigationType
import com.example.baytro.view.navigationType.NavigationDrawerView
import com.example.baytro.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, name = "Light")
@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navController = rememberNavController()
    AppTheme {
        val navigationType: NavigationType = NavigationType.NavigationDrawer
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    NavigationDrawerView (
                        onDrawerClicked = {
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onPropertyClicked = {
                            navController.navigate("buildings_screen")
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onTenantClicked = {
                            navController.navigate("tenants_screen")
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onMaintenanceClicked = {
                            navController.navigate("maintenance_screen")
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onDashboardClicked = {
                            navController.navigate("dashboard_screen")
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onBillClicked = {
                            navController.navigate("bills_screen")
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        onContractClicked = {
                            navController.navigate("contracts_screen")
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
                navigationType = navigationType,
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