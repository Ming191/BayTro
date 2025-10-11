package com.example.baytro.data.user

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global state holder for user role
 * Allows MainActivity to load role once and share with MainScreen
 */
object UserRoleState {
    private val _userRole = MutableStateFlow<Role?>(null)
    val userRole: StateFlow<Role?> = _userRole.asStateFlow()

    fun setRole(role: Role?) {
        _userRole.value = role
    }

    fun clearRole() {
        _userRole.value = null
    }
}

