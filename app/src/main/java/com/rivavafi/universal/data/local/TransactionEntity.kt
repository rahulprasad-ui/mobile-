package com.rivavafi.universal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val bankName: String? = null,
    val merchantName: String,
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT"
    val category: String,
    val date: Long,
    val smsId: String? = null,
    val rawMessage: String? = null,
    val availableBalance: Double? = null,

    // Additional fields for new tracking features (Subscriptions, Vouchers, etc)
    val billingCycle: String? = null,
    val lastPaymentDate: Long? = null,
    val referenceId: String? = null,

    // Subcategory for granular tracking
    val subcategory: String? = null,
    val upiId: String? = null,
    val accountNumberLast4: String? = null,
    val transactionId: String? = null,
    val smsSender: String? = null,
    val source: String = "MANUAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val documentId: String = "",
    val description: String? = null,
    val notes: String? = null,
    val paymentMethod: String? = null
)
