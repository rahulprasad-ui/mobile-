package com.rivavafi.universal.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.local.CalculatorHistoryEntity
import com.rivavafi.universal.data.repository.CalculatorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

enum class CalculatorType(val displayName: String, val icon: String, val category: String) {
    PERCENTAGE("Percentage", "🔢", "Tools"),
    EQUIVALENCE("Ratio & Equivalence", "⚖️", "Tools"),
    PROFIT_LOSS("Profit & Loss", "💹", "Tools"),
    COMPOUND_INTEREST("Compound Interest", "⏳", "Tools"),
    MDR("MDR Calculator", "💳", "Tools"),
    EMI("Loan EMI", "🏠", "Borrowing"),
    SIP("SIP Calculator", "📈", "Investment"),
    LUMPSUM("Lump Sum", "💰", "Investment"),
    FD("Fixed Deposit", "🏦", "Savings"),
    RD("Recurring Deposit", "🔄", "Savings"),
    GST("GST Calculator", "📑", "Tax"),
    INFLATION("Inflation", "📊", "Planning")
}

data class PercentageResult(
    val percentage: Double = 0.0,
    val baseAmount: Double = 0.0,
    val resultAmount: Double = 0.0,
    val addedTotal: Double = 0.0,
    val subtractedTotal: Double = 0.0
)

data class RatioResult(
    val valA: Double = 0.0,
    val valB: Double = 0.0,
    val valC: Double = 0.0,
    val valD: Double = 0.0,
    val unitRate: Double = 0.0,
    val multiplier: Double = 0.0
)

data class ProfitLossResult(
    val originalAmount: Double = 0.0,
    val percentage: Double = 0.0,
    val isProfit: Boolean = true,
    val profitOrLossAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val marginPercent: Double = 0.0
)

data class CompoundInterestResult(
    val principal: Double = 0.0,
    val annualRate: Double = 0.0,
    val tenureYears: Int = 0,
    val frequencyPerYear: Int = 1,
    val finalAmount: Double = 0.0,
    val totalInterest: Double = 0.0,
    val growthMultiplier: Double = 1.0
)

data class MdrResult(
    val transactionAmount: Double = 0.0,
    val mdrRate: Double = 0.4,
    val threshold: Double = 2000.0,
    val capAmount: Double = 300.0,
    val calculatedMdr: Double = 0.0,
    val actualMdr: Double = 0.0,
    val isThresholdWaived: Boolean = false,
    val isCapped: Boolean = false,
    val netPayout: Double = 0.0,
    val effectiveRatePercent: Double = 0.0
)

data class SipResult(
    val investedAmount: Double = 0.0,
    val estimatedReturns: Double = 0.0,
    val totalValue: Double = 0.0
)

data class EmiResult(
    val monthlyEmi: Double = 0.0,
    val totalInterest: Double = 0.0,
    val totalPayment: Double = 0.0,
    val principalPercent: Float = 50f,
    val interestPercent: Float = 50f
)

data class LumpsumResult(
    val investedAmount: Double = 0.0,
    val estimatedReturns: Double = 0.0,
    val totalValue: Double = 0.0
)

data class FdResult(
    val investedAmount: Double = 0.0,
    val totalInterest: Double = 0.0,
    val maturityAmount: Double = 0.0
)

data class RdResult(
    val investedAmount: Double = 0.0,
    val totalInterest: Double = 0.0,
    val maturityValue: Double = 0.0
)

