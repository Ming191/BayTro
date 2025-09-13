package com.example.baytro.navigation

sealed interface NavigationType {
    object NavigationBottom : NavigationType
    object NavigationRail : NavigationType
    object NavigationDrawer : NavigationType
}