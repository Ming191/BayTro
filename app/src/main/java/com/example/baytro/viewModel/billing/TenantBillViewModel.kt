package com.example.baytro.viewModel.billing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.baytro.data.billing.Bill
import com.example.baytro.data.billing.BillRepository
import com.example.baytro.data.billing.BillSummary
import com.example.baytro.data.contract.Contract
import com.example.baytro.data.contract.ContractRepository
import com.example.baytro.utils.SingleEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar

private const val TAG = "TenantBillViewModel"

sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Error(val message: String) : DataState<Nothing>()
}

data class TenantBillUiState(
    val isContractLoading: Boolean = true,
    val contractId: String? = null,
    val currentBillState: DataState<Bill?> = DataState.Loading,
    val historicalBillsState: DataState<List<BillSummary>> = DataState.Loading,
    val selectedHistoryDate: Pair<Int, Int>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TenantBillViewModel(
    private val billRepository: BillRepository,
    private val contractRepository: ContractRepository,
    auth: FirebaseAuth
) : ViewModel() {

    private val _selectedHistoryDate: MutableStateFlow<Pair<Int, Int>>
    private val _errorEvent = MutableSharedFlow<SingleEvent<String>>(replay = 0)
    val errorEvent: SharedFlow<SingleEvent<String>> = _errorEvent.asSharedFlow()

    val uiState: StateFlow<TenantBillUiState>

    init {
        Log.d(TAG, "Initializing TenantBillViewModel")

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        _selectedHistoryDate = MutableStateFlow(Pair(currentMonth, currentYear))

        val tenantId = auth.currentUser?.uid
        val isContractLoadingFlow = MutableStateFlow(true)
        val activeContractFlow = getActiveContractFlow(tenantId, isContractLoadingFlow)

        val currentBillFlow = getCurrentBillFlow(activeContractFlow)
        val historyBillsFlow = getHistoryBillsFlow(activeContractFlow)

        uiState = combine(
            isContractLoadingFlow,
            activeContractFlow,
            currentBillFlow,
            historyBillsFlow,
            _selectedHistoryDate
        ) { isContractLoading, contract, currentBillState, historyState, selectedDate ->
            TenantBillUiState(
                isContractLoading = isContractLoading,
                contractId = contract?.id,
                currentBillState = currentBillState,
                historicalBillsState = historyState,
                selectedHistoryDate = selectedDate
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TenantBillUiState(
                isContractLoading  = true,
                selectedHistoryDate = Pair(currentMonth, currentYear)
            )
        )

        Log.d(TAG, "TenantBillViewModel initialization complete")
    }

    private fun getActiveContractFlow(
        tenantId: String?,
        isLoading: MutableStateFlow<Boolean>
    ): StateFlow<Contract?> {
        return flow {
            if (tenantId != null) {
                isLoading.value = true
                try {
                    val contract = contractRepository.getActiveContractForTenant(tenantId)
                    emit(contract)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching active contract", e)
                    _errorEvent.emit(SingleEvent("No active contract found."))
                    emit(null)
                } finally {
                    isLoading.value = false
                }
            } else {
                isLoading.value = false
                emit(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    private fun getCurrentBillFlow(activeContractFlow: StateFlow<Contract?>): Flow<DataState<Bill?>> {
        return activeContractFlow.flatMapLatest { contract ->
            if (contract == null) {
                flowOf(DataState.Success(null))
            } else {
                billRepository.listenForCurrentBillByContract(contract.id)
                    .map<Bill?, DataState<Bill?>> { bill -> DataState.Success(bill) }
                    .onStart { emit(DataState.Loading) }
                    .catch { e ->
                        Log.e(TAG, "Error loading current bill", e)
                        emit(DataState.Error("Failed to load current bill."))
                    }
            }
        }
    }

    private fun getHistoryBillsFlow(
        activeContractFlow: StateFlow<Contract?>
    ): Flow<DataState<List<BillSummary>>> {
        return combine(activeContractFlow, _selectedHistoryDate) { contract, date ->
            Pair(contract, date)
        }.flatMapLatest { (contract, date) ->
            if (contract == null) {
                flowOf(DataState.Success(emptyList()))
            } else {
                billRepository.listenForBillsByContractAndMonth(
                    contract.id,
                    date.first,
                    date.second
                )
                    .map<List<BillSummary>, DataState<List<BillSummary>>> { bills -> DataState.Success(bills) } // Bọc dữ liệu trong Success
                    .onStart { emit(DataState.Loading) }
                    .catch { e ->
                        Log.e(TAG, "Error loading bill history", e)
                        _errorEvent.emit(SingleEvent("Lỗi tải lịch sử hóa đơn: ${e.message}"))
                        emit(DataState.Error("Failed to load bill history."))
                    }
            }
        }
    }

    private fun updateMonth(offset: Int) {
        val (month, year) = _selectedHistoryDate.value
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1)
            set(Calendar.YEAR, year)
            add(Calendar.MONTH, offset)
        }
        _selectedHistoryDate.value =
            Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }

    fun goToNextMonth() = updateMonth(1)
    fun goToPreviousMonth() = updateMonth(-1)
}