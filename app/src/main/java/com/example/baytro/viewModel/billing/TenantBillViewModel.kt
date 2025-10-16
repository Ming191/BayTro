package com.example.baytro.viewModel.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.billing.BillSummary
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class TenantBillUiState(
    val isLoading: Boolean = true,
    val contractId: String? = null,
    val currentBill: Bill? = null,
    val historicalBills: List<BillSummary> = emptyList(),
    val selectedHistoryDate: Pair<Int, Int>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TenantBillViewModel(
    private val billRepository: BillRepository,
    private val contractRepository: ContractRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _selectedHistoryDate: MutableStateFlow<Pair<Int, Int>>
    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>()
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()
    val uiState: StateFlow<TenantBillUiState>

    init {
        // Khởi tạo các giá trị ban đầu
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        _selectedHistoryDate = MutableStateFlow(Pair(currentMonth, currentYear))

        val tenantId = auth.currentUser?.uid
        val activeContractFlow = flow {
            if (tenantId != null) {
                try {
                    emit(contractRepository.getActiveContractForTenant(tenantId))
                } catch (e: Exception) {
                    _errorEvent.emit(SingleEvent("Could not find an active contract."))
                    emit(null)
                }
            } else {
                emit(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        val currentBillFlow = activeContractFlow.flatMapLatest { contract ->
            if (contract == null) flowOf(null)
            else billRepository.listenForCurrentBillByContract(contract.id)
                .catch { e -> _errorEvent.emit(SingleEvent("Error loading current bill: ${e.message}")); emit(null) }
        }

        val historyBillsFlow = combine(activeContractFlow, _selectedHistoryDate) { contract, date ->
            Pair(contract, date)
        }.flatMapLatest { (contract, date) ->
            if (contract == null) flowOf(emptyList())
            else billRepository.listenForBillsByContractAndMonth(contract.id, date.first, date.second)
                .catch { e -> _errorEvent.emit(SingleEvent("Error loading bill history: ${e.message}")); emit(emptyList()) }
        }

        uiState = combine(
            activeContractFlow,
            currentBillFlow,
            historyBillsFlow,
            _selectedHistoryDate
        ) { contract, currentBill, history, selectedDate ->
            TenantBillUiState(
                isLoading = (contract == null && tenantId != null && currentBill == null),
                contractId = contract?.id,
                currentBill = currentBill,
                historicalBills = history,
                selectedHistoryDate = selectedDate
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TenantBillUiState(selectedHistoryDate = Pair(currentMonth, currentYear))
        )
    }

    fun goToNextMonth() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, _selectedHistoryDate.value.first - 1)
            set(Calendar.YEAR, _selectedHistoryDate.value.second)
            add(Calendar.MONTH, 1)
        }
        _selectedHistoryDate.value = Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }

    fun goToPreviousMonth() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, _selectedHistoryDate.value.first - 1)
            set(Calendar.YEAR, _selectedHistoryDate.value.second)
            add(Calendar.MONTH, -1)
        }
        _selectedHistoryDate.value = Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
}