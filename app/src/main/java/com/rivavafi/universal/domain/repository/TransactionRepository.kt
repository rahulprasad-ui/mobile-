package com.rivavafi.universal.domain.repository

import com.rivavafi.universal.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>
    fun getTransactionById(id: Long, userId: String): Flow<TransactionEntity?>
    suspend fun getTransactionsByMerchant(merchantName: String, userId: String): List<TransactionEntity>
    suspend fun isSmsIdProcessed(smsId: String, userId: String): Boolean
    suspend fun doesTransactionExist(date: Long, amount: Double, merchantName: String, userId: String): Boolean
    suspend fun findDuplicate(userId: String, transactionId: String?, referenceId: String?, date: Long, amount: Double, type: String, merchantName: String, smsSender: String?): TransactionEntity?
    suspend fun getAllTransactionsSync(userId: String): List<TransactionEntity>
    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteAllTransactions(userId: String)
    fun startSync(userId: String)
    fun stopSync()
    suspend fun fetchMoreHistory(userId: String, lastDate: Long)
}
