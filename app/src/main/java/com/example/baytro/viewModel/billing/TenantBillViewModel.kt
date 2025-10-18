package com.example.baytro.viewModel.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.billing.BillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TenantBillUiState(
    val isLoading: Boolean = true,
    val currentBill: Bill? = null,
    val error: String? = null
)

class TenantBillViewModel(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TenantBillUiState())
    val uiState: StateFlow<TenantBillUiState> = _uiState

    fun loadCurrentBill(tenantId: String) {
        viewModelScope.launch {
            try {
                billRepository.listenForCurrentBill(tenantId).collect { bill ->
                    _uiState.value = TenantBillUiState(
                        isLoading = false,
                        currentBill = bill,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TenantBillUiState(
                    isLoading = false,
                    currentBill = null,
                    error = e.message ?: "An error occurred while loading the bill"
                )
            }
        }
    }
}

