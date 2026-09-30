package com.rivavafi.universal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        UserCorrectionEntity::class,
        CalculatorHistoryEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class RivavaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userCorrectionDao(): UserCorrectionDao
    abstract fun calculatorHistoryDao(): CalculatorHistoryDao
}
