package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<TransactionEntity>> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return repository.getAllTransactions(userId)
    }
}
