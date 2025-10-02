package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TabRowComponent(
    tabItemList: List<Pair<String, ImageVector>>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.background(Color.White)
        ) {
            tabItemList.forEachIndexed { index, tabItem ->
                LeadingIconTab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = tabItem.first,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = tabItem.second,
                            contentDescription = tabItem.first
                        )
                    }
                )
            }
        }
    }
}
