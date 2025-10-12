package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.EditLandlordInformationFormState
import com.example.baytro.auth.EditPersonalInformationFormState
import com.example.baytro.auth.EditTenantInformationFormState
import com.example.baytro.auth.RoleFormState
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.view.AuthUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditPersonalInformationVM (
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _editPersonalInformationUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val editPersonalInformationUIState: StateFlow<AuthUIState> = _editPersonalInformationUIState
    private val _editPersonalInformationFormState = MutableStateFlow(EditPersonalInformationFormState())
    val editPersonalInformationFormState: StateFlow<EditPersonalInformationFormState> = _editPersonalInformationFormState

    private val _editRoleInformationFormState = MutableStateFlow<RoleFormState?>(null)
    val editRoleInformationFormState: StateFlow<RoleFormState?> = _editRoleInformationFormState

    fun loadEditPersonalInformation() {
        viewModelScope.launch {
            _editPersonalInformationUIState.value = AuthUIState.Loading
            val auth = authRepository.getCurrentUser()
            val user = userRepository.getById(auth!!.uid)
            if (user == null) {
                _editPersonalInformationUIState.value = AuthUIState.Error("Fail to get User")
            } else {
                _editPersonalInformationFormState.value = EditPersonalInformationFormState(
                    fullName = user.fullName,
                    dateOfBirth = user.dateOfBirth,
                    address = user.address,
                    gender = user.gender,
                    phoneNumber = user.phoneNumber,
                    email = user.email,
                    role = user.role
                )
                when (user.role) {
                    is Role.Tenant -> {
                        _editRoleInformationFormState.value = RoleFormState.Tenant(EditTenantInformationFormState())
                    }
                    is Role.Landlord -> {
                        _editRoleInformationFormState.value = RoleFormState.Landlord(EditLandlordInformationFormState())
                    }
                    else -> {
                        _editPersonalInformationUIState.value = AuthUIState.Error("Unknown user role")
                    }
                }
            }
            _editPersonalInformationUIState.value = AuthUIState.Success(auth)
        }
    }

    fun onFullNameChange(fullName: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            fullName = fullName
        )
    }

    fun onDateOfBirthChange(dateOfBirth: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            dateOfBirth = dateOfBirth,
            dateOfBirthError = ValidationResult.Success
        )
    }

    fun onGenderChange(gender: Gender) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            gender = gender
        )
    }

    fun onAddressChange(address: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            address = address
        )
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _editPersonalInformationFormState.value = _editPersonalInformationFormState.value.copy(
            phoneNumber = phoneNumber,
            phoneNumberError = ValidationResult.Success
        )
    }

    fun onEmailChange(email: String) {
    }
}