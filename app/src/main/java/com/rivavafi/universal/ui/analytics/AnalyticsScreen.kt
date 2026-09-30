package com.rivavafi.universal.ui.analytics

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.ui.theme.AmoledBlack
import com.rivavafi.universal.ui.theme.PrimarySky
import com.rivavafi.universal.ui.theme.glassMorphism
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardBg = Color(0xFF141A24)
private val CardBorder = Color(0xFF1E293B)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentBlue = Color(0xFF38BDF8)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentAmber = Color(0xFFF59E0B)

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val terminologyMode by viewModel.terminologyMode.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var activeTab by remember { mutableStateOf("Overview") }
    val tabs = listOf("Overview", "Categories", "Trends", "Comparison")

    Scaffold(
        containerColor = AmoledBlack,
        modifier = Modifier.systemBarsPadding()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val availableMonths = when (val state = uiState) {
                is AnalyticsUiState.Success -> state.availableMonths
                is AnalyticsUiState.Empty -> state.availableMonths
                else -> emptyList()
            }

            var showMonthPicker by remember { mutableStateOf(false) }

            if (showMonthPicker && availableMonths.isNotEmpty()) {
                MonthPickerDialog(
                    availableMonths = availableMonths,
                    selectedMonth = selectedMonth,
                    onSelectMonth = {
                        viewModel.selectMonth(it)
                        showMonthPicker = false
                    },
                    onDismiss = { showMonthPicker = false }
                )
            }

            // Header
            AnalyticsHeader(
                selectedMonth = selectedMonth,
                availableMonths = availableMonths,
                onPrevMonth = { viewModel.previousMonth() },
                onNextMonth = { viewModel.nextMonth() },
                onResetMonth = { viewModel.resetToCurrentMonth() },
                onOpenMonthPicker = { showMonthPicker = true }
            )

            // Filter Tabs
            FilterTabRow(
                tabs = tabs,
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )

            val context = androidx.compose.ui.platform.LocalContext.current
            val isScanning by viewModel.isScanning.collectAsState()

            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimarySky, strokeWidth = 3.dp)
                    }
                }
                is AnalyticsUiState.Error -> {
                    ErrorStateCard(message = state.message)
                }
                is AnalyticsUiState.Empty -> {
                    EmptyStateView(
                        selectedMonth = state.selectedMonth,
                        prevMonthExpense = state.prevMonthExpense,
                        availableMonths = state.availableMonths,
                        totalTransactionsCount = state.totalTransactionsCount,
                        isScanning = isScanning,
                        onScanSms = { viewModel.scanSms(context) },
                        onResetMonth = { viewModel.resetToCurrentMonth() },
                        onSelectMonth = { viewModel.selectMonth(it) }
                    )
                }
                is AnalyticsUiState.Success -> {
                    val data = state.data
                    when (activeTab) {
                        "Overview" -> {
                            // 1. Net Savings Hero
                            NetSavingsHeroCard(data = data, terminologyMode = terminologyMode)

                            // 2. Income vs Expense Cards
                            IncomeExpenseCards(data = data, terminologyMode = terminologyMode)

                            // 3. Projected Run-Rate & Daily Velocity
                            RunRateVelocityCard(data = data)

                            // 4. Quick MoM Summary Pill Card
                            MonthlyComparisonBanner(data = data)

                            // 5. Category Quick Preview
                            CategoryPreviewCard(
                                categories = data.categoryBreakdown,
                                onSeeAll = { activeTab = "Categories" }
                            )

                            // 6. Top Merchants
                            TopMerchantsSection(merchants = data.topMerchants)

                            // 7. Subscriptions
                            if (data.subscriptions.isNotEmpty()) {
                                SubscriptionsSection(subscriptions = data.subscriptions)
                            }
                        }
                        "Categories" -> {
                            CategoryDeepDiveView(data = data)
                        }
                        "Trends" -> {
                            TrendsDeepDiveView(data = data)
                        }
                        "Comparison" -> {
                            MonthOverMonthView(data = data, terminologyMode = terminologyMode)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// -------------------------------------------------------------------------------------------------
// HEADER & CONTROLS
// -------------------------------------------------------------------------------------------------

@Composable
fun AnalyticsHeader(
    selectedMonth: Calendar,
    availableMonths: List<Pair<Calendar, Int>> = emptyList(),
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit = {}
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthString = monthFormat.format(selectedMonth.time)

    val currentCal = Calendar.getInstance()
    val isCurrentMonth = selectedMonth.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
            selectedMonth.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial Intelligence",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Text(
                    text = "Real-time metrics & automated analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Modern Month Selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = AccentBlue
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenMonthPicker() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = monthString,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select Month",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    if (isCurrentMonth) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Current",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentBlue
                            )
                        }
                    }
                }

                IconButton(onClick = onNextMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = AccentBlue
                    )
                }
            }
        }
    }
}

