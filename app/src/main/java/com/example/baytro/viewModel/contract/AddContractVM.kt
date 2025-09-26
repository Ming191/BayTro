package com.example.baytro.viewModel.contract

import androidx.lifecycle.ViewModel
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AddContractVM (
    private val contractRepo : ContractRepository
) : ViewModel() {
    private val _addContractUiState = MutableStateFlow<UiState<Contract>>(UiState.Idle)
    val addContractUiState : StateFlow<UiState<Contract>> = _addContractUiState

    private val _addContractFormState = MutableStateFlow(AddContractFormState())
    val addContractFormState : StateFlow<AddContractFormState> = _addContractFormState


}