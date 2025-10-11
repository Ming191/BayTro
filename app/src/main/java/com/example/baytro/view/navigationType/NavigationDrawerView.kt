package com.example.baytro.view.navigationType

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.baytro.navigation.Screens

@Composable
fun NavigationDrawerView(
    currentRoute: String? = null,
    onDrawerClicked: () -> Unit,
    onDashboardClicked: () -> Unit,
    onPropertyClicked: () -> Unit,
    onTenantClicked: () -> Unit,
    onMaintenanceClicked: () -> Unit,
    onBillClicked: () -> Unit,
    onContractClicked: () -> Unit,
    onServiceClicked: () -> Unit,
    onPersonalInformationClicked: () -> Unit
) {
    val selectedItem = remember(currentRoute) {
        mutableStateOf(
            when {
                currentRoute == Screens.Dashboard.route -> Screens.Dashboard
                currentRoute == Screens.BuildingList.route || currentRoute?.startsWith("building_") == true -> Screens.BuildingList
                currentRoute == Screens.TenantList.route -> Screens.TenantList
                currentRoute == Screens.MaintenanceRequestList.route -> Screens.MaintenanceRequestList
                currentRoute == Screens.BillList.route -> Screens.BillList
                currentRoute == Screens.ContractList.route -> Screens.ContractList
                currentRoute == Screens.PersonalInformation.route -> Screens.PersonalInformation
                else -> Screens.Dashboard
            }
        )
    }

    BoxWithConstraints {
        val drawerWidth = maxWidth * 0.7f

        Column (
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .background(MaterialTheme.colorScheme.inverseOnSurface)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        onDrawerClicked()
                    },
                ) {
                    Icon (
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Close Menu",
                    )
                }
            }
            NavigationDrawerItem(
                label = { Text(text = "Dashboard") },
                selected = selectedItem.value == Screens.Dashboard,
                onClick = {
                    onDashboardClicked()
                    selectedItem.value = Screens.Dashboard
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            // ... các NavigationDrawerItem khác
            NavigationDrawerItem(
                label = { Text(text = "Buildings") },
                selected = selectedItem.value == Screens.BuildingList,
                onClick = {
                    onPropertyClicked()
                    selectedItem.value = Screens.BuildingList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Tenants") },
                selected = selectedItem.value == Screens.TenantList,
                onClick = {
                    onTenantClicked()
                    selectedItem.value = Screens.TenantList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Maintenance") },
                selected = selectedItem.value == Screens.MaintenanceRequestList,
                onClick = {
                    onMaintenanceClicked()
                    selectedItem.value = Screens.MaintenanceRequestList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Bills") },
                selected = selectedItem.value == Screens.BillList,
                onClick = {
                    onBillClicked()
                    selectedItem.value = Screens.BillList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Contracts") },
                selected = selectedItem.value == Screens.ContractList,
                onClick = {
                    onContractClicked()
                    selectedItem.value = Screens.ContractList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Services") },
                selected = selectedItem.value == Screens.ServiceList,
                onClick = {
                    onServiceClicked()
                    selectedItem.value = Screens.ServiceList
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
            NavigationDrawerItem(
                label = { Text(text = "Personal Information") },
                selected = selectedItem.value == Screens.PersonalInformation,
                onClick = {
                    onPersonalInformationClicked()
                    selectedItem.value = Screens.PersonalInformation
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}