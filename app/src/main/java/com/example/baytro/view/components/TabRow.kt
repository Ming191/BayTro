package com.example.baytro.view.components

import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun TabRowComponent(
    tabItemList : List<Pair<String, ImageVector>>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            //.padding(start = 16.dp)
            .background(Color.White)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            Modifier.background(Color.White)
        ) {
            tabItemList.forEach() { tabItem ->
                LeadingIconTab(
                    selected = selectedTabIndex == 0,
                    onClick = {onTabSelected(0)},
                    text = { Text(text = tabItem.first, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    icon = {
                        Icon(
                            imageVector = tabItem.second,
                            contentDescription = tabItem.first // Add a valid content description
                        )
                    }
                )
            }
        }
    }
}