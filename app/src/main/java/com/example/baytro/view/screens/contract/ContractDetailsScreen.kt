package com.example.baytro.view.screens.contract

import QrCodeDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.contract.Status
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.User
import com.example.baytro.view.components.AddFirstTenantPrompt
import com.example.baytro.view.components.ContractCard
import com.example.baytro.view.components.DividerWithSubhead
import com.example.baytro.view.components.TenantsSection
import com.example.baytro.viewModel.contract.ContractDetailsFormState
import com.example.baytro.viewModel.contract.ContractDetailsVM
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContractDetailsScreen(
    contractId: String,
    viewModel: ContractDetailsVM = koinViewModel()
) {
    val qrState by viewModel.qrState
    val formState by viewModel.formState.collectAsState()
    QrCodeDialog(
        state = qrState,
        onDismissRequest = viewModel::clearQrCode,
        onRetry = viewModel::generateQrCode
    )

    ContractDetailsContent(
        formState = formState,
        onAddTenant = { viewModel.generateQrCode() },
    )
}

@Composable
fun ContractDetailsContent(
    formState: ContractDetailsFormState,
    onAddTenant: () -> Unit = {},
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DividerWithSubhead(
                    subhead = "Contract number #${formState.contractNumber}",
                    modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)
                )
                ContractCard(
                    infoMap = mapOf(
                        "Building name:" to formState.buildingName,
                        "Room number:" to formState.roomNumber,
                        "Num.Tenants:" to formState.tenantList.size.toString(),
                        "Start Date:" to formState.startDate,
                        "End Date:" to formState.endDate,
                        "Rental fee:" to "${formState.rentalFee} VND/month",
                        "Deposit:" to "${formState.deposit} VND",
                    )
                )
            }
            item {
                DividerWithSubhead(
                    subhead = "Services",
                    modifier = Modifier.fillMaxSize().padding(bottom = 4.dp)
                )
            }
            item {
                DividerWithSubhead(
                    subhead = "Tenants",
                    modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                )
            }

            if (formState.status == Status.PENDING) {
                item {
                    AddFirstTenantPrompt(onClick = onAddTenant)
                }
            } else {
                item {
                    TenantsSection(
                        tenants = formState.tenantList,
                        onAddTenantClick = onAddTenant,
                    )
                    Spacer(Modifier.height(16.dp))
                    ActionButtonsRow()
                }
            }
        }
    }
}

@Composable
@Preview
fun ContractDetailsContentPreview() {
    ContractDetailsContent(
        formState = ContractDetailsFormState(
            contractNumber = "123456",
            buildingName = "Sunrise Apartment",
            roomNumber = "A101",
            rentalFee = "5000000",
            deposit = "10000000",
            tenantList = listOf(
                User(
                    email = "",
                    phoneNumber = "0123456789",
                    fullName = "John Doe",
                    dateOfBirth = "",
                    gender = Gender.MALE,
                    address = "",
                    profileImgUrl = ""
                )
            ),
            startDate = "2023-01-01",
            endDate = "2023-12-31",
            status = Status.ACTIVE
        )
    )
}

@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton(text = "Export", icon = Icons.Default.UploadFile, modifier = Modifier.weight(1f), isPrimary = true)
        ActionButton(text = "Edit", icon = Icons.Default.Edit, modifier = Modifier.weight(1f), isPrimary = false)
        ActionButton(text = "End", icon = Icons.Default.Delete, modifier = Modifier.weight(1f), isPrimary = false)
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    onClick: () -> Unit = {}
) {
    if (isPrimary) {
        Button(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}
