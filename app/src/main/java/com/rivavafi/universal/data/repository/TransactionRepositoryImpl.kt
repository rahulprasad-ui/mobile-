package com.rivavafi.universal.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import com.rivavafi.universal.data.local.TransactionDao
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.data.mapper.TransactionMapper
import com.rivavafi.universal.data.remote.TransactionDto
import com.rivavafi.universal.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TransactionRepository {

    private var syncListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getAllTransactions(userId: String): Flow<List<TransactionEntity>> {
        return dao.getAllTransactions(userId)
    }

    override fun getTransactionById(id: Long, userId: String): Flow<TransactionEntity?> {
        return dao.getTransactionById(id, userId)
    }

    override suspend fun getTransactionsByMerchant(merchantName: String, userId: String): List<TransactionEntity> {
        return dao.getTransactionsByMerchant(merchantName, userId)
    }

    override suspend fun isSmsIdProcessed(smsId: String, userId: String): Boolean {
        return dao.isSmsIdProcessed(smsId, userId) > 0
    }

    override suspend fun doesTransactionExist(date: Long, amount: Double, merchantName: String, userId: String): Boolean {
        return dao.doesTransactionExist(date, amount, merchantName, userId) > 0
    }

    override suspend fun findDuplicate(userId: String, transactionId: String?, referenceId: String?, date: Long, amount: Double, type: String, merchantName: String, smsSender: String?): TransactionEntity? {
        return dao.findDuplicate(userId, transactionId, referenceId, date, amount, type, merchantName, smsSender)
    }

    override suspend fun getAllTransactionsSync(userId: String): List<TransactionEntity> {
        return dao.getAllTransactionsSync(userId)
    }

    override suspend fun addTransaction(transaction: TransactionEntity) {
        val documentId = if (transaction.documentId.isEmpty()) UUID.randomUUID().toString() else transaction.documentId
        val updatedTransaction = transaction.copy(documentId = documentId, updatedAt = System.currentTimeMillis())

        dao.insertTransaction(updatedTransaction)

        val user = auth.currentUser
        if (user != null && updatedTransaction.userId == user.uid) {
            val dto = TransactionMapper.toDto(updatedTransaction)
            scope.launch {
                try {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("transactions")
                        .document(dto.transactionId)
                        .set(dto)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)

        val user = auth.currentUser
        if (user != null && transaction.documentId.isNotEmpty()) {
            scope.launch {
                try {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("transactions")
                        .document(transaction.documentId)
                        .delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override suspend fun deleteAllTransactions(userId: String) {
        dao.deleteAllTransactions(userId)
    }

    override fun startSync(userId: String) {
        if (syncListener != null) return

        // Backfill local un-synced transactions
        scope.launch {
            val localTxns = dao.getAllTransactionsSync(userId)
            val unSynced = localTxns.filter { it.documentId.isEmpty() }
            for (txn in unSynced) {
                val newDocId = UUID.randomUUID().toString()
                val updated = txn.copy(documentId = newDocId, updatedAt = System.currentTimeMillis())
                dao.insertTransaction(updated)

                val dto = TransactionMapper.toDto(updated)
                try {
                    firestore.collection("users")
                        .document(userId)
                        .collection("transactions")
                        .document(newDocId)
                        .set(dto)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        syncListener = firestore.collection("users")
            .document(userId)
            .collection("transactions")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch {
                        for (change in snapshot.documentChanges) {
                            val dto = change.document.toObject(TransactionDto::class.java)

                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val existing = dao.getTransactionByDocumentId(dto.transactionId, userId)
                                    val localId = existing?.id ?: 0L
                                    val entity = TransactionMapper.toEntity(dto, localId)
                                    dao.insertTransaction(entity)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val toDelete = dao.getTransactionByDocumentId(dto.transactionId, userId)
                                    if(toDelete != null) {
                                        dao.deleteTransaction(toDelete)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    override fun stopSync() {
        syncListener?.remove()
        syncListener = null
    }

    override suspend fun fetchMoreHistory(userId: String, lastDate: Long) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .startAfter(lastDate)
                .limit(50)
                .get()
                .await()

            for (doc in snapshot.documents) {
                val dto = doc.toObject(TransactionDto::class.java) ?: continue
                val existing = dao.getTransactionByDocumentId(dto.transactionId, userId)
                if (existing == null) {
                    val entity = TransactionMapper.toEntity(dto, 0L)
                    dao.insertTransaction(entity)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