@Composable
fun MonthPickerDialog(
    availableMonths: List<Pair<Calendar, Int>>,
    selectedMonth: Calendar,
    onSelectMonth: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val selectedYear = selectedMonth.get(Calendar.YEAR)
    val selectedMonthNum = selectedMonth.get(Calendar.MONTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Text("Select Statement Month", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableMonths.forEach { (cal, txnCount) ->
                    val isCurrent = cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonthNum
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMonth(cal) }
                            .border(
                                1.dp,
                                if (isCurrent) AccentBlue else CardBorder,
                                RoundedCornerShape(14.dp)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) AccentBlue.copy(alpha = 0.15f) else CardBg
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthFormat.format(cal.time),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isCurrent) AccentBlue else Color.White
                            )
                            Surface(
                                color = AccentBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$txnCount txns",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentBlue
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentBlue)
            }
        }
    )
}

@Composable
fun FilterTabRow(
    tabs: List<String>,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// HERO & OVERVIEW CARDS
// -------------------------------------------------------------------------------------------------

@Composable
fun NetSavingsHeroCard(data: MonthlyFinancialData, terminologyMode: String) {
    val isPositive = data.netSavings >= 0
    val statusColor = if (isPositive) AccentGreen else AccentRed
    val savingsRateFormatted = String.format(Locale.getDefault(), "%.1f%%", data.savingsRate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.4f), CardBorder)),
                RoundedCornerShape(24.dp)
            )
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPositive) "💰" else "⚠️", fontSize = 18.sp)
                    }
                    Text(
                        text = if (terminologyMode == "CREDIT_DEBIT") "Net Cash Flow" else "Net Savings / Profit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isPositive) "$savingsRateFormatted Saved" else "Deficit",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Text(
                text = "${if (isPositive) "+" else "-"}₹${formatCurrency(Math.abs(data.netSavings))}",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isPositive) AccentGreen else AccentRed
            )

            // Savings Health Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val progress = if (data.totalIncome > 0) (data.netSavings / data.totalIncome).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = Color(0xFF1E293B),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPositive) "Healthy Savings Ratio ($savingsRateFormatted)" else "Expenses exceeded income",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${data.transactionCount} transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseCards(data: MonthlyFinancialData, terminologyMode: String) {
    val isCreditDebit = terminologyMode == "CREDIT_DEBIT"
    val incomeLabel = if (isCreditDebit) "Total Credit" else "Total Income"
    val expenseLabel = if (isCreditDebit) "Total Debit" else "Total Expense"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Income / Credit Card
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = incomeLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF94A3B8)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "₹${formatCurrency(data.totalIncome)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AccentGreen
                )

                Text(
                    text = "${data.creditCount} inflows",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Expense / Debit Card
        Card(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expenseLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF94A3B8)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "₹${formatCurrency(data.totalExpense)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AccentRed
                )

                Text(
                    text = "${data.debitCount} outflows",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun RunRateVelocityCard(data: MonthlyFinancialData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Run Rate & Pace",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Day ${data.daysPassed} of ${data.daysInMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Daily Average", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(
                        text = "₹${formatCurrency(data.dailyAverageSpend)}/day",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Projected Month-End", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text(
                        text = "₹${formatCurrency(data.projectedSpend)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonBanner(data: MonthlyFinancialData) {
    if (data.prevMonthExpense <= 0 && data.prevMonthIncome <= 0) return

    val expenseDiff = data.totalExpense - data.prevMonthExpense
    val isLowerExpense = expenseDiff <= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isLowerExpense) AccentGreen.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLowerExpense) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = if (isLowerExpense) AccentGreen else AccentRed
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val pct = String.format(Locale.getDefault(), "%.1f%%", Math.abs(data.expenseChangePercent))
                val desc = if (isLowerExpense) {
                    "Spending is down $pct vs last month (Saved ₹${formatCurrency(Math.abs(expenseDiff))})"
                } else {
                    "Spending is up $pct vs last month (+₹${formatCurrency(expenseDiff)})"
                }

                Text(
                    text = "Month-over-Month Shift",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isLowerExpense) AccentGreen else AccentRed
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CATEGORY PREVIEW & DEEP DIVE
// -------------------------------------------------------------------------------------------------

@Composable
fun CategoryPreviewCard(
    categories: List<CategoryStat>,
    onSeeAll: () -> Unit
) {
    if (categories.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Spending Categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentBlue,
                    modifier = Modifier.clickable { onSeeAll() }
                )
            }

            categories.take(4).forEach { cat ->
                CategoryProgressRow(cat = cat)
            }
        }
    }
}

