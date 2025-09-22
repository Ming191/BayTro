package com.example.baytro.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.auth.AuthRepository
import com.example.baytro.auth.SignInFormState
import com.example.baytro.utils.ValidationResult
import com.example.baytro.utils.Validator
import com.example.baytro.view.AuthUIState
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInVM(
    private val authRepository: AuthRepository,
    private val validator: Validator
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
        if (validateInput(formState, validator = validator)) {
            performLogin(formState.email, formState.password)
        }
    }

    private fun validateInput(
        formState: SignInFormState,
        validator: Validator
    ): Boolean {
        val emailValidator = validator.validateEmail(formState.email)
        val passwordValidator = validator.validatePassword(formState.password)
        val isValid = emailValidator == ValidationResult.Success && passwordValidator == ValidationResult.Success

        _signInFormState.value = formState.copy(
            emailError = emailValidator,
            passwordError = passwordValidator
        )
        return isValid
    }

    private fun performLogin(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _signInUIState.value = AuthUIState.Loading
            try {
                val user = authRepository.signIn(email, password)

                if (user.isEmailVerified) {
                    _signInUIState.value = AuthUIState.Success(user)
                } else {
                    authRepository.sendVerificationEmail(user)
                    authRepository.signOut()
                    _signInUIState.value = AuthUIState.NeedVerification("Email chưa được xác thực. Chúng tôi đã gửi lại một email xác thực cho bạn.")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "Email hoặc mật khẩu không đúng."
                        "ERROR_USER_DISABLED" -> "Tài khoản của bạn đã bị vô hiệu hóa."
                        else -> "Đăng nhập thất bại. Vui lòng thử lại."
                    }
                    is java.net.UnknownHostException -> "Không có kết nối mạng."
                    else -> "Đã có lỗi không xác định xảy ra."
                }
                _signInUIState.value = AuthUIState.Error(errorMessage)
            }
        }
    }
}