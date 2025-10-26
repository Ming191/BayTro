package com.example.baytro.viewModel.tenant

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TenantInfoVM(
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val tenantId : String? = checkNotNull(savedStateHandle["tenantId"])
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val uiState: StateFlow<UiState<User>> = _uiState

    private val _tenant = MutableStateFlow<User?>(null)
    val tenant: StateFlow<User?> = _tenant

    init {
        Log.d("TenantInfoVM", "init tenantId: $tenantId")
    }

    fun getTenant() {
        viewModelScope.launch {
            if (tenantId == null) {
                _uiState.value = UiState.Error("No tenant ID provided")
                return@launch
            }
            _uiState.value = UiState.Loading
            val tenant = userRepository.getById(tenantId)
            if(tenant != null) {
                _tenant.value = tenant
                _uiState.value = UiState.Success(tenant)
            } else {
                _uiState.value = UiState.Error("Tenant not found")
            }
        }
    }
}