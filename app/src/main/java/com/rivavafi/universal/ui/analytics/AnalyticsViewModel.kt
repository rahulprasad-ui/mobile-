package com.rivavafi.universal.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.usecase.GetFinancialSummaryUseCase
import com.rivavafi.universal.domain.usecase.GetTransactionsUseCase
import com.rivavafi.universal.sms.SmsTrackingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import javax.inject.Inject

data class MonthlyFinancialData(
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRate: Double,
    val transactionCount: Int,
    val creditCount: Int,
    val debitCount: Int,
    val dailyAverageSpend: Double,
    val projectedSpend: Double,
    val daysInMonth: Int,
    val daysPassed: Int,
    val prevMonthIncome: Double,
    val prevMonthExpense: Double,
    val prevMonthSavings: Double,
    val incomeChangePercent: Double,
    val expenseChangePercent: Double,
    val savingsChangePercent: Double,
    val categoryBreakdown: List<CategoryStat>,
    val topMerchants: List<MerchantStat>,
    val dailyTrends: List<DailySpendPoint>,
    val dayOfWeekStats: List<DayOfWeekStat>,
    val largestTransactions: List<TransactionEntity>,
    val subscriptions: List<SubscriptionStat>
)

data class CategoryStat(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int,
    val colorHex: String,
    val icon: String
)

data class MerchantStat(
    val name: String,
    val amount: Double,
    val count: Int,
    val percentage: Float
)

data class DailySpendPoint(
    val dayOfMonth: Int,
    val dayLabel: String,
    val debit: Float,
    val credit: Float
)

data class DayOfWeekStat(
    val dayName: String,
    val totalAmount: Double,
    val percentage: Float
)

