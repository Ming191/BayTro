package com.example.baytro.view

data class UIState<T>(
    val isLoading: Boolean = false,
    val error: String = "",
    val items: List<T> = emptyList()
)