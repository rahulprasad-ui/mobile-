package com.rivavafi.universal.sms

data class ParsedSmsTransaction(
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT"
    val merchantName: String,
    val bankName: String?,
    val category: String,
    val date: Long,
    val smsId: String?,
    val rawMessage: String?,
    val availableBalance: Double?,
    val referenceId: String?,
    val subcategory: String? = null,
    val upiId: String? = null,
    val accountNumberLast4: String? = null,
    val transactionId: String? = null,
    val smsSender: String? = null,
    val source: String = "SMS"
)
