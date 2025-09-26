package com.example.baytro.view.screens.contract

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.contract.Contract
import com.example.baytro.view.components.DropdownSelectField
import com.example.baytro.view.components.RequiredDateTextField
import com.example.baytro.view.components.RequiredTextField
import com.example.baytro.view.components.SubmitButton
import com.example.baytro.view.screens.UiState
import com.example.baytro.viewModel.contract.AddContractFormState
import com.example.baytro.viewModel.contract.AddContractVM
import org.koin.compose.viewmodel.koinViewModel

enum class Tenant { Tenant1, Tenant2, Tenant3 }
enum class Property { Property1, Property2, Property3 }

@Composable
fun AddContractScreen(
    viewModel: AddContractVM = koinViewModel()
) {
    val uiState by viewModel.addContractUiState.collectAsState()
    val formState by viewModel.addContractFormState.collectAsState()

    AddContractContent(
        uiState = uiState,
        formState = formState,
        onSubmit = viewModel::onSubmit,
        onTenantChange = viewModel::onSelectTenant,
        onPropertyChange = viewModel::onSelectProperty,
    )
}

@Composable
fun AddContractContent(
    uiState : UiState<Contract>,
    formState: AddContractFormState,
    onSubmit: () -> Unit,
    onTenantChange: (Tenant) -> Unit,
    onPropertyChange: (Property) -> Unit,
) {
    LazyColumn (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            DropdownSelectField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                label = "Select Tenant",
                options = Tenant.entries.toList(),
                selectedOption = formState.tenantId,
                onOptionSelected = onTenantChange
            )
        }

        item {
            DropdownSelectField(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                label = "Select Property",
                options = Property.entries.toList(),
                selectedOption = formState.propertyId,
                onOptionSelected = onPropertyChange
            )
        }
        item {
            SubmitButton(
                text = "Submit",
                isLoading = uiState is UiState.Loading,
                onClick = onSubmit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddContractScreenPreview() {
    AddContractContent(
        uiState = UiState.Idle,
        formState = AddContractFormState(),
        onSubmit = {},
        onTenantChange = {},
        onPropertyChange = {},
    )
}