data class SubscriptionStat(
    val merchantName: String,
    val amount: Double,
    val billingCycle: String,
    val category: String
)

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Empty(
        val selectedMonth: Calendar,
        val prevMonthExpense: Double = 0.0,
        val availableMonths: List<Pair<Calendar, Int>> = emptyList(),
        val totalTransactionsCount: Int = 0
    ) : AnalyticsUiState()
    data class Success(
        val data: MonthlyFinancialData,
        val transactions: List<TransactionEntity>,
        val availableMonths: List<Pair<Calendar, Int>> = emptyList()
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val preferencesRepository: com.rivavafi.universal.data.preferences.UserPreferencesRepository,
    private val smsInboxScanner: com.rivavafi.universal.sms.SmsInboxScanner
) : ViewModel() {

    val terminologyMode = preferencesRepository.terminologyModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "CREDIT_DEBIT"
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth = _selectedMonth.asStateFlow()
    private var hasManuallySelectedMonth = false

    fun scanSms(context: android.content.Context) {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            try {
                _isScanning.value = true
                preferencesRepository.setSmsTrackingMode(SmsTrackingMode.BOTH.name)
                smsInboxScanner.scanInbox(context, SmsTrackingMode.BOTH, userId)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun selectMonth(cal: Calendar) {
        hasManuallySelectedMonth = true
        _selectedMonth.value = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    }

    fun previousMonth() {
        hasManuallySelectedMonth = true
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, -1)
        _selectedMonth.value = newCal
    }

    fun nextMonth() {
        hasManuallySelectedMonth = true
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, 1)
        _selectedMonth.value = newCal
    }

    fun resetToCurrentMonth() {
        hasManuallySelectedMonth = true
        _selectedMonth.value = Calendar.getInstance()
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getTransactionsUseCase(),
        _selectedMonth
    ) { allTransactions, monthCal ->

        // Discover all months with transactions
        val availableMonthsWithTxns = allTransactions.groupBy { txn ->
            val c = Calendar.getInstance().apply { timeInMillis = txn.date }
            Pair(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }.map { (yearMonth, txns) ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, yearMonth.first)
                set(Calendar.MONTH, yearMonth.second)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            Pair(cal, txns.size)
        }.sortedByDescending { it.first.timeInMillis }

        // If user hasn't explicitly navigated and current month has 0 transactions, auto-focus to latest active month
        var activeCal = monthCal
        val initialCurrentMonthTxns = allTransactions.filter { txn ->
            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
            txnCal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) && txnCal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
        }

        if (!hasManuallySelectedMonth && initialCurrentMonthTxns.isEmpty() && availableMonthsWithTxns.isNotEmpty()) {
            activeCal = availableMonthsWithTxns.first().first
        }

        val selectedYear = activeCal.get(Calendar.YEAR)
        val selectedMonthNum = activeCal.get(Calendar.MONTH)

        // Previous month calendar for comparison
        val prevMonthCal = (activeCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        val prevMonthNum = prevMonthCal.get(Calendar.MONTH)

        val monthlyTransactions = allTransactions.filter { txn ->
            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
            txnCal.get(Calendar.YEAR) == selectedYear && txnCal.get(Calendar.MONTH) == selectedMonthNum
        }

        val prevMonthTransactions = allTransactions.filter { txn ->
            val txnCal = Calendar.getInstance().apply { timeInMillis = txn.date }
            txnCal.get(Calendar.YEAR) == prevYear && txnCal.get(Calendar.MONTH) == prevMonthNum
        }

        val prevIncome = prevMonthTransactions.filter { isCredit(it.type) }.sumOf { it.amount }
        val prevExpense = prevMonthTransactions.filter { isDebit(it.type) }.sumOf { it.amount }
        val prevSavings = prevIncome - prevExpense

        if (monthlyTransactions.isEmpty()) {
            return@combine AnalyticsUiState.Empty(
                selectedMonth = activeCal,
                prevMonthExpense = prevExpense,
                availableMonths = availableMonthsWithTxns,
                totalTransactionsCount = allTransactions.size
            )
        }

        val credits = monthlyTransactions.filter { isCredit(it.type) }
        val debits = monthlyTransactions.filter { isDebit(it.type) }

        val totalIncome = credits.sumOf { it.amount }
        val totalExpense = debits.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100.0).coerceIn(-100.0, 100.0) else 0.0

        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentCal = Calendar.getInstance()
        val isCurrentMonth = selectedYear == currentCal.get(Calendar.YEAR) && selectedMonthNum == currentCal.get(Calendar.MONTH)
        val daysPassed = if (isCurrentMonth) currentCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth) else daysInMonth

        val dailyAvg = if (daysPassed > 0) totalExpense / daysPassed else 0.0
        val projectedSpend = if (isCurrentMonth) dailyAvg * daysInMonth else totalExpense

        val incomeChange = if (prevIncome > 0) ((totalIncome - prevIncome) / prevIncome) * 100.0 else 0.0
        val expenseChange = if (prevExpense > 0) ((totalExpense - prevExpense) / prevExpense) * 100.0 else 0.0
        val savingsChange = if (prevSavings != 0.0) ((netSavings - prevSavings) / Math.abs(prevSavings)) * 100.0 else 0.0

        // Category breakdown
        val categoryBreakdown = debits.groupBy {
            val cat = it.category.ifBlank { "Other" }
            if (!it.subcategory.isNullOrBlank()) it.subcategory else cat
        }.map { (catName, txns) ->
            val amount = txns.sumOf { it.amount }
            val pct = if (totalExpense > 0) ((amount / totalExpense) * 100).toFloat() else 0f
            CategoryStat(
                category = catName,
                amount = amount,
                percentage = pct,
                count = txns.size,
                colorHex = getCategoryColor(catName),
                icon = getCategoryIcon(catName)
            )
        }.sortedByDescending { it.amount }

        // Top Merchants
        val topMerchants = debits.groupBy { it.merchantName.ifBlank { "Unknown Merchant" } }
            .map { (merchant, txns) ->
                val amount = txns.sumOf { it.amount }
                val pct = if (totalExpense > 0) ((amount / totalExpense) * 100).toFloat() else 0f
                MerchantStat(
                    name = merchant,
                    amount = amount,
                    count = txns.size,
                    percentage = pct
                )
            }.sortedByDescending { it.amount }.take(5)

        // Daily Trends across month
        val dailyMap = mutableMapOf<Int, Pair<Float, Float>>()
        for (d in 1..daysInMonth) {
            dailyMap[d] = Pair(0f, 0f)
        }
        monthlyTransactions.forEach { txn ->
            val cal = Calendar.getInstance().apply { timeInMillis = txn.date }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val current = dailyMap[day] ?: Pair(0f, 0f)
            if (isDebit(txn.type)) {
                dailyMap[day] = Pair(current.first + txn.amount.toFloat(), current.second)
            } else if (isCredit(txn.type)) {
                dailyMap[day] = Pair(current.first, current.second + txn.amount.toFloat())
            }
        }
        val dailyTrends = dailyMap.map { (day, amounts) ->
            DailySpendPoint(
                dayOfMonth = day,
                dayLabel = "$day",
                debit = amounts.first,
                credit = amounts.second
            )
        }.sortedBy { it.dayOfMonth }

        // Day of week distribution
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val daySums = DoubleArray(7) { 0.0 }
        debits.forEach { txn ->
            val cal = Calendar.getInstance().apply { timeInMillis = txn.date }
            val dayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
            if (dayIdx in 0..6) {
                daySums[dayIdx] += txn.amount
            }
        }
        val dayOfWeekStats = dayNames.mapIndexed { idx, name ->
            val sum = daySums[idx]
            val pct = if (totalExpense > 0) ((sum / totalExpense) * 100).toFloat() else 0f
            DayOfWeekStat(name, sum, pct)
        }

        // Subscriptions
        val subscriptions = debits.filter {
            it.category.equals("SUBSCRIPTION", ignoreCase = true) ||
            it.billingCycle != null ||
            it.merchantName.contains("Netflix", ignoreCase = true) ||
            it.merchantName.contains("Spotify", ignoreCase = true) ||
            it.merchantName.contains("Prime", ignoreCase = true) ||
            it.merchantName.contains("YouTube", ignoreCase = true)
        }.distinctBy { it.merchantName }.map {
            SubscriptionStat(
                merchantName = it.merchantName,
                amount = it.amount,
                billingCycle = it.billingCycle ?: "Monthly",
                category = it.category
            )
        }

        val largestTransactions = monthlyTransactions.sortedByDescending { it.amount }.take(5)

        val data = MonthlyFinancialData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRate = savingsRate,
            transactionCount = monthlyTransactions.size,
            creditCount = credits.size,
            debitCount = debits.size,
            dailyAverageSpend = dailyAvg,
            projectedSpend = projectedSpend,
            daysInMonth = daysInMonth,
            daysPassed = daysPassed,
            prevMonthIncome = prevIncome,
            prevMonthExpense = prevExpense,
            prevMonthSavings = prevSavings,
            incomeChangePercent = incomeChange,
            expenseChangePercent = expenseChange,
            savingsChangePercent = savingsChange,
            categoryBreakdown = categoryBreakdown,
            topMerchants = topMerchants,
            dailyTrends = dailyTrends,
            dayOfWeekStats = dayOfWeekStats,
            largestTransactions = largestTransactions,
            subscriptions = subscriptions
        )

        AnalyticsUiState.Success(data, monthlyTransactions, availableMonthsWithTxns)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .catch { e ->
        emit(AnalyticsUiState.Error(e.message ?: "An unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState.Loading
    )

    private fun isCredit(type: String): Boolean {
        val upper = type.uppercase()
        return upper == "CREDIT" || upper == "INCOME" || upper == "REWARD" || upper == "REFUND"
    }

    private fun isDebit(type: String): Boolean {
        val upper = type.uppercase()
        return upper == "DEBIT" || upper == "EXPENSE" || upper == "BILL_PENDING" || upper == "PAYMENT" || upper == "TRANSFER"
    }

    private fun getCategoryColor(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("swiggy") || lower.contains("zomato") -> "#F59E0B"
            lower.contains("shop") || lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> "#EC4899"
            lower.contains("travel") || lower.contains("fuel") || lower.contains("uber") || lower.contains("ola") || lower.contains("flight") -> "#06B6D4"
            lower.contains("bill") || lower.contains("electricity") || lower.contains("recharge") || lower.contains("utility") -> "#8B5CF6"
            lower.contains("entertain") || lower.contains("movie") || lower.contains("netflix") || lower.contains("spotify") -> "#EF4444"
            lower.contains("invest") || lower.contains("stock") || lower.contains("groww") || lower.contains("zerodha") || lower.contains("mutual") -> "#10B981"
            lower.contains("health") || lower.contains("medical") || lower.contains("pharmacy") || lower.contains("doctor") -> "#14B8A6"
            lower.contains("transfer") || lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") -> "#3B82F6"
            lower.contains("salary") || lower.contains("income") -> "#22C55E"
            lower.contains("subscript") -> "#6366F1"
            else -> "#94A3B8"
        }
    }

    private fun getCategoryIcon(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("food") || lower.contains("dining") -> "🍔"
            lower.contains("shop") -> "🛍️"
            lower.contains("travel") || lower.contains("fuel") -> "🚗"
            lower.contains("bill") || lower.contains("recharge") -> "⚡"
            lower.contains("entertain") -> "🎬"
            lower.contains("invest") -> "📈"
            lower.contains("health") -> "🏥"
            lower.contains("transfer") || lower.contains("upi") -> "💸"
            lower.contains("salary") || lower.contains("income") -> "💰"
            lower.contains("subscript") -> "🔄"
            else -> "🏷️"
        }
    }
}
