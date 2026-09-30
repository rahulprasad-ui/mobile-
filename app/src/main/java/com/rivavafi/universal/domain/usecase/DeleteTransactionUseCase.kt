package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity) {
        repository.deleteTransaction(transaction)
    }
}
