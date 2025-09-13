package com.example.baytro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import com.example.baytro.navigation.AppScaffold
import com.example.baytro.navigation.ContentType
import com.example.baytro.navigation.NavigationType
import com.example.baytro.ui.theme.BayTroTheme
import com.example.baytro.view.navigationType.NavigationDrawerView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val navController = rememberNavController()
            BayTroTheme {
                val navigationType: NavigationType = NavigationType.NavigationDrawer
                val contentType: ContentType = ContentType.List
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
                                    navController.navigate("properties_screen")
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
                        contentType = contentType,
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
    }
}