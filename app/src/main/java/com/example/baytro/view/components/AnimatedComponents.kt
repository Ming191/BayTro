package com.example.baytro.view.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable

/**
 * A reusable animated wrapper component that fades in and slides up content.
 * Used for consistent animations across form fields and UI elements.
 *
 * @param visible Whether the content should be visible
 * @param content The composable content to animate
 */
@Composable
fun AnimatedItem(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 400, easing = EaseOutCubic)
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}

