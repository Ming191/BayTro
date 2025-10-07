package com.example.baytro.view.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        controlsVisible = true
    }

    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(animationSpec = tween(300, 100)) + slideInVertically { it / 2 }
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageButton(
                text = "Back",
                isSelected = false,
                enabled = hasPreviousPage,
                onClick = onPreviousPage
            )

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
                                    onPageClick(pageItem.number - 1)
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "BackgroundColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300),
        label = "TextColor"
    )

    val buttonSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) 44.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonSize"
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(buttonSize)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "PageNumberText"
        ) { targetText ->
            Text(
                text = targetText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor // Áp dụng màu chữ đã được animate
            )
        }
    }
}

private sealed class PageItem {
    data class Page(val number: Int) : PageItem()
    object Ellipsis : PageItem()
}

private fun generatePageNumbers(currentPage: Int, totalPages: Int): List<PageItem> {
    if (totalPages <= 7) {
        return (1..totalPages).map { PageItem.Page(it) }
    }

    val pages = mutableListOf<PageItem>()

    pages.add(PageItem.Page(1))

    when {
        currentPage <= 4 -> {
            (2..5).forEach { pages.add(PageItem.Page(it)) }
            pages.add(PageItem.Ellipsis)
            pages.add(PageItem.Page(totalPages))
        }
        currentPage >= totalPages - 3 -> {
            pages.add(PageItem.Ellipsis)
            (totalPages - 4..totalPages).forEach { pages.add(PageItem.Page(it)) }
        }
        else -> {
            pages.add(PageItem.Ellipsis)
            (currentPage - 1..currentPage + 1).forEach { pages.add(PageItem.Page(it)) }
            pages.add(PageItem.Ellipsis)
            pages.add(PageItem.Page(totalPages))
        }
    }

    return pages
}