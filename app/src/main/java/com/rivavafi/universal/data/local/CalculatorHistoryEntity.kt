package com.rivavafi.universal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculator_history")
data class CalculatorHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val calculatorType: String, // SIP, EMI, LUMPSUM, FD, RD, GST, INFLATION
    val title: String,
    val inputValuesJson: String, // JSON map of input parameters e.g. {"amount": 5000, "rate": 12.0, "years": 10}
    val primaryResult: String, // Main calculated figure e.g. "₹11.62 Lakhs" or "₹14,347 / mo"
    val secondaryResult: String = "", // Detailed breakdown e.g. "Invested: ₹6.00 L | Returns: ₹5.62 L"
    val timestamp: Long = System.currentTimeMillis()
)
