package com.rivavafi.universal.ui.calculator

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.data.local.CalculatorHistoryEntity
import com.rivavafi.universal.ui.theme.AmoledBlack
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardBg = Color(0xFF141A24)
private val CardBorder = Color(0xFF1E293B)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentBlue = Color(0xFF38BDF8)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentIndigo = Color(0xFF6366F1)
private val InputBg = Color(0xFF0F172A)
 
data class ToolCardItem(
    val type: CalculatorType,
    val title: String,
    val subtitle: String,
    val tag: String,
    val color: Color,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    onBack: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel()
) {
    val currentType by viewModel.currentCalculator.collectAsState()
    val allHistory by viewModel.allHistory.collectAsState()
    val filteredHistory by viewModel.filteredHistory.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()

    var activeCalculator by remember { mutableStateOf<CalculatorType?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AmoledBlack,
        modifier = Modifier.systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Sleek minimalist top header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeCalculator != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { activeCalculator = null }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "All Tools",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBack() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Calculators",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                IconButton(onClick = { showHistorySheet = true }) {
                    if (allHistory.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = AccentBlue,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = "${allHistory.size.coerceAtMost(99)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = "Calculation History",
                                tint = AccentBlue
                            )
                        }
                    } else {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = "Calculation History",
                            tint = AccentBlue
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (activeCalculator == null) {
            // -------------------------------------------------------------
            // OPTION SELECT HUB (Grid / List of Cards)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Financial Tools & Calculators",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Text(
                        text = "Choose a tool below to calculate with custom values instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Primary Featured Calculation Tools Cards
                val tools = listOf(
                    ToolCardItem(
                        type = CalculatorType.EQUIVALENCE,
                        title = "R&E Calculator",
                        subtitle = "Ratio, Equivalence & Scaling Proportionality",
                        tag = "RATIO & EQUIV",
                        color = AccentPurple,
                        icon = "⚖️"
                    ),
                    ToolCardItem(
                        type = CalculatorType.MDR,
                        title = "MDR Calculator",
                        subtitle = "0.4% rate, ₹2,000 threshold waiver & ₹300 cap",
                        tag = "PAYMENTS",
                        color = AccentGreen,
                        icon = "💳"
                    ),
                    ToolCardItem(
                        type = CalculatorType.EMI,
                        title = "Loan EMI Calculator",
                        subtitle = "Monthly installment, total interest & loan payout",
                        tag = "BORROWING",
                        color = AccentBlue,
                        icon = "🏠"
                    ),
                    ToolCardItem(
                        type = CalculatorType.PROFIT_LOSS,
                        title = "Profit & Loss Calculator",
                        subtitle = "Gain/Loss %, cost breakdown & final amount",
                        tag = "BUSINESS",
                        color = AccentAmber,
                        icon = "💹"
                    ),
                    ToolCardItem(
                        type = CalculatorType.COMPOUND_INTEREST,
                        title = "Compound Interest (CI)",
                        subtitle = "Exponential growth with compounding periods",
                        tag = "INVESTMENT",
                        color = AccentIndigo,
                        icon = "⏳"
                    ),
                    ToolCardItem(
                        type = CalculatorType.PERCENTAGE,
                        title = "Percentage Calculator",
                        subtitle = "Direct %, value additions & subtractions",
                        tag = "UTILITY",
                        color = AccentBlue,
                        icon = "🔢"
                    ),
                    ToolCardItem(
                        type = CalculatorType.SIP,
                        title = "SIP & Wealth Builder",
                        subtitle = "Monthly SIP projection & long-term wealth",
                        tag = "GROWTH",
                        color = AccentGreen,
                        icon = "📈"
                    ),
                    ToolCardItem(
                        type = CalculatorType.GST,
                        title = "GST Calculator",
                        subtitle = "Inclusive & Exclusive tax calculations",
                        tag = "TAX",
                        color = AccentAmber,
                        icon = "📑"
                    ),
                    ToolCardItem(
                        type = CalculatorType.FD,
                        title = "Fixed Deposit (FD / RD)",
                        subtitle = "Quarterly compounded deposit maturity",
                        tag = "SAVINGS",
                        color = AccentPurple,
                        icon = "🏦"
                    ),
                    ToolCardItem(
                        type = CalculatorType.INFLATION,
                        title = "Inflation Impact",
                        subtitle = "Future cost & purchasing power erosion",
                        tag = "PLANNING",
                        color = AccentRed,
                        icon = "📊"
                    )
                )

                tools.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                viewModel.setCalculatorType(item.type)
                                activeCalculator = item.type
                            }
                            .border(1.dp, item.color.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(item.color.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.icon, fontSize = 20.sp)
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            color = item.color.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = item.tag,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                                color = item.color,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = item.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Recent History preview card if available
                if (allHistory.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    RecentHistoryPreviewCard(
                        recentItems = allHistory.take(2),
                        onOpenHistory = { showHistorySheet = true },
                        onReopen = {
                            viewModel.reopenCalculation(it)
                            activeCalculator = CalculatorType.valueOf(it.calculatorType)
                        }
                    )
                }

                Spacer(Modifier.height(140.dp))
            }
        } else {
            // -------------------------------------------------------------
            // ACTIVE CALCULATOR VIEW (Interactive screen with custom inputs)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentType.displayName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            text = "Enter your custom values below for real-time results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Dynamic Calculator Content
                when (currentType) {
                    CalculatorType.PERCENTAGE -> PercentageCalculatorSection(viewModel)
                    CalculatorType.EQUIVALENCE -> EquivalenceCalculatorSection(viewModel)
                    CalculatorType.PROFIT_LOSS -> ProfitLossCalculatorSection(viewModel)
                    CalculatorType.COMPOUND_INTEREST -> CompoundInterestCalculatorSection(viewModel)
                    CalculatorType.MDR -> MdrCalculatorSection(viewModel)
                    CalculatorType.EMI -> EmiCalculatorSection(viewModel)
                    CalculatorType.SIP -> SipCalculatorSection(viewModel)
                    CalculatorType.LUMPSUM -> LumpsumCalculatorSection(viewModel)
                    CalculatorType.FD -> FdCalculatorSection(viewModel)
                    CalculatorType.RD -> RdCalculatorSection(viewModel)
                    CalculatorType.GST -> GstCalculatorSection(viewModel)
                    CalculatorType.INFLATION -> InflationCalculatorSection(viewModel)
                }

                // Save to History CTA Button
                Button(
                    onClick = {
                        viewModel.saveCurrentCalculation()
                        scope.launch {
                            snackbarHostState.showSnackbar("Calculation saved to history! 📜")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Calculation History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Calculation History Bottom Sheet / Dialog
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = Color(0xFF0F172A),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF475569)) }
        ) {
            CalculationHistorySheetContent(
                historyList = filteredHistory,
                activeFilter = historyFilter,
                onFilterSelected = { viewModel.setHistoryFilter(it) },
                onReopen = {
                    viewModel.reopenCalculation(it)
                    showHistorySheet = false
                },
                onDelete = { viewModel.deleteHistoryItem(it.id) },
                onClearAll = { viewModel.clearHistory() },
                onClose = { showHistorySheet = false }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CATEGORY FILTER BAR
// -------------------------------------------------------------------------------------------------

@Composable
fun CategoryFilterBar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("ALL" to "All Tools", "Tools" to "⚡ Tools", "Borrowing" to "🏠 Loans", "Investment" to "📈 Investments", "Savings" to "🏦 Savings", "Tax" to "📑 Tax")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        items(categories) { (key, label) ->
            val isSelected = selectedCategory == key
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onCategorySelected(key) },
                color = if (isSelected) Color(0xFF2563EB) else CardBg,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) AccentBlue else CardBorder
                )
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CALCULATOR TYPE SELECTOR
// -------------------------------------------------------------------------------------------------

