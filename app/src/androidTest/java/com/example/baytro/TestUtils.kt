package com.example.baytro.utils

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithContentDescription

// Extension functions for common test operations
fun SemanticsNodeInteractionsProvider.waitForLoadingToComplete() {
    waitUntil(3000L) {
        onNodeWithContentDescription("Loading progress")
            .fetchSemanticsNode()
            .isAttached
            .not()
    }
}

fun SemanticsNodeInteractionsProvider.openNavigationDrawer() {
    onNodeWithContentDescription("Navigation drawer")
        .or(onNodeWithContentDescription("Menu"))
        .or(onNodeWithContentDescription("Open drawer"))
        .performClick()
}