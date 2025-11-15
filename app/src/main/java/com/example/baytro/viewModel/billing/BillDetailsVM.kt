package com.example.baytro.viewModel.billing

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.billing.BillStatus
import com.example.baytro.data.billing.PaymentMethod
import com.example.baytro.data.user.Role
import com.example.baytro.data.user.UserRoleState
import com.example.baytro.utils.SingleEvent
import com.example.baytro.utils.cloudFunctions.BillingCloudFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

data class BillDetailsUiState(
    val isLoading: Boolean = true,
    val isActionInProgress: Boolean = false,
    val bill: Bill? = null,
    val qrCodeUrl: String? = null,
    val error: String? = null,
    val isLandlord: Boolean = false,
    val showManualChargeDialog: Boolean = false,
    val showReminderDialog: Boolean = false,

    val bankApps: List<BankAppInfo> = emptyList()
)

sealed interface BillDetailsEvent {
    data class ShowSnackbar(val message: String) : BillDetailsEvent
    data object NavigateBack : BillDetailsEvent
}

sealed interface BillDetailsAction {
    data object MarkAsPaid : BillDetailsAction
    data object ShowReminderDialog : BillDetailsAction
    data object HideReminderDialog : BillDetailsAction
    data class SendReminder(val customMessage: String = "") : BillDetailsAction
    data object ShowManualChargeDialog : BillDetailsAction
    data object HideManualChargeDialog : BillDetailsAction
    data class AddManualCharge(val description: String, val amount: Double) : BillDetailsAction
}

class BillDetailsViewModel(
    private val billRepository: BillRepository,
    private val billingCloudFunctions: BillingCloudFunctions,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: String = savedStateHandle.get<String>("billId")!!

    private val _uiState = MutableStateFlow(BillDetailsUiState())
    val uiState: StateFlow<BillDetailsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SingleEvent<BillDetailsEvent>>()
    val events = _events.asSharedFlow()

    init {
        if (billId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Bill ID is missing.") }
        } else {
            checkUserRole()
            observeBillDetails()
            fetchBankApps()
        }
    }

    private fun fetchBankApps() {
        viewModelScope.launch {
            try {
                val banks = fetchAndroidBankAppsFromApi()
                _uiState.update { it.copy(bankApps = banks) }
            } catch (e: Exception) {
                Log.e("BillDetailsVM", "Error fetching bank apps", e)
            }
        }
    }

    private suspend fun fetchAndroidBankAppsFromApi(): List<BankAppInfo> {
        return withContext(Dispatchers.IO) {
            val url = URL("https://api.vietqr.io/v2/android-app-deeplinks")
            val jsonString = url.readText()
            val jsonObject = JSONObject(jsonString)
            val appsArray = jsonObject.getJSONArray("apps")

            val list = mutableListOf<BankAppInfo>()
            for (i in 0 until appsArray.length()) {
                val appObject = appsArray.getJSONObject(i)
                list.add(
                    BankAppInfo(
                        appId = appObject.getString("appId"),
                        appName = appObject.getString("appName"),
                        appLogo = appObject.getString("appLogo"),
                        deepLink = appObject.getString("deeplink"),
                    )
                )
            }
            list
        }
    }

    fun onAction(action: BillDetailsAction) {
        when (action) {
            is BillDetailsAction.MarkAsPaid -> markBillAsPaidManually()
            is BillDetailsAction.ShowReminderDialog -> showReminderDialog()
            is BillDetailsAction.HideReminderDialog -> hideReminderDialog()
            is BillDetailsAction.SendReminder -> sendPaymentReminder(action.customMessage)
            is BillDetailsAction.ShowManualChargeDialog -> showManualChargeDialog()
            is BillDetailsAction.HideManualChargeDialog -> hideManualChargeDialog()
            is BillDetailsAction.AddManualCharge -> addManualCharge(action.description, action.amount)
        }
    }

    private fun checkUserRole() {
        val currentRole = UserRoleState.userRole.value
        val isLandlord = currentRole is Role.Landlord
        _uiState.update { it.copy(isLandlord = isLandlord) }
    }

    private fun observeBillDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                billRepository.observeById(billId).collect { bill ->
                    if (bill != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                bill = bill,
                                qrCodeUrl = generateQrCodeUrl(bill)
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Bill not found.") }
                    }
                }
            } catch (e: Exception) {
                Log.e("BillDetailsVM", "Error observing bill", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load bill details.") }
            }
        }
    }

    private fun generateQrCodeUrl(bill: Bill): String? {
        val details = bill.paymentDetails ?: return null
        val accountNumber = details.accountNumber
        val bankCode = details.bankCode
        val amount = bill.totalAmount.toLong()
        val paymentCode = bill.paymentCode

        if (accountNumber.isBlank() || bankCode.isBlank() || paymentCode.isNullOrBlank()) {
            return null
        }

        return try {
            val encodedDescription = URLEncoder.encode(paymentCode, "UTF-8")
            "https://qr.sepay.vn/img?acc=$accountNumber&bank=$bankCode&amount=$amount&des=$encodedDescription"
        } catch (_: Exception) {
            null
        }
    }

    private fun markBillAsPaidManually() {
        val currentBill = _uiState.value.bill ?: return

        if (currentBill.status == BillStatus.PAID || _uiState.value.isActionInProgress) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }

            billingCloudFunctions.markBillAsPaid(
                billId = billId,
                paidAmount = currentBill.totalAmount,
                paymentMethod = PaymentMethod.CASH.name
            ).onSuccess { response ->
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar(response.message)))
            }.onFailure { error ->
                Log.e("BillDetailsVM", "Error marking bill as paid", error)
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar("Error: ${error.message}")))
            }
        }
    }

    private fun sendPaymentReminder(customMessage: String = "") {
        if (_uiState.value.isActionInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showReminderDialog = false) }

            billingCloudFunctions.sendBillPaymentReminder(
                billId = billId,
                customMessage = customMessage
            ).onSuccess { response ->
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar(response.message)))
            }.onFailure { error ->
                Log.e("BillDetailsVM", "Error sending reminder", error)
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar("Error: ${error.message}")))
            }
        }
    }

    private fun showReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = true) }
    }

    private fun hideReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = false) }
    }

    private fun showManualChargeDialog() {
        _uiState.update { it.copy(showManualChargeDialog = true) }
    }

    private fun hideManualChargeDialog() {
        _uiState.update { it.copy(showManualChargeDialog = false) }
    }

    private fun addManualCharge(description: String, amount: Double) {
        if (description.isBlank() || amount <= 0) {
            viewModelScope.launch {
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar("Invalid charge details")))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, showManualChargeDialog = false) }

            billingCloudFunctions.addManualChargeToBill(
                billId = billId,
                description = description,
                amount = amount
            ).onSuccess { response ->
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar(response.message)))
            }.onFailure { error ->
                Log.e("BillDetailsVM", "Error adding manual charge", error)
                _uiState.update { it.copy(isActionInProgress = false) }
                _events.emit(SingleEvent(BillDetailsEvent.ShowSnackbar("Error: ${error.message}")))
            }
        }
    }
}