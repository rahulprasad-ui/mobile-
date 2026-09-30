package com.rivavafi.universal.data.remote

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class TransactionDto(
    val transactionId: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val subcategory: String? = null,
    val paymentMethod: String? = null,
    val date: Long = 0L,
    val notes: String? = null,
    val source: String = "MANUAL",
    val bankName: String? = null,
    val smsId: String? = null,
    val rawMessage: String? = null,
    val availableBalance: Double? = null,
    val billingCycle: String? = null,
    val lastPaymentDate: Long? = null,
    val referenceId: String? = null,
    val upiId: String? = null,
    val accountNumberLast4: String? = null,
    val originalTransactionId: String? = null,
    val smsSender: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @ServerTimestamp
    val serverUpdatedAt: Date? = null
)
