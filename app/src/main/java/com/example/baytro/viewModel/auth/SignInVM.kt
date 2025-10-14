package com.example.baytro.viewModel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignInFormState
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.data.qr_session.QrSessionRepository
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRepository
import com.example.baytro.data.user.UserRoleCache
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class SignInVM(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val contractRepository: ContractRepository,
    private val qrSessionRepository: QrSessionRepository,
    private val roleCache: UserRoleCache
) : ViewModel() {
    private val _signInUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val signInUIState: StateFlow<AuthUIState> = _signInUIState

    private val _signInFormState = MutableStateFlow(SignInFormState())
    val signInFormState: StateFlow<SignInFormState> = _signInFormState

    fun onEmailChange(email: String) {
        _signInFormState.value = _signInFormState.value.copy(email = email, emailError = ValidationResult.Success)
    }

    fun onPasswordChange(password: String) {
        _signInFormState.value = _signInFormState.value.copy(password = password, passwordError = ValidationResult.Success)
    }

    fun login() {
        val formState = _signInFormState.value
        if (validateInput(formState)) {
            performLogin(formState.email, formState.password)
        }
    }

    private fun validateInput(
        formState: SignInFormState
    ): Boolean {
        val emailValidator = Validator.validateEmail(formState.email)
        val passwordValidator = Validator.validatePassword(formState.password)
        val isValid = emailValidator == ValidationResult.Success && passwordValidator == ValidationResult.Success

        _signInFormState.value = formState.copy(
            emailError = emailValidator,
            passwordError = passwordValidator
        )
        return isValid
    }

    private fun performLogin(email: String, password: String) {
        viewModelScope.launch {
            _signInUIState.value = AuthUIState.Loading
            try {
                val user = authRepository.signIn(email, password)

                if (authRepository.checkVerification()) {
                    val repoUser = userRepository.getById(user.uid)
                    if (repoUser == null) {
                        _signInUIState.value = AuthUIState.FirstTimeUser(user)
                    } else {
                        repoUser.role?.let { role ->
                            com.example.baytro.data.user.UserRoleState.setRole(role)
                            roleCache.setRoleType(user.uid, role)
                        }

                        if (repoUser.role is Role.Tenant) {
                            val hasPendingSession = qrSessionRepository.hasScannedSession(user.uid)
                            if (hasPendingSession) {
                                _signInUIState.value = AuthUIState.TenantPendingSession(user)
                                return@launch
                            }

                            val isInContract = contractRepository.isUserInAnyContract(user.uid)
                            if (!isInContract) {
                                _signInUIState.value = AuthUIState.TenantNoContract(user)
                                return@launch
                            }

                            // Tenant with active contract - redirect to tenant dashboard
                            _signInUIState.value = AuthUIState.TenantWithContract(user)
                            return@launch
                        }
                        // Landlord - redirect to main screen
                        _signInUIState.value = AuthUIState.Success(user)
                    }
                } else {
                    authRepository.sendVerificationEmail()
                    authRepository.signOut()
                    _signInUIState.value = AuthUIState.NeedVerification("Email is not verified. We have sent you a new verification email.")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Incorrect email or password."
                        "ERROR_USER_DISABLED" -> "Your account has been disabled."
                        else -> "Sign in failed. Please try again."
                    }
                    is UnknownHostException -> "No network connection."
                    else -> e.message
                }
                _signInUIState.value = AuthUIState.Error(errorMessage ?: "An unknown error occurred")
            }
        }
    }
}