package com.example.baytro.viewModel.splash

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.data.IdCardInfo
import com.example.baytro.data.MediaRepository
import com.example.baytro.data.user.Gender
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.User
import com.example.baytro.data.user.UserRepository
import com.example.baytro.utils.ImageProcessor
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.screens.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NewTenantUserVM(
    private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository,
    private val roleCache: com.example.baytro.data.user.UserRoleCache
) : ViewModel() {

    companion object {
        private const val TAG = "NewTenantUserVM"
    }

    private val _newTenantUserFormState = MutableStateFlow(NewTenantUserFormState())
    val newTenantUserFormState: StateFlow<NewTenantUserFormState> = _newTenantUserFormState

    private val _newTenantUserUIState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val newTenantUserUIState: StateFlow<UiState<User>> = _newTenantUserUIState

    private var idCardFrontImageUrl: String? = null
    private var idCardBackImageUrl: String? = null

    fun onFullNameChange(fullName: String) {
        Log.v(TAG, "onFullNameChange: '$fullName'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(fullName = fullName, fullNameError = ValidationResult.Success)
    }

    fun onPermanentAddressChange(permanentAddress: String) {
        Log.v(TAG, "onPermanentAddressChange: '$permanentAddress'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(permanentAddress = permanentAddress, permanentAddressError = ValidationResult.Success)
    }

    fun onDateOfBirthChange(dateOfBirth: String) {
        Log.v(TAG, "onDateOfBirthChange: '$dateOfBirth'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(dateOfBirth = dateOfBirth, dateOfBirthError = ValidationResult.Success)
    }

    fun onGenderChange(gender: Gender) {
        Log.v(TAG, "onGenderChange: $gender")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(gender = gender)
    }

    fun onAvatarUriChange(avatarUri: Uri) {
        Log.d(TAG, "onAvatarUriChange: $avatarUri")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(avatarUri = avatarUri, avatarUriError = ValidationResult.Success)
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        Log.v(TAG, "onPhoneNumberChange: '$phoneNumber'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(phoneNumber = phoneNumber, phoneNumberError = ValidationResult.Success)
    }

    fun onOccupationChange(occupation: String) {
        Log.v(TAG, "onOccupationChange: '$occupation'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(occupation = occupation, occupationError = ValidationResult.Success)
    }

    fun onIdCardNumberChange(idCardNumber: String) {
        Log.v(TAG, "onIdCardNumberChange: '$idCardNumber'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(idCardNumber = idCardNumber, idCardNumberError = ValidationResult.Success)
    }

    fun onIdCardIssueDateChange(idCardIssueDate: String) {
        Log.v(TAG, "onIdCardIssueDateChange: '$idCardIssueDate'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(idCardIssueDate = idCardIssueDate, idCardIssueDateError = ValidationResult.Success)
    }

    fun onEmergencyContactChange(emergencyContact: String) {
        Log.v(TAG, "onEmergencyContactChange: '$emergencyContact'")
        _newTenantUserFormState.value = _newTenantUserFormState.value.copy(emergencyContact = emergencyContact, emergencyContactError = ValidationResult.Success)
    }

    private fun validateInput(): Boolean {
        val formState = _newTenantUserFormState.value
        val fullNameValidator = Validator.validateNonEmpty(formState.fullName, "Full Name")
        val permanentAddressValidator = Validator.validateNonEmpty(formState.permanentAddress, "Permanent Address")
        val dateOfBirthValidator = Validator.validateNonEmpty(formState.dateOfBirth, "Date of Birth")
        val phoneNumberValidator = Validator.validatePhoneNumber(formState.phoneNumber)
        val occupationValidator = Validator.validateNonEmpty(formState.occupation, "Occupation")
        val idCardNumberValidator = Validator.validateNonEmpty(formState.idCardNumber, "ID Card Number")
        val idCardIssueDateValidator = Validator.validateNonEmpty(formState.idCardIssueDate, "ID Card Issue Date")
        val emergencyContactValidator = Validator.validatePhoneNumber(formState.emergencyContact)
        val avatarUriValidator = if (formState.avatarUri == Uri.EMPTY) ValidationResult.Error("Please select an avatar") else ValidationResult.Success

        val isValid = fullNameValidator == ValidationResult.Success &&
                permanentAddressValidator == ValidationResult.Success &&
                dateOfBirthValidator == ValidationResult.Success &&
                phoneNumberValidator == ValidationResult.Success &&
                occupationValidator == ValidationResult.Success &&
                idCardNumberValidator == ValidationResult.Success &&
                idCardIssueDateValidator == ValidationResult.Success &&
                emergencyContactValidator == ValidationResult.Success &&
                avatarUriValidator == ValidationResult.Success

        _newTenantUserFormState.value = formState.copy(
            fullNameError = fullNameValidator,
            permanentAddressError = permanentAddressValidator,
            dateOfBirthError = dateOfBirthValidator,
            phoneNumberError = phoneNumberValidator,
            occupationError = occupationValidator,
            idCardNumberError = idCardNumberValidator,
            idCardIssueDateError = idCardIssueDateValidator,
            emergencyContactError = emergencyContactValidator,
            avatarUriError = avatarUriValidator
        )
        return isValid
    }

    fun submit() {
        onSubmit()
    }

    /**
     * Handles the submission of the new tenant user form.
     * It validates the input fields, and if valid, proceeds to:
     * 1. Get the current authenticated user.
     * 2. Compress the selected avatar image.
     * 3. Upload the compressed image to get a profile image URL.
     * 4. Create a [User] object with the tenant role and form data.
     * 5. Save the new user to the repository.
     *
     * The UI state is updated to [UiState.Loading] during the process,
     * then to [UiState.Success] with the created [User] on success,
     * or [UiState.Error] if any step fails or validation is unsuccessful.
     */
    private fun onSubmit() {
        _newTenantUserUIState.value = UiState.Loading
        val formState = _newTenantUserFormState.value
        Log.d(TAG, "Submit called with formState: $formState")

        if (!validateInput()) {
            Log.w(TAG, "Validation failed")
            _newTenantUserUIState.value = UiState.Error("Please fix the errors in the form.")
            return
        }

        viewModelScope.launch {
            _newTenantUserUIState.value = UiState.Loading
            try {
                Log.d(TAG, "Fetching auth user...")
                val authUser = authRepository.getCurrentUser()
                    ?: throw IllegalStateException("User is not authenticated. Please log in again.")
                Log.d(TAG, "Auth user found: ${authUser.uid}")

                Log.d(TAG, "Compressing image: ${formState.avatarUri}")
                val compressedFile = ImageProcessor.compressImage(
                    context = context,
                    uri = formState.avatarUri
                )
                val compressedFileUri = Uri.fromFile(compressedFile)
                Log.d(TAG, "Compressed image saved at: $compressedFileUri")

                Log.d(TAG, "Uploading profile image...")
                val profileImgUrl = mediaRepository.uploadUserImage(
                    userId = authUser.uid,
                    imageUri = compressedFileUri,
                    subfolder = "profile",
                    imageName = "profile"
                )
                Log.d(TAG, "Profile image uploaded: $profileImgUrl")

                val tenantRole = Role.Tenant(
                    occupation = formState.occupation,
                    idCardNumber = formState.idCardNumber,
                    idCardImageFrontUrl = idCardFrontImageUrl,
                    idCardImageBackUrl = idCardBackImageUrl,
                    idCardIssueDate = formState.idCardIssueDate,
                    emergencyContact = formState.emergencyContact
                )

                val user = User(
                    id = authUser.uid,
                    email = authUser.email!!,
                    fullName = formState.fullName,
                    dateOfBirth = formState.dateOfBirth,
                    gender = formState.gender,
                    address = formState.permanentAddress,
                    phoneNumber = formState.phoneNumber,
                    profileImgUrl = profileImgUrl,
                    role = tenantRole
                )
                Log.d(TAG, "Saving user: $user")

                userRepository.addWithId(user.id, user)
                Log.d(TAG, "User saved successfully")

                // Cache the user role for faster app startup
                Log.d(TAG, "=== CACHING ROLE TYPE ===")
                Log.d(TAG, "User ID: ${authUser.uid}")
                Log.d(TAG, "Role: Tenant")
                roleCache.setRoleType(authUser.uid, tenantRole)
                Log.d(TAG, "✅ User role cached successfully - future app launches will be faster!")
                Log.d(TAG, "=========================")

                _newTenantUserUIState.value = UiState.Success(user)
            } catch (e: Exception) {
                Log.e(TAG, "Submit failed", e)
                _newTenantUserUIState.value =
                    UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun prefillWithIdCardInfo(idCardInfo: IdCardInfo, frontImageUrl: String?, backImageUrl: String?) {
        Log.d(TAG, "prefillWithIdCardInfo: Starting form prefill with data: $idCardInfo")
        Log.d(TAG, "prefillWithIdCardInfo: Front image URL: $frontImageUrl")
        Log.d(TAG, "prefillWithIdCardInfo: Back image URL: $backImageUrl")

        idCardFrontImageUrl = frontImageUrl
        idCardBackImageUrl = backImageUrl

        val currentState = _newTenantUserFormState.value
        Log.d(TAG, "prefillWithIdCardInfo: Current form state before prefill: $currentState")

        val gender = idCardInfo.gender

        val updatedState = currentState.copy(
            fullName = idCardInfo.fullName,
            idCardNumber = idCardInfo.idCardNumber,
            dateOfBirth = idCardInfo.dateOfBirth,
            gender = gender,
            permanentAddress = idCardInfo.permanentAddress,
            idCardIssueDate = idCardInfo.idCardIssueDate,
            fullNameError = ValidationResult.Success,
            permanentAddressError = ValidationResult.Success,
            dateOfBirthError = ValidationResult.Success,
            idCardNumberError = ValidationResult.Success,
            idCardIssueDateError = ValidationResult.Success
        )

        Log.d(TAG, "prefillWithIdCardInfo: Updated form state: $updatedState")
        _newTenantUserFormState.value = updatedState
        Log.i(TAG, "prefillWithIdCardInfo: Form successfully prefilled with ID card data and image URLs stored")
    }
}
