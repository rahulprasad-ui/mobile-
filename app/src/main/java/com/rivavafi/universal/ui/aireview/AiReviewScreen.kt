package com.rivavafi.universal.ui.aireview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.ui.theme.bounceClick
import com.rivavafi.universal.ui.theme.CategoryVisuals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(viewModel: AiReviewViewModel = hiltViewModel()) {
    val uncertainTransactions by viewModel.uncertainTransactions.collectAsState()
    val learningQuestions by viewModel.learningQuestions.collectAsState()
    val topCategory by viewModel.topCategory.collectAsState()
    val avgDaily by viewModel.avgDaily.collectAsState()
    val frequentMerchant by viewModel.frequentMerchant.collectAsState()

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 60.dp)
    ) {
        Text(
            text = "AI Review",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Uncertain Transactions
            if (uncertainTransactions.isNotEmpty()) {
                item {
                    SectionHeader("Needs Classification")
                }
                items(uncertainTransactions) { transaction ->
                    UncertainTransactionCard(transaction = transaction) { newCat ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.confirmCategory(transaction, newCat)
                    }
                }
            }

            // 2. Learning Questions
            if (learningQuestions.isNotEmpty()) {
                item {
                    SectionHeader("Help the AI Learn")
                }
                items(learningQuestions) { transaction ->
                    val suggested = if (transaction.amount > 1000) "BILL" else "FOOD" // Simple dummy suggestion
                    LearningQuestionCard(
                        transaction = transaction,
                        suggestedCategory = suggested,
                        onAnswer = { isYes ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.answerLearningQuestion(transaction, isYes, suggested)
                        }
                    )
                }
            }

            // 3. Insights (Always show if nothing else, or at the bottom)
            item {
                SectionHeader("AI Insights")
            }
            item {
                InsightsCard(topCategory, avgDaily, frequentMerchant)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun UncertainTransactionCard(transaction: TransactionEntity, onCategorySelected: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "What is ${transaction.merchantName}?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCategorySelected("FOOD") }, modifier = Modifier.weight(1f)) {
                    Text("Food")
                }
                Button(onClick = { onCategorySelected("SHOPPING") }, modifier = Modifier.weight(1f)) {
                    Text("Shopping")
                }
            }
        }
    }
}

@Composable
fun LearningQuestionCard(transaction: TransactionEntity, suggestedCategory: String, onAnswer: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Is ${transaction.merchantName} a $suggestedCategory debit?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAnswer(true) },
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick { onAnswer(true) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Yes")
                }
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick { onAnswer(false) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("No")
                }
            }
        }
    }
}

@Composable
fun InsightsCard(topCategory: String, avgDaily: Double, frequentMerchant: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            InsightRow("Top Spending Category", topCategory.ifBlank { "Not enough data" })
            Spacer(modifier = Modifier.height(16.dp))
            InsightRow("Average Daily Spend", if (avgDaily > 0) "₹${String.format(java.util.Locale.getDefault(), "%.0f", avgDaily)}" else "Not enough data")
            Spacer(modifier = Modifier.height(16.dp))
            InsightRow("Most Frequent Merchant", frequentMerchant.ifBlank { "Not enough data" })
        }
    }
}

@Composable
fun InsightRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}
