package com.example.baytro.view.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val tabData = listOf(
    "Home" to Icons.Filled.Home,
    "Search" to Icons.Filled.Search,
    "Favorites" to Icons.Filled.Favorite,
    "Settings" to Icons.Filled.Settings
)

@Composable
fun Tabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabData: List<Pair<String, ImageVector>>
) {
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        indicator = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        tabData.forEachIndexed { index, (label, icon) ->
            val selected = selectedTabIndex == index
            Tab(
                selected = selected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else unselectedColor
                    )
                },
                text = {
                    Text(
                        text = label,
                        color = if (selected) MaterialTheme.colorScheme.primary else unselectedColor
                    )
                }
            )
        }
    }
}

@Composable
fun HomeScreen() { Text("Home Content") }

@Composable
fun SearchScreen() { Text("Search Content") }

@Composable
fun FavoritesScreen() { Text("Favorites Content") }

@Composable
fun SettingsScreen() { Text("Settings Content") }

@Preview
@Composable
fun TabsPreview() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    Tabs(selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it }, tabData = tabData)
}