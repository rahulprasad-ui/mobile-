package com.rivavafi.universal.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId LIMIT 1")
    fun getTransactionById(id: Long, userId: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllTransactionsSync(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE merchantName = :merchantName AND userId = :userId ORDER BY date DESC")
    suspend fun getTransactionsByMerchant(merchantName: String, userId: String): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE smsId = :smsId AND userId = :userId")
    suspend fun isSmsIdProcessed(smsId: String, userId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND amount = :amount AND merchantName = :merchantName AND userId = :userId")
    suspend fun doesTransactionExist(date: Long, amount: Double, merchantName: String, userId: String): Int

    @Query("SELECT * FROM transactions WHERE userId = :userId AND ((transactionId IS NOT NULL AND transactionId = :transactionId) OR (referenceId IS NOT NULL AND referenceId = :referenceId) OR (date BETWEEN :date - 60000 AND :date + 60000 AND amount = :amount AND type = :type AND merchantName = :merchantName AND smsSender = :smsSender)) LIMIT 1")
    suspend fun findDuplicate(userId: String, transactionId: String?, referenceId: String?, date: Long, amount: Double, type: String, merchantName: String, smsSender: String?): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactions(userId: String)
    @Query("SELECT * FROM transactions WHERE documentId = :documentId AND userId = :userId LIMIT 1")
    suspend fun getTransactionByDocumentId(documentId: String, userId: String): TransactionEntity?
}