@Composable
fun CategoryDeepDiveView(data: MonthlyFinancialData) {
    if (data.categoryBreakdown.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No categorized expenses for this month.", color = Color(0xFF94A3B8))
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Donut Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            isDrawHoleEnabled = true
                            setHoleColor(AndroidColor.TRANSPARENT)
                            setTransparentCircleColor(AndroidColor.TRANSPARENT)
                            holeRadius = 60f
                            transparentCircleRadius = 65f
                            setDrawCenterText(true)
                            centerText = "Expenses"
                            setCenterTextSize(15f)
                            setCenterTextColor(AndroidColor.WHITE)
                            rotationAngle = 0f
                            isRotationEnabled = true
                            legend.isEnabled = true
                            legend.textColor = AndroidColor.WHITE
                            legend.isWordWrapEnabled = true
                            setEntryLabelColor(AndroidColor.WHITE)
                            setEntryLabelTextSize(10f)
                        }
                    },
                    update = { pieChart ->
                        val entries = data.categoryBreakdown.take(7).map {
                            PieEntry(it.amount.toFloat(), it.category)
                        }
                        val colors = data.categoryBreakdown.take(7).map {
                            try { AndroidColor.parseColor(it.colorHex) } catch (e: Exception) { AndroidColor.GRAY }
                        }
                        val dataSet = PieDataSet(entries, "").apply {
                            sliceSpace = 2f
                            selectionShift = 6f
                            this.colors = colors
                            valueTextColor = AndroidColor.WHITE
                            valueTextSize = 11f
                        }
                        pieChart.data = PieData(dataSet)
                        pieChart.invalidate()
                    }
                )
            }
        }

        // All Categories Full List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "All Categories Breakdown (${data.categoryBreakdown.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                data.categoryBreakdown.forEach { cat ->
                    CategoryProgressRow(cat = cat)
                }
            }
        }
    }
}

