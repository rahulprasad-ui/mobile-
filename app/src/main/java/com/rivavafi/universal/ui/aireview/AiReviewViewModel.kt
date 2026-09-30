package com.rivavafi.universal.ui.aireview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.data.local.UserCorrectionDao
import com.rivavafi.universal.data.local.UserCorrectionEntity
import com.rivavafi.universal.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val userCorrectionDao: UserCorrectionDao
) : ViewModel() {

    private val _uncertainTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val uncertainTransactions = _uncertainTransactions.asStateFlow()

    private val _learningQuestions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val learningQuestions = _learningQuestions.asStateFlow()

    private val _topCategory = MutableStateFlow("")
    val topCategory = _topCategory.asStateFlow()

    private val _avgDaily = MutableStateFlow(0.0)
    val avgDaily = _avgDaily.asStateFlow()

    private val _frequentMerchant = MutableStateFlow("")
    val frequentMerchant = _frequentMerchant.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            repository.getAllTransactions(userId).collectLatest { allTransactions ->
                // Mock uncertain transactions: "UNKNOWN" or low confidence mappings.
                // In a real app we'd have a 'confidenceScore' field, for now any 'OTHER' or 'UNKNOWN'
                // or just pick some to demonstrate functionality as per constraints.
                _uncertainTransactions.value = allTransactions.filter {
                    it.category == "UNKNOWN" || it.category == "OTHER"
                }.take(3)

                // Pick a few transactions for learning questions that don't have a correction yet
                val allCorrections = userCorrectionDao.getAllCorrections(userId).map { it.merchantName }
                _learningQuestions.value = allTransactions
                    .filter { !allCorrections.contains(it.merchantName) && it.merchantName.isNotBlank() }
                    .distinctBy { it.merchantName }
                    .take(3)

                // Insights Calculation
                val expenses = allTransactions.filter { it.type == "EXPENSE" }
                if (expenses.isNotEmpty()) {
                    val catGroups = expenses.groupBy { it.category }
                    _topCategory.value = catGroups.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: ""

                    val merchGroups = expenses.groupBy { it.merchantName }
                    _frequentMerchant.value = merchGroups.maxByOrNull { it.value.size }?.key ?: ""

                    val firstDate = allTransactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
                    val days = ((System.currentTimeMillis() - firstDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    val totalSpent = expenses.sumOf { it.amount }
                    _avgDaily.value = totalSpent / days
                }
            }
        }
    }

    fun confirmCategory(transaction: TransactionEntity, newCategory: String) {
        viewModelScope.launch {
            repository.addTransaction(transaction.copy(category = newCategory))
            userCorrectionDao.insertCorrection(UserCorrectionEntity(
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch,
                merchantName = transaction.merchantName,
                category = newCategory,
                subcategory = null,
                keyword = null
            ))
        }
    }

    fun answerLearningQuestion(transaction: TransactionEntity, isYes: Boolean, suggestedCategory: String) {
        viewModelScope.launch {
            if (isYes) {
                userCorrectionDao.insertCorrection(UserCorrectionEntity(
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch,
                    merchantName = transaction.merchantName,
                    category = suggestedCategory,
                    subcategory = null,
                    keyword = null
                ))
            } else {
                // If "No", maybe just skip, or map to OTHER to stop asking.
                userCorrectionDao.insertCorrection(UserCorrectionEntity(
                userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch,
                    merchantName = transaction.merchantName,
                    category = "OTHER",
                    subcategory = null,
                    keyword = null
                ))
            }
            // Remove from local list to animate away
            _learningQuestions.value = _learningQuestions.value.filter { it.id != transaction.id }
        }
    }
}
