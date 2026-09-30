package com.rivavafi.universal.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rivavafi.universal.domain.repository.TransactionRepository
import com.rivavafi.universal.data.local.UserCorrectionDao
import com.rivavafi.universal.data.local.UserCorrectionEntity
import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.domain.usecase.GetCategoriesUseCase
import com.rivavafi.universal.domain.usecase.AddCategoryUseCase
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

sealed class TransactionsUiState {
    object Loading : TransactionsUiState()
    object Empty : TransactionsUiState()
    data class Success(
        val transactions: List<TransactionEntity>,
        val monthlyCredit: Double = 0.0,
        val monthlyDebit: Double = 0.0,
        val groupedByDay: Map<String, List<TransactionEntity>> = emptyMap()
    ) : TransactionsUiState()
    data class Error(val message: String) : TransactionsUiState()
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val repository: TransactionRepository,
    private val userCorrectionDao: UserCorrectionDao,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun getTransactionById(id: Long): kotlinx.coroutines.flow.Flow<TransactionEntity?> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(null)
        return repository.getTransactionById(id, userId)
    }

    fun updateTransaction(transaction: TransactionEntity, createRule: Boolean = true, ruleKeyword: String? = null) {
        viewModelScope.launch {
            // Save transaction changes
            repository.addTransaction(transaction)

            if (createRule) {
                // Layer 3 Learning: Save user correction mapping so future SMS gets categorized correctly
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val correction = UserCorrectionEntity(
                    merchantName = transaction.merchantName,
                    category = transaction.category,
                    subcategory = transaction.subcategory,
                    keyword = ruleKeyword,
                    userId = userId
                )
                userCorrectionDao.insertCorrection(correction)
            }
        }
    }

    private val _uiState = MutableStateFlow<TransactionsUiState>(TransactionsUiState.Loading)
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth = _selectedMonth.asStateFlow()

    fun previousMonth() {
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, -1)
        _selectedMonth.value = newCal
        applyFilters()
    }

    fun nextMonth() {
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, 1)
        _selectedMonth.value = newCal
        applyFilters()
    }

    val showSmsDetails = userPreferencesRepository.showSmsDetailsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            repository.startSync(userId)
        }
        loadTransactions()
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase.initialize()
            getCategoriesUseCase().collectLatest {
                _categories.value = it
            }
        }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            addCategoryUseCase(CategoryEntity(name = name, type = type))
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun onSortOrderChanged(order: SortOrder) {
        _sortOrder.value = order
        applyFilters()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            getTransactionsUseCase().collectLatest { transactions ->
                allTransactions = transactions
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        var filteredList = allTransactions
        val query = _searchQuery.value.trim().lowercase()

        // Filter by selected month
        val calSelected = _selectedMonth.value
        val selectedYear = calSelected.get(Calendar.YEAR)
        val selectedMonthNum = calSelected.get(Calendar.MONTH)

        filteredList = filteredList.filter { txn ->
            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
            txnCal.get(Calendar.YEAR) == selectedYear && txnCal.get(Calendar.MONTH) == selectedMonthNum
        }

        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.merchantName.lowercase().contains(query) ||
                it.category.lowercase().contains(query) ||
                it.amount.toString().contains(query) ||
                (it.upiId != null && it.upiId.lowercase().contains(query)) ||
                (it.transactionId != null && it.transactionId.lowercase().contains(query)) ||
                (it.referenceId != null && it.referenceId.lowercase().contains(query))
            }
        }

        filteredList = when (_sortOrder.value) {
            SortOrder.DATE_DESC -> filteredList.sortedByDescending { it.date }
            SortOrder.DATE_ASC -> filteredList.sortedBy { it.date }
            SortOrder.AMOUNT_DESC -> filteredList.sortedByDescending { it.amount }
            SortOrder.AMOUNT_ASC -> filteredList.sortedBy { it.amount }
            SortOrder.MERCHANT_ASC -> filteredList.sortedBy { it.merchantName.lowercase() }
            SortOrder.CATEGORY_ASC -> filteredList.sortedBy { it.category.lowercase() }
        }

        if (filteredList.isEmpty()) {
            _uiState.value = TransactionsUiState.Empty
        } else {
            val credit = filteredList.filter { it.type == "CREDIT" || it.type == "INCOME" || it.type == "REWARD" || it.type == "REWARD" }.sumOf { it.amount }
            val debit = filteredList.filter { it.type == "DEBIT" || it.type == "EXPENSE" || it.type == "BILL_PENDING" || it.type == "BILL_PENDING" }.sumOf { it.amount }

            val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val grouped = filteredList.groupBy { txn ->
                dateFormatter.format(java.util.Date(txn.date))
            }

            _uiState.value = TransactionsUiState.Success(
                transactions = filteredList,
                monthlyCredit = credit,
                monthlyDebit = debit,
                groupedByDay = grouped
            )
        }
    }

    fun fetchMoreHistory() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val lastDate = allTransactions.minOfOrNull { it.date } ?: return@launch
            repository.fetchMoreHistory(userId, lastDate)
        }
    }
}

enum class SortOrder {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, MERCHANT_ASC, CATEGORY_ASC
}