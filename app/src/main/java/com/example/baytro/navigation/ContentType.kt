package com.example.baytro.navigation

sealed interface ContentType {
    object List : ContentType
    object ListWithDetails : ContentType
}