data class GstResult(
    val netAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

data class InflationResult(
    val futureCost: Double = 0.0,
    val costIncrease: Double = 0.0
)

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorRepository: CalculatorRepository
) : ViewModel() {

    private val _currentCalculator = MutableStateFlow(CalculatorType.PERCENTAGE)
    val currentCalculator = _currentCalculator.asStateFlow()

    // History filter
    private val _historyFilter = MutableStateFlow("ALL")
    val historyFilter = _historyFilter.asStateFlow()

    // Real-time calculation state
    // 1. Percentage Calculator
    var percentValue = MutableStateFlow(40.0)
    var percentBaseAmount = MutableStateFlow(100.0)

    // 2. Equivalence / Ratio Calculator
    var ratioValA = MutableStateFlow(100.0)
    var ratioValB = MutableStateFlow(450.0)
    var ratioValC = MutableStateFlow(200.0)

    // 3. Profit & Loss Calculator
    var plOriginalAmount = MutableStateFlow(10000.0)
    var plPercentage = MutableStateFlow(40.0)
    var plIsProfit = MutableStateFlow(true) // true = Profit, false = Loss

    // 4. Compound Interest Calculator
    var ciPrincipal = MutableStateFlow(1000.0)
    var ciAnnualRate = MutableStateFlow(20.0)
    var ciTenureYears = MutableStateFlow(10)
    var ciFrequency = MutableStateFlow(1) // 1: Annual, 2: Semi-Annual, 4: Quarterly, 12: Monthly

    // 5. MDR Calculator
    var mdrTransactionAmount = MutableStateFlow(5000.0)
    var mdrRate = MutableStateFlow(0.4) // 0.4%
    var mdrThreshold = MutableStateFlow(2000.0) // ₹2,000 threshold
    var mdrCap = MutableStateFlow(300.0) // ₹300 max cap

    // 6. EMI
    var emiLoanAmount = MutableStateFlow(1000000.0)
    var emiInterestRate = MutableStateFlow(8.5)
    var emiTenureYears = MutableStateFlow(5)

    // 7. SIP
    var sipMonthlyAmount = MutableStateFlow(5000.0)
    var sipExpectedReturnRate = MutableStateFlow(12.0)
    var sipTenureYears = MutableStateFlow(10)

    // 8. Lump Sum
    var lumpsumAmount = MutableStateFlow(100000.0)
    var lumpsumReturnRate = MutableStateFlow(12.0)
    var lumpsumTenureYears = MutableStateFlow(5)

    // 9. FD
    var fdAmount = MutableStateFlow(100000.0)
    var fdInterestRate = MutableStateFlow(7.0)
    var fdTenureYears = MutableStateFlow(3)

    // 10. RD
    var rdMonthlyDeposit = MutableStateFlow(5000.0)
    var rdInterestRate = MutableStateFlow(6.5)
    var rdTenureMonths = MutableStateFlow(24)

    // 11. GST
    var gstAmount = MutableStateFlow(10000.0)
    var gstRate = MutableStateFlow(18.0)
    var isGstInclusive = MutableStateFlow(false)

    // 12. Inflation
    var inflationPresentCost = MutableStateFlow(100000.0)
    var inflationRate = MutableStateFlow(6.0)
    var inflationTenureYears = MutableStateFlow(10)

    // History Flow
    val allHistory: StateFlow<List<CalculatorHistoryEntity>> = calculatorRepository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredHistory = combine(allHistory, _historyFilter) { history, filter ->
        if (filter == "ALL") history else history.filter { it.calculatorType == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCalculatorType(type: CalculatorType) {
        _currentCalculator.value = type
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    // --- Calculation Logics ---

    fun calculatePercentage(): PercentageResult {
        val pct = percentValue.value
        val base = percentBaseAmount.value
        val res = (pct / 100.0) * base
        val added = base + res
        val sub = (base - res).coerceAtLeast(0.0)
        return PercentageResult(
            percentage = pct,
            baseAmount = base,
            resultAmount = res,
            addedTotal = added,
            subtractedTotal = sub
        )
    }

    fun calculateRatio(): RatioResult {
        val a = ratioValA.value
        val b = ratioValB.value
        val c = ratioValC.value
        if (a == 0.0) {
            return RatioResult(a, b, c, 0.0, 0.0, 0.0)
        }
        val d = (b * c) / a
        val unitRate = b / a
        val multiplier = c / a
        return RatioResult(a, b, c, d, unitRate, multiplier)
    }

    fun calculateProfitLoss(): ProfitLossResult {
        val orig = plOriginalAmount.value
        val pct = plPercentage.value
        val isProfit = plIsProfit.value
        val diff = (pct / 100.0) * orig
        val finalAmt = if (isProfit) orig + diff else (orig - diff).coerceAtLeast(0.0)
        val margin = if (finalAmt > 0 && isProfit) (diff / finalAmt) * 100.0 else 0.0
        return ProfitLossResult(
            originalAmount = orig,
            percentage = pct,
            isProfit = isProfit,
            profitOrLossAmount = diff,
            finalAmount = finalAmt,
            marginPercent = margin
        )
    }

    fun calculateCompoundInterest(): CompoundInterestResult {
        val p = ciPrincipal.value
        val r = ciAnnualRate.value / 100.0
        val t = ciTenureYears.value.toDouble()
        val n = ciFrequency.value.toDouble()

        if (p <= 0 || t <= 0 || n <= 0) {
            return CompoundInterestResult(p, ciAnnualRate.value, ciTenureYears.value, ciFrequency.value, p, 0.0, 1.0)
        }

        val finalAmount = p * (1.0 + r / n).pow(n * t)
        val totalInterest = (finalAmount - p).coerceAtLeast(0.0)
        val multiplier = if (p > 0) finalAmount / p else 1.0
        return CompoundInterestResult(
            principal = p,
            annualRate = ciAnnualRate.value,
            tenureYears = ciTenureYears.value,
            frequencyPerYear = ciFrequency.value,
            finalAmount = finalAmount,
            totalInterest = totalInterest,
            growthMultiplier = multiplier
        )
    }

    fun calculateMdr(): MdrResult {
        val amt = mdrTransactionAmount.value
        val rate = mdrRate.value
        val threshold = mdrThreshold.value
        val cap = mdrCap.value

        if (amt <= 0) {
            return MdrResult(amt, rate, threshold, cap, 0.0, 0.0, isThresholdWaived = false, isCapped = false, netPayout = 0.0, effectiveRatePercent = 0.0)
        }

        if (amt <= threshold) {
            return MdrResult(
                transactionAmount = amt,
                mdrRate = rate,
                threshold = threshold,
                capAmount = cap,
                calculatedMdr = 0.0,
                actualMdr = 0.0,
                isThresholdWaived = true,
                isCapped = false,
                netPayout = amt,
                effectiveRatePercent = 0.0
            )
        } else {
            val calculated = (amt * rate) / 100.0
            val isCapped = calculated > cap
            val actual = if (isCapped) cap else calculated
            val net = amt - actual
            val effectiveRate = (actual / amt) * 100.0
            return MdrResult(
                transactionAmount = amt,
                mdrRate = rate,
                threshold = threshold,
                capAmount = cap,
                calculatedMdr = calculated,
                actualMdr = actual,
                isThresholdWaived = false,
                isCapped = isCapped,
                netPayout = net,
                effectiveRatePercent = effectiveRate
            )
        }
    }

    fun calculateEmi(): EmiResult {
        val p = emiLoanAmount.value
        val r = emiInterestRate.value / (12.0 * 100.0)
        val n = emiTenureYears.value * 12

        if (p <= 0 || n <= 0) return EmiResult()

        val emi = if (r > 0) {
            (p * r * (1.0 + r).pow(n.toDouble())) / ((1.0 + r).pow(n.toDouble()) - 1.0)
        } else {
            p / n
        }
        val totalPayment = emi * n
        val totalInterest = (totalPayment - p).coerceAtLeast(0.0)
        val pPct = if (totalPayment > 0) ((p / totalPayment) * 100).toFloat() else 100f
        val iPct = if (totalPayment > 0) ((totalInterest / totalPayment) * 100).toFloat() else 0f
        return EmiResult(
            monthlyEmi = emi,
            totalInterest = totalInterest,
            totalPayment = totalPayment,
            principalPercent = pPct,
            interestPercent = iPct
        )
    }

    fun calculateSip(): SipResult {
        val p = sipMonthlyAmount.value
        val annualRate = sipExpectedReturnRate.value
        val n = sipTenureYears.value * 12

        if (p <= 0 || n <= 0) return SipResult()

        val i = (annualRate / 100.0) / 12.0
        val totalValue = if (i > 0) {
            p * (( (1.0 + i).pow(n.toDouble()) - 1.0) / i) * (1.0 + i)
        } else {
            p * n
        }
        val invested = p * n
        val returns = (totalValue - invested).coerceAtLeast(0.0)
        return SipResult(investedAmount = invested, estimatedReturns = returns, totalValue = totalValue)
    }

    fun calculateLumpsum(): LumpsumResult {
        val p = lumpsumAmount.value
        val r = lumpsumReturnRate.value / 100.0
        val t = lumpsumTenureYears.value

        if (p <= 0 || t <= 0) return LumpsumResult()

        val totalValue = p * (1.0 + r).pow(t.toDouble())
        val returns = (totalValue - p).coerceAtLeast(0.0)
        return LumpsumResult(investedAmount = p, estimatedReturns = returns, totalValue = totalValue)
    }

    fun calculateFd(): FdResult {
        val p = fdAmount.value
        val r = fdInterestRate.value / 100.0
        val t = fdTenureYears.value.toDouble()

        if (p <= 0 || t <= 0) return FdResult()

        // Quarterly compounding for FD (standard in India)
        val n = 4.0
        val maturity = p * (1.0 + r / n).pow(n * t)
        val interest = (maturity - p).coerceAtLeast(0.0)
        return FdResult(investedAmount = p, totalInterest = interest, maturityAmount = maturity)
    }

    fun calculateRd(): RdResult {
        val p = rdMonthlyDeposit.value
        val r = rdInterestRate.value / 100.0
        val n = rdTenureMonths.value

        if (p <= 0 || n <= 0) return RdResult()

        val invested = p * n
        var maturity = 0.0
        for (month in 1..n) {
            val quarters = (n - month + 1) / 3.0
            maturity += p * (1.0 + r / 4.0).pow(quarters)
        }
        val interest = (maturity - invested).coerceAtLeast(0.0)
        return RdResult(investedAmount = invested, totalInterest = interest, maturityValue = maturity)
    }

    fun calculateGst(): GstResult {
        val amt = gstAmount.value
        val rate = gstRate.value

        if (amt <= 0) return GstResult()

        return if (isGstInclusive.value) {
            val net = (amt * 100.0) / (100.0 + rate)
            val gst = amt - net
            GstResult(netAmount = net, gstAmount = gst, totalAmount = amt)
        } else {
            val gst = (amt * rate) / 100.0
            val total = amt + gst
            GstResult(netAmount = amt, gstAmount = gst, totalAmount = total)
        }
    }

    fun calculateInflation(): InflationResult {
        val p = inflationPresentCost.value
        val r = inflationRate.value / 100.0
        val t = inflationTenureYears.value.toDouble()

        if (p <= 0 || t <= 0) return InflationResult()

        val future = p * (1.0 + r).pow(t)
        val increase = (future - p).coerceAtLeast(0.0)
        return InflationResult(futureCost = future, costIncrease = increase)
    }

    // --- History Actions ---

    fun saveCurrentCalculation() {
        viewModelScope.launch {
            when (_currentCalculator.value) {
                CalculatorType.PERCENTAGE -> {
                    val res = calculatePercentage()
                    val json = JSONObject().apply {
                        put("percent", percentValue.value)
                        put("base", percentBaseAmount.value)
                    }.toString()
                    val title = "${formatDecimal(percentValue.value)}% of ₹${formatCurrency(percentBaseAmount.value)}"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.PERCENTAGE.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.resultAmount)}",
                        secondaryResult = "+ Total: ₹${formatCurrency(res.addedTotal)} | - Total: ₹${formatCurrency(res.subtractedTotal)}"
                    )
                }
                CalculatorType.EQUIVALENCE -> {
                    val res = calculateRatio()
                    val json = JSONObject().apply {
                        put("valA", ratioValA.value)
                        put("valB", ratioValB.value)
                        put("valC", ratioValC.value)
                    }.toString()
                    val title = "${formatDecimal(ratioValA.value)} = ${formatDecimal(ratioValB.value)} → ${formatDecimal(ratioValC.value)} = ?"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.EQUIVALENCE.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = formatDecimal(res.valD),
                        secondaryResult = "Unit Rate: 1 = ${formatDecimal(res.unitRate)} | Scale: ${formatDecimal(res.multiplier)}x"
                    )
                }
                CalculatorType.PROFIT_LOSS -> {
                    val res = calculateProfitLoss()
                    val json = JSONObject().apply {
                        put("amount", plOriginalAmount.value)
                        put("percent", plPercentage.value)
                        put("isProfit", plIsProfit.value)
                    }.toString()
                    val modeStr = if (plIsProfit.value) "Profit" else "Loss"
                    val title = "${formatDecimal(plPercentage.value)}% $modeStr on ₹${formatCurrency(plOriginalAmount.value)}"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.PROFIT_LOSS.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "${if (plIsProfit.value) "+" else "-"}₹${formatCurrency(res.profitOrLossAmount)}",
                        secondaryResult = "Final Amount: ₹${formatCurrency(res.finalAmount)} (Original: ₹${formatCurrency(res.originalAmount)})"
                    )
                }
                CalculatorType.COMPOUND_INTEREST -> {
                    val res = calculateCompoundInterest()
                    val json = JSONObject().apply {
                        put("principal", ciPrincipal.value)
                        put("rate", ciAnnualRate.value)
                        put("years", ciTenureYears.value)
                        put("frequency", ciFrequency.value)
                    }.toString()
                    val title = "₹${formatCompact(ciPrincipal.value)} @ ${ciAnnualRate.value}% CI for ${ciTenureYears.value} yrs"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.COMPOUND_INTEREST.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.finalAmount)}",
                        secondaryResult = "Growth Gain: ₹${formatCurrency(res.totalInterest)} (${formatDecimal(res.growthMultiplier)}x)"
                    )
                }
                CalculatorType.MDR -> {
                    val res = calculateMdr()
                    val json = JSONObject().apply {
                        put("amount", mdrTransactionAmount.value)
                        put("rate", mdrRate.value)
                        put("threshold", mdrThreshold.value)
                        put("cap", mdrCap.value)
                    }.toString()
                    val title = "MDR on ₹${formatCurrency(mdrTransactionAmount.value)} @ ${mdrRate.value}% (Max ₹${mdrCap.value.toInt()})"
                    val statusDesc = if (res.isThresholdWaived) "₹0 (Below ₹2,000 threshold)" else if (res.isCapped) "₹${formatDecimal(res.actualMdr)} (Capped at ₹300 max)" else "₹${formatDecimal(res.actualMdr)} (0.4% charged)"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.MDR.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "MDR: ₹${formatDecimal(res.actualMdr)}",
                        secondaryResult = "Net Payout: ₹${formatCurrency(res.netPayout)} | $statusDesc"
                    )
                }
                CalculatorType.EMI -> {
                    val res = calculateEmi()
                    val json = JSONObject().apply {
                        put("amount", emiLoanAmount.value)
                        put("rate", emiInterestRate.value)
                        put("years", emiTenureYears.value)
                    }.toString()
                    val title = "Loan ₹${formatCompact(emiLoanAmount.value)} @ ${emiInterestRate.value}% for ${emiTenureYears.value} yrs"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.EMI.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.monthlyEmi)} / mo",
                        secondaryResult = "Total Interest: ₹${formatCurrency(res.totalInterest)} | Total: ₹${formatCurrency(res.totalPayment)}"
                    )
                }
                CalculatorType.SIP -> {
                    val res = calculateSip()
                    val json = JSONObject().apply {
                        put("amount", sipMonthlyAmount.value)
                        put("rate", sipExpectedReturnRate.value)
                        put("years", sipTenureYears.value)
                    }.toString()
                    val title = "₹${formatCompact(sipMonthlyAmount.value)}/mo for ${sipTenureYears.value} yrs @ ${sipExpectedReturnRate.value}%"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.SIP.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.totalValue)}",
                        secondaryResult = "Invested: ₹${formatCurrency(res.investedAmount)} | Gain: ₹${formatCurrency(res.estimatedReturns)}"
                    )
                }
                CalculatorType.LUMPSUM -> {
                    val res = calculateLumpsum()
                    val json = JSONObject().apply {
                        put("amount", lumpsumAmount.value)
                        put("rate", lumpsumReturnRate.value)
                        put("years", lumpsumTenureYears.value)
                    }.toString()
                    val title = "₹${formatCompact(lumpsumAmount.value)} for ${lumpsumTenureYears.value} yrs @ ${lumpsumReturnRate.value}%"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.LUMPSUM.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.totalValue)}",
                        secondaryResult = "Invested: ₹${formatCurrency(res.investedAmount)} | Gain: ₹${formatCurrency(res.estimatedReturns)}"
                    )
                }
                CalculatorType.FD -> {
                    val res = calculateFd()
                    val json = JSONObject().apply {
                        put("amount", fdAmount.value)
                        put("rate", fdInterestRate.value)
                        put("years", fdTenureYears.value)
                    }.toString()
                    val title = "FD ₹${formatCompact(fdAmount.value)} @ ${fdInterestRate.value}% for ${fdTenureYears.value} yrs"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.FD.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.maturityAmount)}",
                        secondaryResult = "Principal: ₹${formatCurrency(res.investedAmount)} | Interest: ₹${formatCurrency(res.totalInterest)}"
                    )
                }
                CalculatorType.RD -> {
                    val res = calculateRd()
                    val json = JSONObject().apply {
                        put("amount", rdMonthlyDeposit.value)
                        put("rate", rdInterestRate.value)
                        put("months", rdTenureMonths.value)
                    }.toString()
                    val title = "RD ₹${formatCompact(rdMonthlyDeposit.value)}/mo @ ${rdInterestRate.value}% for ${rdTenureMonths.value} mos"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.RD.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "₹${formatCurrency(res.maturityValue)}",
                        secondaryResult = "Invested: ₹${formatCurrency(res.investedAmount)} | Interest: ₹${formatCurrency(res.totalInterest)}"
                    )
                }
                CalculatorType.GST -> {
                    val res = calculateGst()
                    val json = JSONObject().apply {
                        put("amount", gstAmount.value)
                        put("rate", gstRate.value)
                        put("inclusive", isGstInclusive.value)
                    }.toString()
                    val title = "GST ${gstRate.value}% on ₹${formatCurrency(gstAmount.value)} (${if (isGstInclusive.value) "Inclusive" else "Exclusive"})"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.GST.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "Total: ₹${formatCurrency(res.totalAmount)}",
                        secondaryResult = "Net: ₹${formatCurrency(res.netAmount)} | GST: ₹${formatCurrency(res.gstAmount)}"
                    )
                }
                CalculatorType.INFLATION -> {
                    val res = calculateInflation()
                    val json = JSONObject().apply {
                        put("amount", inflationPresentCost.value)
                        put("rate", inflationRate.value)
                        put("years", inflationTenureYears.value)
                    }.toString()
                    val title = "₹${formatCompact(inflationPresentCost.value)} @ ${inflationRate.value}% inflation in ${inflationTenureYears.value} yrs"
                    calculatorRepository.saveCalculation(
                        calculatorType = CalculatorType.INFLATION.name,
                        title = title,
                        inputValuesJson = json,
                        primaryResult = "Future Cost: ₹${formatCurrency(res.futureCost)}",
                        secondaryResult = "Cost Increase: +₹${formatCurrency(res.costIncrease)}"
                    )
                }
            }
        }
    }

    fun reopenCalculation(historyItem: CalculatorHistoryEntity) {
        try {
            val type = CalculatorType.valueOf(historyItem.calculatorType)
            _currentCalculator.value = type
            val json = JSONObject(historyItem.inputValuesJson)

            when (type) {
                CalculatorType.PERCENTAGE -> {
                    percentValue.value = json.optDouble("percent", 40.0)
                    percentBaseAmount.value = json.optDouble("base", 100.0)
                }
                CalculatorType.EQUIVALENCE -> {
                    ratioValA.value = json.optDouble("valA", 100.0)
                    ratioValB.value = json.optDouble("valB", 450.0)
                    ratioValC.value = json.optDouble("valC", 200.0)
                }
                CalculatorType.PROFIT_LOSS -> {
                    plOriginalAmount.value = json.optDouble("amount", 10000.0)
                    plPercentage.value = json.optDouble("percent", 40.0)
                    plIsProfit.value = json.optBoolean("isProfit", true)
                }
                CalculatorType.COMPOUND_INTEREST -> {
                    ciPrincipal.value = json.optDouble("principal", 1000.0)
                    ciAnnualRate.value = json.optDouble("rate", 20.0)
                    ciTenureYears.value = json.optInt("years", 10)
                    ciFrequency.value = json.optInt("frequency", 1)
                }
                CalculatorType.MDR -> {
                    mdrTransactionAmount.value = json.optDouble("amount", 5000.0)
                    mdrRate.value = json.optDouble("rate", 0.4)
                    mdrThreshold.value = json.optDouble("threshold", 2000.0)
                    mdrCap.value = json.optDouble("cap", 300.0)
                }
                CalculatorType.EMI -> {
                    emiLoanAmount.value = json.optDouble("amount", 1000000.0)
                    emiInterestRate.value = json.optDouble("rate", 8.5)
                    emiTenureYears.value = json.optInt("years", 5)
                }
                CalculatorType.SIP -> {
                    sipMonthlyAmount.value = json.optDouble("amount", 5000.0)
                    sipExpectedReturnRate.value = json.optDouble("rate", 12.0)
                    sipTenureYears.value = json.optInt("years", 10)
                }
                CalculatorType.LUMPSUM -> {
                    lumpsumAmount.value = json.optDouble("amount", 100000.0)
                    lumpsumReturnRate.value = json.optDouble("rate", 12.0)
                    lumpsumTenureYears.value = json.optInt("years", 5)
                }
                CalculatorType.FD -> {
                    fdAmount.value = json.optDouble("amount", 100000.0)
                    fdInterestRate.value = json.optDouble("rate", 7.0)
                    fdTenureYears.value = json.optInt("years", 3)
                }
                CalculatorType.RD -> {
                    rdMonthlyDeposit.value = json.optDouble("amount", 5000.0)
                    rdInterestRate.value = json.optDouble("rate", 6.5)
                    rdTenureMonths.value = json.optInt("months", 24)
                }
                CalculatorType.GST -> {
                    gstAmount.value = json.optDouble("amount", 10000.0)
                    gstRate.value = json.optDouble("rate", 18.0)
                    isGstInclusive.value = json.optBoolean("inclusive", false)
                }
                CalculatorType.INFLATION -> {
                    inflationPresentCost.value = json.optDouble("amount", 100000.0)
                    inflationRate.value = json.optDouble("rate", 6.0)
                    inflationTenureYears.value = json.optInt("years", 10)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            calculatorRepository.deleteHistory(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            if (_historyFilter.value == "ALL") {
                calculatorRepository.clearAllHistory()
            } else {
                calculatorRepository.clearHistoryByType(_historyFilter.value)
            }
        }
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
            if (amount % 1.0 == 0.0) {
                formatter.maximumFractionDigits = 0
                formatter.minimumFractionDigits = 0
            } else {
                formatter.maximumFractionDigits = 2
                formatter.minimumFractionDigits = 2
            }
            return formatter.format(amount)
        }

        fun formatDecimal(amount: Double, maxDecimals: Int = 2): String {
            if (amount % 1.0 == 0.0) {
                return amount.toInt().toString()
            }
            val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
            formatter.maximumFractionDigits = maxDecimals
            formatter.minimumFractionDigits = 0
            return formatter.format(amount)
        }

        fun formatCompact(amount: Double): String {
            return when {
                amount >= 10000000.0 -> String.format(Locale.getDefault(), "%.2f Cr", amount / 10000000.0)
                amount >= 100000.0 -> String.format(Locale.getDefault(), "%.2f L", amount / 100000.0)
                amount >= 1000.0 -> String.format(Locale.getDefault(), "%.1f k", amount / 1000.0)
                else -> String.format(Locale.getDefault(), "%.0f", amount)
            }
        }
    }
}
