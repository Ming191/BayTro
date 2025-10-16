// DÁN TOÀN BỘ KHỐI CODE NÀY VÀO CUỐI FILE CỦA BẠN

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class SpeedDialMenuItem(
    val icon: ImageVector,
    val label: String
)

@Composable
fun SpeedDialFab(
    isMenuOpen: Boolean,
    onToggleMenu: () -> Unit,
    items: List<SpeedDialMenuItem>,
    onItemClick: (SpeedDialMenuItem) -> Unit,
    mainIconOpen: ImageVector = Icons.Default.Add,
    mainIconClose: ImageVector = Icons.Default.Close
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabRotation"
    )

    val fabSize by animateDpAsState(
        targetValue = if (isMenuOpen) 60.dp else 56.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabSize"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                expandFrom = Alignment.Bottom
            ),
            exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.forEachIndexed { index, item ->
                    SpeedDialMenuItem(
                        item = item,
                        index = index,
                        isMenuOpen = isMenuOpen,
                        onClick = {
                            onItemClick(item)
                            onToggleMenu()
                        },
                        fabSize = fabSize
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onToggleMenu,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(fabSize)
        ) {
            Icon(
                imageVector = if (isMenuOpen) mainIconClose else mainIconOpen,
                contentDescription = "Toggle Speed Dial Menu",
                modifier = Modifier.rotate(rotationAngle)
            )
        }
    }
}

@Composable
private fun SpeedDialMenuItem(
    item: SpeedDialMenuItem,
    index: Int,
    isMenuOpen: Boolean,
    onClick: () -> Unit,
    fabSize: Dp = 48.dp
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isMenuOpen) {
        if (isMenuOpen) {
            delay(index * 50L)
            isVisible = true
        } else {
            isVisible = false
        }
    }

    var pressCount by remember { mutableStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (pressCount > 0) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "ItemScale",
        finishedListener = {
            if (pressCount > 0) {
                pressCount = 0
            }
        }
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(300)
        ) + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialScale = 0.8f
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            animationSpec = tween(150),
            targetScale = 0.8f
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.scale(scale)
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = item.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FloatingActionButton(
                onClick = {
                    pressCount++
                    onClick()
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(fabSize)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label
                )
            }
        }
    }
}