@Composable
fun CalculatorTypeSelector(
    selectedType: CalculatorType,
    selectedCategory: String,
    onTypeSelected: (CalculatorType) -> Unit
) {
    val filteredTypes = if (selectedCategory == "ALL") {
        CalculatorType.values().toList()
    } else {
        CalculatorType.values().filter { it.category == selectedCategory }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(filteredTypes) { type ->
            val isSelected = selectedType == type
            Card(
                modifier = Modifier
                    .clickable { onTypeSelected(type) }
                    .border(
                        1.dp,
                        if (isSelected) AccentBlue else CardBorder,
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) AccentBlue.copy(alpha = 0.15f) else CardBg
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(type.icon, fontSize = 16.sp)
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. PERCENTAGE CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun PercentageCalculatorSection(viewModel: CalculatorViewModel) {
    val percent by viewModel.percentValue.collectAsState()
    val baseAmount by viewModel.percentBaseAmount.collectAsState()

    val result = viewModel.calculatePercentage()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Hero
        ResultHeroCard(
            title = "${CalculatorViewModel.formatDecimal(percent)}% of ₹${CalculatorViewModel.formatCurrency(baseAmount)}",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.resultAmount)}",
            pills = listOf(
                ResultPill("Base Value", "₹${CalculatorViewModel.formatCurrency(baseAmount)}", AccentBlue),
                ResultPill("Value + ${CalculatorViewModel.formatDecimal(percent)}%", "₹${CalculatorViewModel.formatCurrency(result.addedTotal)}", AccentGreen),
                ResultPill("Value - ${CalculatorViewModel.formatDecimal(percent)}%", "₹${CalculatorViewModel.formatCurrency(result.subtractedTotal)}", AccentAmber)
            )
        )

        // Percentage Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick Percentage Presets", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5.0, 10.0, 18.0, 20.0, 25.0, 40.0, 50.0).forEach { p ->
                    val isSelected = percent == p
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.percentValue.value = p },
                        color = if (isSelected) AccentBlue else CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else CardBorder)
                    ) {
                        Text(
                            text = "${p.toInt()}%",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Direct Text & Slider Input for Percentage
        CalculatorNumberInput(
            title = "Percentage (%)",
            value = percent,
            unit = "%",
            min = 0.5f,
            max = 100f,
            step = 0.5f,
            onValueChange = { viewModel.percentValue.value = it }
        )

        // Direct Text & Slider Input for Base Amount
        CalculatorNumberInput(
            title = "Base Amount",
            value = baseAmount,
            unit = "₹",
            min = 1f,
            max = 1000000f,
            step = 100f,
            onValueChange = { viewModel.percentBaseAmount.value = it }
        )

        // Formula Explain Card
        FormulaExplainerCard(
            title = "Formula Applied",
            formula = "(${percent}% ÷ 100) × ₹${CalculatorViewModel.formatCurrency(baseAmount)} = ₹${CalculatorViewModel.formatCurrency(result.resultAmount)}"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 2. EQUIVALENCE / RATIO CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun EquivalenceCalculatorSection(viewModel: CalculatorViewModel) {
    val valA by viewModel.ratioValA.collectAsState()
    val valB by viewModel.ratioValB.collectAsState()
    val valC by viewModel.ratioValC.collectAsState()

    val result = viewModel.calculateRatio()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Equation Visual Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.horizontalGradient(listOf(AccentPurple.copy(alpha = 0.4f), CardBorder)), RoundedCornerShape(24.dp))
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Equivalence & Proportionality",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${CalculatorViewModel.formatDecimal(valA)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentBlue
                    )
                    Text("  =  ", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF64748B))
                    Text(
                        text = "${CalculatorViewModel.formatDecimal(valB)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${CalculatorViewModel.formatDecimal(valC)}",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentAmber
                    )
                    Text("  =  ", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF64748B))
                    Text(
                        text = "${CalculatorViewModel.formatDecimal(result.valD)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Unit Rate (1 A = ? B)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        Text("1 = ${CalculatorViewModel.formatDecimal(result.unitRate)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AccentPurple)
                    }
                    Column {
                        Text("Multiplier (C / A)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        Text("${CalculatorViewModel.formatDecimal(result.multiplier)}x", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AccentBlue)
                    }
                    Column {
                        Text("Calculated Result", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        Text(CalculatorViewModel.formatDecimal(result.valD), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = AccentGreen)
                    }
                }
            }
        }

        // Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sample Scenarios", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.ratioValA.value = 100.0
                            viewModel.ratioValB.value = 450.0
                            viewModel.ratioValC.value = 200.0
                        },
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "100 = 450 → 200 = ?",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentBlue
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.ratioValA.value = 1.0
                            viewModel.ratioValB.value = 83.0
                            viewModel.ratioValC.value = 50.0
                        },
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Text(
                        text = "1 = 83 → 50 = ?",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentGreen
                    )
                }
            }
        }

        // Inputs
        CalculatorNumberInput(
            title = "First Value (A)",
            value = valA,
            unit = "",
            min = 1f,
            max = 100000f,
            step = 10f,
            onValueChange = { viewModel.ratioValA.value = it }
        )

        CalculatorNumberInput(
            title = "Equivalent Value (B)",
            value = valB,
            unit = "",
            min = 1f,
            max = 100000f,
            step = 10f,
            onValueChange = { viewModel.ratioValB.value = it }
        )

        CalculatorNumberInput(
            title = "Target Comparison Value (C)",
            value = valC,
            unit = "",
            min = 1f,
            max = 100000f,
            step = 10f,
            onValueChange = { viewModel.ratioValC.value = it }
        )

        FormulaExplainerCard(
            title = "Equivalence Formula",
            formula = "? = (B × C) ÷ A = (${valB} × ${valC}) ÷ ${valA} = ${CalculatorViewModel.formatDecimal(result.valD)}"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 3. PROFIT & LOSS CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun ProfitLossCalculatorSection(viewModel: CalculatorViewModel) {
    val originalAmount by viewModel.plOriginalAmount.collectAsState()
    val percentage by viewModel.plPercentage.collectAsState()
    val isProfit by viewModel.plIsProfit.collectAsState()

    val result = viewModel.calculateProfitLoss()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Profit / Loss Toggle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(InputBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isProfit) AccentGreen else Color.Transparent)
                    .clickable { viewModel.plIsProfit.value = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📈 Profit (+)",
                    color = if (isProfit) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isProfit) AccentRed else Color.Transparent)
                    .clickable { viewModel.plIsProfit.value = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📉 Loss (-)",
                    color = if (!isProfit) Color.White else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Result Hero Card
        ResultHeroCard(
            title = "${CalculatorViewModel.formatDecimal(percentage)}% ${if (isProfit) "Profit" else "Loss"} on ₹${CalculatorViewModel.formatCurrency(originalAmount)}",
            primaryResult = "${if (isProfit) "+" else "-"}₹${CalculatorViewModel.formatCurrency(result.profitOrLossAmount)}",
            pills = listOf(
                ResultPill("Original Amount", "₹${CalculatorViewModel.formatCurrency(originalAmount)}", AccentBlue),
                ResultPill("Percentage", "${CalculatorViewModel.formatDecimal(percentage)}%", if (isProfit) AccentGreen else AccentRed),
                ResultPill("Final Amount", "₹${CalculatorViewModel.formatCurrency(result.finalAmount)}", if (isProfit) AccentGreen else AccentRed)
            )
        )

        // Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Examples", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.plOriginalAmount.value = 10000.0
                            viewModel.plPercentage.value = 40.0
                            viewModel.plIsProfit.value = true
                        },
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isProfit && percentage == 40.0 && originalAmount == 10000.0) AccentGreen else CardBorder)
                ) {
                    Text(
                        text = "40% profit on ₹10,000",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentGreen
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.plOriginalAmount.value = 10000.0
                            viewModel.plPercentage.value = 20.0
                            viewModel.plIsProfit.value = false
                        },
                    color = CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (!isProfit && percentage == 20.0 && originalAmount == 10000.0) AccentRed else CardBorder)
                ) {
                    Text(
                        text = "20% loss on ₹10,000",
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentRed
                    )
                }
            }
        }

        // Inputs
        CalculatorNumberInput(
            title = "Original / Cost Amount",
            value = originalAmount,
            unit = "₹",
            min = 100f,
            max = 10000000f,
            step = 500f,
            onValueChange = { viewModel.plOriginalAmount.value = it }
        )

        CalculatorNumberInput(
            title = "${if (isProfit) "Profit" else "Loss"} Percentage",
            value = percentage,
            unit = "%",
            min = 0.5f,
            max = 100f,
            step = 0.5f,
            onValueChange = { viewModel.plPercentage.value = it }
        )

        FormulaExplainerCard(
            title = "Calculation Breakdown",
            formula = "${if (isProfit) "Profit" else "Loss"} = (${percentage}% × ₹${CalculatorViewModel.formatCurrency(originalAmount)}) = ₹${CalculatorViewModel.formatCurrency(result.profitOrLossAmount)}\nFinal Amount = ₹${CalculatorViewModel.formatCurrency(originalAmount)} ${if (isProfit) "+" else "-"} ₹${CalculatorViewModel.formatCurrency(result.profitOrLossAmount)} = ₹${CalculatorViewModel.formatCurrency(result.finalAmount)}"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 4. COMPOUND INTEREST CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun CompoundInterestCalculatorSection(viewModel: CalculatorViewModel) {
    val principal by viewModel.ciPrincipal.collectAsState()
    val rate by viewModel.ciAnnualRate.collectAsState()
    val years by viewModel.ciTenureYears.collectAsState()
    val frequency by viewModel.ciFrequency.collectAsState()

    val result = viewModel.calculateCompoundInterest()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Hero
        ResultHeroCard(
            title = "Final Maturity Value",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.finalAmount)}",
            pills = listOf(
                ResultPill("Initial Principal", "₹${CalculatorViewModel.formatCurrency(principal)}", AccentBlue),
                ResultPill("Interest Rate", "${CalculatorViewModel.formatDecimal(rate)}% p.a.", AccentPurple),
                ResultPill("Duration", "$years Years", AccentAmber),
                ResultPill("Total Growth", "+₹${CalculatorViewModel.formatCurrency(result.totalInterest)}", AccentGreen)
            )
        )

        // Compounding Frequency Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Compounding Frequency", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1 to "Annually", 2 to "Half-Yearly", 4 to "Quarterly", 12 to "Monthly").forEach { (freq, label) ->
                    val isSelected = frequency == freq
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.ciFrequency.value = freq },
                        color = if (isSelected) AccentBlue else CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else CardBorder)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Example Preset
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    viewModel.ciPrincipal.value = 1000.0
                    viewModel.ciAnnualRate.value = 20.0
                    viewModel.ciTenureYears.value = 10
                    viewModel.ciFrequency.value = 1
                },
            color = CardBg,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Text(
                text = "⚡ Example: ₹1,000 invested for 10 years @ 20% interest",
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentBlue
            )
        }

        // Inputs
        CalculatorNumberInput(
            title = "Initial Investment (Principal)",
            value = principal,
            unit = "₹",
            min = 500f,
            max = 10000000f,
            step = 500f,
            onValueChange = { viewModel.ciPrincipal.value = it }
        )

        CalculatorNumberInput(
            title = "Annual Interest Rate",
            value = rate,
            unit = "%",
            min = 1f,
            max = 50f,
            step = 0.5f,
            onValueChange = { viewModel.ciAnnualRate.value = it }
        )

        CalculatorNumberInput(
            title = "Investment Duration",
            value = years.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 50f,
            step = 1f,
            onValueChange = { viewModel.ciTenureYears.value = it.toInt() }
        )

        FormulaExplainerCard(
            title = "Compound Interest Formula",
            formula = "A = P(1 + r/n)^(nt)\n₹${CalculatorViewModel.formatCurrency(principal)} × (1 + ${rate}%/${frequency})^(${frequency} × ${years}) = ₹${CalculatorViewModel.formatCurrency(result.finalAmount)} (${CalculatorViewModel.formatDecimal(result.growthMultiplier)}x Growth)"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 5. MDR CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun MdrCalculatorSection(viewModel: CalculatorViewModel) {
    val amount by viewModel.mdrTransactionAmount.collectAsState()
    val rate by viewModel.mdrRate.collectAsState()
    val threshold by viewModel.mdrThreshold.collectAsState()
    val cap by viewModel.mdrCap.collectAsState()

    val result = viewModel.calculateMdr()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status Badge Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (result.isThresholdWaived) AccentGreen else if (result.isCapped) AccentAmber else AccentBlue,
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = (if (result.isThresholdWaived) AccentGreen else if (result.isCapped) AccentAmber else AccentBlue).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (result.isThresholdWaived) "✅" else if (result.isCapped) "🔒" else "⚡",
                    fontSize = 20.sp
                )
                Column {
                    Text(
                        text = if (result.isThresholdWaived) "Exempt below ₹2,000 threshold" else if (result.isCapped) "Maximum Cap of ₹300 Applied" else "0.4% Standard MDR Applied",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = if (result.isThresholdWaived) "Transactions ≤ ₹2,000 incur 0% MDR fee (100% merchant payout)"
                        else if (result.isCapped) "MDR capped at max limit ₹${cap.toInt()}. Saved ₹${CalculatorViewModel.formatCurrency(result.calculatedMdr - result.actualMdr)}!"
                        else "0.4% charge on amounts > ₹2,000 (below ₹300 cap limit)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Result Hero
        ResultHeroCard(
            title = "MDR Charge on ₹${CalculatorViewModel.formatCurrency(amount)}",
            primaryResult = "₹${CalculatorViewModel.formatDecimal(result.actualMdr)}",
            pills = listOf(
                ResultPill("Transaction Amount", "₹${CalculatorViewModel.formatCurrency(amount)}", AccentBlue),
                ResultPill("Effective Rate", "${CalculatorViewModel.formatDecimal(result.effectiveRatePercent, 3)}%", if (result.isThresholdWaived) AccentGreen else AccentPurple),
                ResultPill("Net Merchant Payout", "₹${CalculatorViewModel.formatCurrency(result.netPayout)}", AccentGreen)
            )
        )

        // Quick Amount Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Test Amounts (Below & Above ₹2,000 Threshold)", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1500.0, 2000.0, 5000.0, 25000.0, 75000.0, 100000.0).forEach { testAmt ->
                    val isSelected = amount == testAmt
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.mdrTransactionAmount.value = testAmt },
                        color = if (isSelected) AccentBlue else CardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else CardBorder)
                    ) {
                        Text(
                            text = if (testAmt >= 1000) "₹${testAmt.toInt() / 1000}k" else "₹${testAmt.toInt()}",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Inputs
        CalculatorNumberInput(
            title = "Transaction Amount",
            value = amount,
            unit = "₹",
            min = 100f,
            max = 500000f,
            step = 500f,
            onValueChange = { viewModel.mdrTransactionAmount.value = it }
        )

        FormulaExplainerCard(
            title = "MDR Rule Specifications",
            formula = "• Amounts ≤ ₹2,000: 0% MDR (Waived)\n• Amounts > ₹2,000: 0.4% of amount\n• Maximum Capped Charge: ₹300.00\n\nCalculation for ₹${CalculatorViewModel.formatCurrency(amount)}:\n${if (amount <= 2000) "Amount ≤ ₹2,000 → MDR = ₹0.00" else "0.4% × ₹${CalculatorViewModel.formatCurrency(amount)} = ₹${CalculatorViewModel.formatDecimal(result.calculatedMdr)} ${if (result.isCapped) "→ Capped at ₹300.00" else "→ ₹${CalculatorViewModel.formatDecimal(result.actualMdr)}"}"}"
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 6. EMI CALCULATOR SECTION WITH VISUAL BREAKDOWN CHART
// -------------------------------------------------------------------------------------------------

@Composable
fun EmiCalculatorSection(viewModel: CalculatorViewModel) {
    val loanAmount by viewModel.emiLoanAmount.collectAsState()
    val interestRate by viewModel.emiInterestRate.collectAsState()
    val tenureYears by viewModel.emiTenureYears.collectAsState()

    val result = viewModel.calculateEmi()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result Hero
        ResultHeroCard(
            title = "Monthly Loan EMI",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.monthlyEmi)} / mo",
            pills = listOf(
                ResultPill("Principal Amount", "₹${CalculatorViewModel.formatCurrency(loanAmount)}", AccentBlue),
                ResultPill("Total Interest", "₹${CalculatorViewModel.formatCurrency(result.totalInterest)}", AccentRed),
                ResultPill("Total Payable", "₹${CalculatorViewModel.formatCurrency(result.totalPayment)}", AccentAmber)
            )
        )

        // Visual Breakdown Donut Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Breakdown of Total Payable Amount",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 22f
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            val principalSweep = (result.principalPercent / 100f) * 360f
                            val interestSweep = 360f - principalSweep

                            // Principal Arc
                            drawArc(
                                color = AccentBlue,
                                startAngle = -90f,
                                sweepAngle = principalSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Interest Arc
                            drawArc(
                                color = AccentRed,
                                startAngle = -90f + principalSweep,
                                sweepAngle = interestSweep,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${tenureYears}y",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Tenure",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Legend & Percentage Breakdown
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Principal Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(AccentBlue, CircleShape))
                                Text("Principal", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", result.principalPercent)}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentBlue
                            )
                        }

                        // Interest Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(AccentRed, CircleShape))
                                Text("Total Interest", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", result.interestPercent)}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentRed
                            )
                        }

                        // Progress Bar Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight((result.principalPercent / 100f).coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(AccentBlue)
                            )
                            Box(
                                modifier = Modifier
                                    .weight((result.interestPercent / 100f).coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(AccentRed)
                            )
                        }
                    }
                }
            }
        }

        CalculatorNumberInput(
            title = "Loan Amount",
            value = loanAmount,
            unit = "₹",
            min = 10000f,
            max = 10000000f,
            step = 10000f,
            onValueChange = { viewModel.emiLoanAmount.value = it }
        )

        CalculatorNumberInput(
            title = "Interest Rate (p.a)",
            value = interestRate,
            unit = "%",
            min = 1f,
            max = 25f,
            step = 0.25f,
            onValueChange = { viewModel.emiInterestRate.value = it }
        )

        CalculatorNumberInput(
            title = "Loan Tenure",
            value = tenureYears.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 30f,
            step = 1f,
            onValueChange = { viewModel.emiTenureYears.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 7. SIP CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun SipCalculatorSection(viewModel: CalculatorViewModel) {
    val monthlyAmount by viewModel.sipMonthlyAmount.collectAsState()
    val returnRate by viewModel.sipExpectedReturnRate.collectAsState()
    val tenureYears by viewModel.sipTenureYears.collectAsState()

    val result = viewModel.calculateSip()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = "Expected Maturity Amount",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.totalValue)}",
            pills = listOf(
                ResultPill("Invested Amount", "₹${CalculatorViewModel.formatCurrency(result.investedAmount)}", AccentBlue),
                ResultPill("Est. Wealth Gain", "₹${CalculatorViewModel.formatCurrency(result.estimatedReturns)}", AccentGreen)
            )
        )

        CalculatorNumberInput(
            title = "Monthly Investment",
            value = monthlyAmount,
            unit = "₹",
            min = 500f,
            max = 200000f,
            step = 500f,
            onValueChange = { viewModel.sipMonthlyAmount.value = it }
        )

        CalculatorNumberInput(
            title = "Expected Return Rate (p.a)",
            value = returnRate,
            unit = "%",
            min = 1f,
            max = 30f,
            step = 0.5f,
            onValueChange = { viewModel.sipExpectedReturnRate.value = it }
        )

        CalculatorNumberInput(
            title = "Time Period",
            value = tenureYears.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 40f,
            step = 1f,
            onValueChange = { viewModel.sipTenureYears.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 8. LUMP SUM CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun LumpsumCalculatorSection(viewModel: CalculatorViewModel) {
    val amount by viewModel.lumpsumAmount.collectAsState()
    val rate by viewModel.lumpsumReturnRate.collectAsState()
    val years by viewModel.lumpsumTenureYears.collectAsState()

    val result = viewModel.calculateLumpsum()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = "Total Future Value",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.totalValue)}",
            pills = listOf(
                ResultPill("Initial Investment", "₹${CalculatorViewModel.formatCurrency(result.investedAmount)}", AccentBlue),
                ResultPill("Est. Returns", "₹${CalculatorViewModel.formatCurrency(result.estimatedReturns)}", AccentGreen)
            )
        )

        CalculatorNumberInput(
            title = "Total Investment",
            value = amount,
            unit = "₹",
            min = 5000f,
            max = 20000000f,
            step = 5000f,
            onValueChange = { viewModel.lumpsumAmount.value = it }
        )

        CalculatorNumberInput(
            title = "Expected Return Rate (p.a)",
            value = rate,
            unit = "%",
            min = 1f,
            max = 30f,
            step = 0.5f,
            onValueChange = { viewModel.lumpsumReturnRate.value = it }
        )

        CalculatorNumberInput(
            title = "Time Period",
            value = years.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 40f,
            step = 1f,
            onValueChange = { viewModel.lumpsumTenureYears.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 9. FD CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun FdCalculatorSection(viewModel: CalculatorViewModel) {
    val amount by viewModel.fdAmount.collectAsState()
    val rate by viewModel.fdInterestRate.collectAsState()
    val years by viewModel.fdTenureYears.collectAsState()

    val result = viewModel.calculateFd()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = "Maturity Amount (Compounded)",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.maturityAmount)}",
            pills = listOf(
                ResultPill("Principal Deposit", "₹${CalculatorViewModel.formatCurrency(result.investedAmount)}", AccentBlue),
                ResultPill("Total Interest", "₹${CalculatorViewModel.formatCurrency(result.totalInterest)}", AccentGreen)
            )
        )

        CalculatorNumberInput(
            title = "Deposit Amount",
            value = amount,
            unit = "₹",
            min = 1000f,
            max = 10000000f,
            step = 5000f,
            onValueChange = { viewModel.fdAmount.value = it }
        )

        CalculatorNumberInput(
            title = "Rate of Interest (p.a)",
            value = rate,
            unit = "%",
            min = 2f,
            max = 15f,
            step = 0.1f,
            onValueChange = { viewModel.fdInterestRate.value = it }
        )

        CalculatorNumberInput(
            title = "Tenure",
            value = years.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 10f,
            step = 1f,
            onValueChange = { viewModel.fdTenureYears.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 10. RD CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun RdCalculatorSection(viewModel: CalculatorViewModel) {
    val monthly by viewModel.rdMonthlyDeposit.collectAsState()
    val rate by viewModel.rdInterestRate.collectAsState()
    val months by viewModel.rdTenureMonths.collectAsState()

    val result = viewModel.calculateRd()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = "Maturity Value",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.maturityValue)}",
            pills = listOf(
                ResultPill("Total Invested", "₹${CalculatorViewModel.formatCurrency(result.investedAmount)}", AccentBlue),
                ResultPill("Total Interest", "₹${CalculatorViewModel.formatCurrency(result.totalInterest)}", AccentGreen)
            )
        )

        CalculatorNumberInput(
            title = "Monthly Deposit",
            value = monthly,
            unit = "₹",
            min = 500f,
            max = 500000f,
            step = 500f,
            onValueChange = { viewModel.rdMonthlyDeposit.value = it }
        )

        CalculatorNumberInput(
            title = "Rate of Interest (p.a)",
            value = rate,
            unit = "%",
            min = 2f,
            max = 15f,
            step = 0.1f,
            onValueChange = { viewModel.rdInterestRate.value = it }
        )

        CalculatorNumberInput(
            title = "Tenure",
            value = months.toDouble(),
            unit = "Mos",
            min = 6f,
            max = 120f,
            step = 6f,
            onValueChange = { viewModel.rdTenureMonths.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 11. GST CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun GstCalculatorSection(viewModel: CalculatorViewModel) {
    val amount by viewModel.gstAmount.collectAsState()
    val rate by viewModel.gstRate.collectAsState()
    val isInclusive by viewModel.isGstInclusive.collectAsState()

    val result = viewModel.calculateGst()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = if (isInclusive) "Total Amount (Inc. GST)" else "Total Amount (GST Added)",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.totalAmount)}",
            pills = listOf(
                ResultPill("Net Price", "₹${CalculatorViewModel.formatCurrency(result.netAmount)}", AccentBlue),
                ResultPill("GST Amount", "₹${CalculatorViewModel.formatCurrency(result.gstAmount)}", AccentAmber)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(InputBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isInclusive) Color(0xFF2563EB) else Color.Transparent)
                    .clickable { viewModel.isGstInclusive.value = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GST Exclusive (+)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isInclusive) Color(0xFF2563EB) else Color.Transparent)
                    .clickable { viewModel.isGstInclusive.value = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("GST Inclusive (Included)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        CalculatorNumberInput(
            title = "Base Amount",
            value = amount,
            unit = "₹",
            min = 100f,
            max = 10000000f,
            step = 500f,
            onValueChange = { viewModel.gstAmount.value = it }
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("GST Rate Slab", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5.0, 12.0, 18.0, 28.0).forEach { slab ->
                    val isSelected = rate == slab
                    Button(
                        onClick = { viewModel.gstRate.value = slab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AccentBlue else InputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("$slab%", fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 12. INFLATION CALCULATOR SECTION
// -------------------------------------------------------------------------------------------------

@Composable
fun InflationCalculatorSection(viewModel: CalculatorViewModel) {
    val cost by viewModel.inflationPresentCost.collectAsState()
    val rate by viewModel.inflationRate.collectAsState()
    val years by viewModel.inflationTenureYears.collectAsState()

    val result = viewModel.calculateInflation()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResultHeroCard(
            title = "Future Purchasing Cost",
            primaryResult = "₹${CalculatorViewModel.formatCurrency(result.futureCost)}",
            pills = listOf(
                ResultPill("Present Cost", "₹${CalculatorViewModel.formatCurrency(cost)}", AccentBlue),
                ResultPill("Erosion / Hike", "+₹${CalculatorViewModel.formatCurrency(result.costIncrease)}", AccentRed)
            )
        )

        CalculatorNumberInput(
            title = "Current Cost / Expense",
            value = cost,
            unit = "₹",
            min = 1000f,
            max = 50000000f,
            step = 5000f,
            onValueChange = { viewModel.inflationPresentCost.value = it }
        )

        CalculatorNumberInput(
            title = "Expected Inflation Rate (p.a)",
            value = rate,
            unit = "%",
            min = 1f,
            max = 20f,
            step = 0.5f,
            onValueChange = { viewModel.inflationRate.value = it }
        )

        CalculatorNumberInput(
            title = "Years From Now",
            value = years.toDouble(),
            unit = "Yrs",
            min = 1f,
            max = 40f,
            step = 1f,
            onValueChange = { viewModel.inflationTenureYears.value = it.toInt() }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// REUSABLE UI COMPONENTS (RESULT HERO, INPUTS, FORMULA CARDS, HISTORY)
// -------------------------------------------------------------------------------------------------

data class ResultPill(val label: String, val value: String, val color: Color)

@Composable
fun ResultHeroCard(
    title: String,
    primaryResult: String,
    pills: List<ResultPill>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(AccentBlue.copy(alpha = 0.4f), CardBorder)),
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = primaryResult,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
            }

            HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                pills.forEach { pill ->
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = pill.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pill.value,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = pill.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorNumberInput(
    title: String,
    value: Double,
    unit: String,
    min: Float,
    max: Float,
    step: Float,
    onValueChange: (Double) -> Unit
) {
    var isEditingText by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)) }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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

                if (isEditingText) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            it.toDoubleOrNull()?.let { num ->
                                onValueChange(num)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isEditingText = false
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = InputBg,
                            unfocusedContainerColor = InputBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                } else {
                    Surface(
                        modifier = Modifier.clickable { isEditingText = true },
                        color = AccentBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (unit == "₹") "₹${CalculatorViewModel.formatCurrency(value)}" else if (unit == "%") "${CalculatorViewModel.formatDecimal(value)}%" else "${CalculatorViewModel.formatDecimal(value)} $unit",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentBlue
                            )
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit value",
                                tint = AccentBlue,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Slider(
                value = value.toFloat().coerceIn(min, max),
                onValueChange = { onValueChange(it.toDouble()) },
                valueRange = min..max,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                    inactiveTrackColor = Color(0xFF334155)
                )
            )
        }
    }
}

@Composable
fun FormulaExplainerCard(title: String, formula: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = InputBg)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("💡", fontSize = 14.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentBlue
                )
            }
            Text(
                text = formula,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CALCULATION HISTORY BOTTOM SHEET CONTENT
// -------------------------------------------------------------------------------------------------

@Composable
fun CalculationHistorySheetContent(
    historyList: List<CalculatorHistoryEntity>,
    activeFilter: String,
    onFilterSelected: (String) -> Unit,
    onReopen: (CalculatorHistoryEntity) -> Unit,
    onDelete: (CalculatorHistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Clear Calculation History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all saved calculations.", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                Text("📜", fontSize = 20.sp)
                Text(
                    text = "Calculation History",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            if (historyList.isNotEmpty()) {
                TextButton(onClick = { showClearConfirmDialog = true }) {
                    Text("Clear All", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        val filters = listOf("ALL") + CalculatorType.values().map { it.name }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                val isSelected = activeFilter == filter
                val label = if (filter == "ALL") "All Types" else filter
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFilterSelected(filter) },
                    color = if (isSelected) Color(0xFF2563EB) else CardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentBlue else CardBorder)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else Color(0xFF94A3B8)
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🧮", fontSize = 48.sp)
                    Text(
                        text = "No calculations saved yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Your calculation history and saved estimates will show up here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        formattedDate = dateFormat.format(Date(item.timestamp)),
                        onReopen = { onReopen(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: CalculatorHistoryEntity,
    formattedDate: String,
    onReopen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Surface(
                        color = AccentBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.calculatorType,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentBlue
                        )
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete Item",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.primaryResult,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AccentGreen
                    )
                    if (item.secondaryResult.isNotBlank()) {
                        Text(
                            text = item.secondaryResult,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = onReopen,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Reopen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun RecentHistoryPreviewCard(
    recentItems: List<CalculatorHistoryEntity>,
    onOpenHistory: () -> Unit,
    onReopen: (CalculatorHistoryEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Calculations",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "See All (${recentItems.size}+) →",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentBlue,
                    modifier = Modifier.clickable { onOpenHistory() }
                )
            }

            recentItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onReopen(item) }
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.calculatorType} • ${item.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.primaryResult,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentGreen
                        )
                    }
                    Text("Tap to reuse", style = MaterialTheme.typography.labelSmall, color = AccentBlue)
                }
            }
        }
    }
}
