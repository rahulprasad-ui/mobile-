package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class FinancialSummaryState(
    val totalCredit: Double = 0.0,
    val totalDebit: Double = 0.0,
    val netSavings: Double = 0.0
)

class GetFinancialSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<FinancialSummaryState> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(FinancialSummaryState())
        return repository.getAllTransactions(userId).map { transactions ->
            val income = transactions.filter { it.type == "CREDIT" || it.type == "INCOME" || it.type == "REWARD" }.sumOf { it.amount }
            val expense = transactions.filter { it.type == "DEBIT" || it.type == "EXPENSE" || it.type == "BILL_PENDING" }.sumOf { it.amount }
            FinancialSummaryState(
                totalCredit = income,
                totalDebit = expense,
                netSavings = income - expense
            )
        }
    }
}
