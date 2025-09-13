package com.example.baytro.view.navigationType

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
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
    onDrawerClicked: () -> Unit,
    onDashboardClicked: () -> Unit,
    onPropertyClicked: () -> Unit,
    onTenantClicked: () -> Unit,
    onMaintenanceClicked: () -> Unit,
    onBillClicked: () -> Unit,
    onContractClicked: () -> Unit,
) {
    val items = listOf(
        Screens.PropertyList,
        Screens.TenantList
    )
    val selectedItem = remember { mutableStateOf(items[0]) }

    Column (
        modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
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
                style = MaterialTheme.typography.headlineLarge,
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

        NavigationDrawerItem(
            label = { Text(text = "Properties") },
            selected = selectedItem.value == Screens.PropertyList,
            onClick = {
                onPropertyClicked()
                selectedItem.value = Screens.PropertyList
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
    }
}