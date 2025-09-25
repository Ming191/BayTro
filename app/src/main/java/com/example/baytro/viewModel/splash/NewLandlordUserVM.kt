package com.example.baytro.viewModel.splash

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.BankCode
import com.example.baytro.data.Gender
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.Role
import com.example.baytro.data.RoleType
import com.example.baytro.data.User
import com.example.baytro.data.UserRepository
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewLandlordUserVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {
    private val _newLandlordUserFormState = MutableStateFlow(NewLandlordUserFormState())
    val newLandlordUserFormState : StateFlow<NewLandlordUserFormState> = _newLandlordUserFormState

    private val _newLandlordUserUIState = MutableStateFlow<SplashUiState>(SplashUiState.Idle)
    val newLandlordUserUIState: StateFlow<SplashUiState> = _newLandlordUserUIState

    fun onFullNameChange(fullName: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(fullName = fullName, fullNameError = ValidationResult.Success)
    }

    fun onPermanentAddressChange(permanentAddress: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(permanentAddress = permanentAddress, permanentAddressError = ValidationResult.Success)
    }

    fun onDateOfBirthChange(dateOfBirth: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(dateOfBirth = dateOfBirth, dateOfBirthError = ValidationResult.Success)
    }

    fun onBankAccountNumberChange(bankAccountNumber: String) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(bankAccountNumber = bankAccountNumber, bankAccountNumberError = ValidationResult.Success)
    }

    fun onGenderChange(gender: Gender) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(gender = gender)
    }

    fun onBankCodeChange(bankCode: BankCode) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(bankCode = bankCode)
    }

    fun onAvatarUriChange(avatarUri: Uri) {
        _newLandlordUserFormState.value = _newLandlordUserFormState.value.copy(avatarUri = avatarUri, avatarUriError = ValidationResult.Success)
    }

    private val _showPhotoSelector = MutableStateFlow(false)
    val showPhotoSelector: StateFlow<Boolean> = _showPhotoSelector
    fun onAvatarClick() {
        _showPhotoSelector.value = true
    }

    fun onPhotoSelectorDismissed() {
        _showPhotoSelector.value = false
    }

    private fun validateInput(
        formState: NewLandlordUserFormState
    ): Boolean {
        val fullNameValidator = Validator.validateNonEmpty(formState.fullName, "Full Name")
        val permanentAddressValidator = Validator.validateNonEmpty(formState.permanentAddress, "Permanent Address")
        val dateOfBirthValidator = Validator.validateNonEmpty(formState.dateOfBirth, "Date of Birth")
        val bankAccountNumberValidator = Validator.validateNonEmpty(formState.bankAccountNumber , "Bank Account Number")
        val isValid = fullNameValidator == ValidationResult.Success &&
                permanentAddressValidator == ValidationResult.Success &&
                dateOfBirthValidator == ValidationResult.Success &&
                bankAccountNumberValidator == ValidationResult.Success

        _newLandlordUserFormState.value = formState.copy(
            fullNameError = fullNameValidator,
            permanentAddressError = permanentAddressValidator,
            dateOfBirthError = dateOfBirthValidator,
            bankAccountNumberError = bankAccountNumberValidator
        )
        return isValid
    }

    fun submit() {
        onSubmit()
    }

    private fun onSubmit() {
        _newLandlordUserUIState.value = SplashUiState.Loading
        val formState = _newLandlordUserFormState.value
        if (!validateInput(formState)) {
            return
        }
        viewModelScope.launch {

        try {
            val userId = authRepository.getCurrentUser()!!.uid
            val user = userRepository.getById(userId)
            val profileImgUrl = mediaRepository.uploadUserProfileImage(
                userId = user!!.id,
                imageUri = formState.avatarUri
            )

            val landlordRole = Role.Landlord(
                fullName = formState.fullName,
                dateOfBirth = formState.dateOfBirth,
                gender = formState.gender,
                address = formState.permanentAddress,
                bankCode = formState.bankCode.name,
                bankAccountNumber = formState.bankAccountNumber,
                profileImgUrl = profileImgUrl
            )
            val updatedUser = User(
                id = user.id,
                email = user.email,
                roleType = RoleType.LANDLORD,
                role = landlordRole,
                phoneNumber = user.phoneNumber,
                lastLogin = user.lastLogin,
                isFirstLogin = false
            )
            Log.d("NewLandlordUserVM", "Updating Firestore document at users/${user.id}")
            Log.d("NewLandlordUserVM", "Updated user: $updatedUser")
            userRepository.update(user.id, updatedUser)
            _newLandlordUserUIState.value = SplashUiState.Success
            } catch (e: Exception) {
                _newLandlordUserUIState.value = SplashUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}