@Composable
fun CategoryProgressRow(cat: CategoryStat) {
    val barColor = try {
        Color(android.graphics.Color.parseColor(cat.colorHex))
    } catch (e: Exception) {
        AccentBlue
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(cat.icon, fontSize = 16.sp)
                Text(
                    text = cat.category,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = "(${cat.count})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${formatCurrency(cat.amount)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", cat.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        LinearProgressIndicator(
            progress = { (cat.percentage / 100f).coerceIn(0.01f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color(0xFF1E293B)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TRENDS & CHARTS VIEW
// -------------------------------------------------------------------------------------------------

@Composable
fun TrendsDeepDiveView(data: MonthlyFinancialData) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Daily Activity Timeline BarChart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Spending Timeline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Day 1 to ${data.daysInMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    factory = { context ->
                        BarChart(context).apply {
                            description.isEnabled = false
                            legend.isEnabled = false
                            setDrawGridBackground(false)
                            axisRight.isEnabled = false
                            axisLeft.textColor = AndroidColor.WHITE
                            axisLeft.setDrawGridLines(true)
                            axisLeft.gridColor = AndroidColor.parseColor("#1E293B")
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.textColor = AndroidColor.WHITE
                            xAxis.setDrawGridLines(false)
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)
                            animateY(800)
                        }
                    },
                    update = { barChart ->
                        val entries = data.dailyTrends.map {
                            BarEntry(it.dayOfMonth.toFloat(), it.debit)
                        }
                        val dataSet = BarDataSet(entries, "Debits").apply {
                            color = AndroidColor.parseColor("#38BDF8")
                            valueTextColor = AndroidColor.TRANSPARENT
                        }
                        barChart.data = BarData(dataSet)
                        barChart.invalidate()
                    }
                )
            }
        }

        // Day of Week Pattern
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Weekly Spending Pattern",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxDaySpend = data.dayOfWeekStats.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0

                    data.dayOfWeekStats.forEach { stat ->
                        val ratio = (stat.totalAmount / maxDaySpend).toFloat().coerceIn(0.05f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(24.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(ratio)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (ratio >= 0.8f) AccentAmber else AccentBlue)
                                )
                            }
                            Text(
                                text = stat.dayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.0f", stat.percentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Largest Transactions List
        if (data.largestTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Highest Value Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    data.largestTransactions.forEach { txn ->
                        val isCred = txn.type.equals("CREDIT", true) || txn.type.equals("INCOME", true)
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = txn.merchantName.ifBlank { "Transaction" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Text(
                                    text = dateFormat.format(Date(txn.date)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Text(
                                text = "${if (isCred) "+" else "-"}₹${formatCurrency(txn.amount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isCred) AccentGreen else AccentRed
                            )
                        }
                        HorizontalDivider(color = Color(0xFF1E293B))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MONTH-OVER-MONTH (MOM) COMPARISON
// -------------------------------------------------------------------------------------------------

@Composable
fun MonthOverMonthView(data: MonthlyFinancialData, terminologyMode: String) {
    val isCreditDebit = terminologyMode == "CREDIT_DEBIT"

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Previous Month vs Current Month",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                // Spend Comparison Row
                MoMComparisonRow(
                    title = if (isCreditDebit) "Total Debit" else "Total Expenses",
                    currentValue = data.totalExpense,
                    prevValue = data.prevMonthExpense,
                    pctChange = data.expenseChangePercent,
                    isExpenseMetric = true
                )

                HorizontalDivider(color = Color(0xFF1E293B))

                // Income Comparison Row
                MoMComparisonRow(
                    title = if (isCreditDebit) "Total Credit" else "Total Income",
                    currentValue = data.totalIncome,
                    prevValue = data.prevMonthIncome,
                    pctChange = data.incomeChangePercent,
                    isExpenseMetric = false
                )

                HorizontalDivider(color = Color(0xFF1E293B))

                // Savings Comparison Row
                MoMComparisonRow(
                    title = if (isCreditDebit) "Net Cash Flow" else "Net Savings",
                    currentValue = data.netSavings,
                    prevValue = data.prevMonthSavings,
                    pctChange = data.savingsChangePercent,
                    isExpenseMetric = false
                )
            }
        }
    }
}

@Composable
fun MoMComparisonRow(
    title: String,
    currentValue: Double,
    prevValue: Double,
    pctChange: Double,
    isExpenseMetric: Boolean
) {
    val isGoodChange = if (isExpenseMetric) pctChange <= 0 else pctChange >= 0
    val trendColor = if (isGoodChange) AccentGreen else AccentRed
    val sign = if (pctChange >= 0) "+" else ""

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Surface(
                color = trendColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$sign${String.format(Locale.getDefault(), "%.1f%%", pctChange)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = trendColor
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("This Month", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Text(
                    text = "₹${formatCurrency(currentValue)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Last Month", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                Text(
                    text = "₹${formatCurrency(prevValue)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MERCHANTS & SUBSCRIPTIONS
// -------------------------------------------------------------------------------------------------

@Composable
fun TopMerchantsSection(merchants: List<MerchantStat>) {
    if (merchants.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Top Spending Merchants",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            merchants.forEachIndexed { idx, m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentBlue
                            )
                        }

                        Column {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = "${m.count} payments",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${formatCurrency(m.amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", m.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                if (idx < merchants.size - 1) {
                    HorizontalDivider(color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@Composable
fun SubscriptionsSection(subscriptions: List<SubscriptionStat>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Subscriptions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                val totalSub = subscriptions.sumOf { it.amount }
                Text(
                    text = "₹${formatCurrency(totalSub)}/mo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentPurple
                )
            }

            subscriptions.forEach { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔄", fontSize = 18.sp)
                        Column {
                            Text(
                                text = sub.merchantName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = sub.billingCycle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Text(
                        text = "₹${formatCurrency(sub.amount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// EMPTY & ERROR STATES
// -------------------------------------------------------------------------------------------------

@Composable
fun EmptyStateView(
    selectedMonth: Calendar,
    prevMonthExpense: Double,
    availableMonths: List<Pair<Calendar, Int>> = emptyList(),
    totalTransactionsCount: Int = 0,
    isScanning: Boolean = false,
    onScanSms: () -> Unit = {},
    onResetMonth: () -> Unit,
    onSelectMonth: (Calendar) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthName = monthFormat.format(selectedMonth.time)

    var hasSmsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.READ_SMS] == true && perms[android.Manifest.permission.RECEIVE_SMS] == true
        hasSmsPermission = granted
        if (granted) {
            onScanSms()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentBlue.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(2.dp, AccentBlue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "No Activity in $monthName",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (availableMonths.isNotEmpty()) {
                    "No transactions recorded for $monthName. You have transaction history in other months below:"
                } else if (prevMonthExpense > 0) {
                    "No transactions found for $monthName. In the previous month, you spent ₹${formatCurrency(prevMonthExpense)}."
                } else {
                    "We haven't detected transactions yet. Add manual transactions or sync your bank SMS messages to generate real-time analytics."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            // SMS Sync Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (!hasSmsPermission) AccentAmber.copy(alpha = 0.4f) else CardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(if (!hasSmsPermission) "🔐" else "📱", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!hasSmsPermission) "SMS Permission Required" else "Automatic SMS Sync",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (!hasSmsPermission) "Grant permission to read bank & UPI SMS alerts." else "Sync your bank debit/credit messages to auto-populate analytics.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (!hasSmsPermission) {
                                permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS))
                            } else {
                                onScanSms()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!hasSmsPermission) AccentAmber else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scanning SMS Inbox...", fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                if (!hasSmsPermission) "Grant SMS Permission & Scan" else "🔄 Scan SMS Inbox Now",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (availableMonths.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Jump to active months with data:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentBlue
                    )

                    availableMonths.take(4).forEach { (cal, count) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMonth(cal) }
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 ${monthFormat.format(cal.time)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Surface(
                                    color = AccentBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$count transactions →",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AccentBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val currentCal = Calendar.getInstance()
            val isCurrentMonth = selectedMonth.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                    selectedMonth.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH)

            if (!isCurrentMonth) {
                Button(
                    onClick = onResetMonth,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Jump to Current Month", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed, modifier = Modifier.size(36.dp))
            Text("Error loading analytics: $message", color = AccentRed, textAlign = TextAlign.Center)
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    return formatter.format(amount)
}

@Composable
fun SubscriptionTrackerCard(
    transactions: List<com.rivavafi.universal.data.local.TransactionEntity>,
    modifier: Modifier = Modifier
) {
    val subKeywords = listOf("netflix", "spotify", "prime", "hotstar", "youtube", "apple", "google", "icloud", "aws", "openai", "github", "gym", "broadband", "wifi", "airtel", "jio")
    val subTxns = transactions.filter { txn ->
        val name = (txn.merchantName ?: txn.description ?: "").lowercase()
        val cat = (txn.category ?: "").lowercase()
        subKeywords.any { name.contains(it) || cat.contains(it) } || cat.contains("subscription") || cat.contains("bill")
    }

    val groupedSubs = subTxns.groupBy { (if (it.merchantName.isNotBlank()) it.merchantName else it.description ?: "Service").trim() }
        .map { (name, txns) ->
            val latest = txns.maxByOrNull { it.date }
            Triple(name, latest?.amount ?: txns.first().amount, txns.size)
        }
        .sortedByDescending { it.second }

    val totalSubMonthly = groupedSubs.sumOf { it.second }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔄", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Subscriptions & Bills",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${groupedSubs.size} active detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Text(
                    text = "₹${formatCurrency(totalSubMonthly)}/mo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentPurple
                )
            }

            if (groupedSubs.isEmpty()) {
                Text(
                    text = "No recurring subscriptions or bill payments detected yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                groupedSubs.take(4).forEach { (name, amount, _) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "₹${formatCurrency(amount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

