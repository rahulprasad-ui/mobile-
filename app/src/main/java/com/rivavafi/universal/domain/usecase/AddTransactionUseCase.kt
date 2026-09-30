package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.addTransaction(transaction.copy(userId = userId))
    }
}
