package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    hasNextPage: Boolean,
    hasPreviousPage: Boolean,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier,
    onPageClick: ((Int) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            PageButton(
                text = "Back",
                isSelected = false,
                enabled = hasPreviousPage,
                onClick = onPreviousPage
            )

            // Generate page numbers to display
            val pages = generatePageNumbers(currentPage + 1, totalPages)
            
            pages.forEach { pageItem ->
                when (pageItem) {
                    is PageItem.Page -> {
                        PageButton(
                            text = pageItem.number.toString(),
                            isSelected = pageItem.number == currentPage + 1,
                            enabled = true,
                            onClick = { 
                                if (onPageClick != null && pageItem.number != currentPage + 1) {
                                    onPageClick(pageItem.number - 1) // Convert back to 0-based
                                }
                            }
                        )
                    }
                    is PageItem.Ellipsis -> {
                        Text(
                            text = "...",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Next button
            PageButton(
                text = "Next",
                isSelected = false,
                enabled = hasNextPage,
                onClick = onNextPage
            )
        }
    }
}

@Composable
private fun PageButton(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

private sealed class PageItem {
    data class Page(val number: Int) : PageItem()
    object Ellipsis : PageItem()
}

private fun generatePageNumbers(currentPage: Int, totalPages: Int): List<PageItem> {
    if (totalPages <= 7) {
        // Show all pages if total is 7 or less
        return (1..totalPages).map { PageItem.Page(it) }
    }

    val pages = mutableListOf<PageItem>()
    
    // Always show first page
    pages.add(PageItem.Page(1))
    
    when {
        currentPage <= 4 -> {
            // Show pages 2, 3, 4, 5, ..., last
            (2..5).forEach { pages.add(PageItem.Page(it)) }
            pages.add(PageItem.Ellipsis)
            pages.add(PageItem.Page(totalPages))
        }
        currentPage >= totalPages - 3 -> {
            // Show 1, ..., last-4, last-3, last-2, last-1, last
            pages.add(PageItem.Ellipsis)
            (totalPages - 4..totalPages).forEach { pages.add(PageItem.Page(it)) }
        }
        else -> {
            // Show 1, ..., current-1, current, current+1, ..., last
            pages.add(PageItem.Ellipsis)
            (currentPage - 1..currentPage + 1).forEach { pages.add(PageItem.Page(it)) }
            pages.add(PageItem.Ellipsis)
            pages.add(PageItem.Page(totalPages))
        }
    }
    
    return pages
}

