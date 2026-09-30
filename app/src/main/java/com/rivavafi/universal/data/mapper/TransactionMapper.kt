package com.rivavafi.universal.data.mapper

import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.data.remote.TransactionDto
import java.util.UUID

object TransactionMapper {
    fun toDto(entity: TransactionEntity): TransactionDto {
        return TransactionDto(
            transactionId = entity.documentId.ifEmpty { UUID.randomUUID().toString() },
            userId = entity.userId,
            title = entity.merchantName,
            description = entity.description,
            amount = entity.amount,
            type = entity.type,
            category = entity.category,
            subcategory = entity.subcategory,
            paymentMethod = entity.paymentMethod,
            date = entity.date,
            notes = entity.notes,
            source = entity.source,
            bankName = entity.bankName,
            smsId = entity.smsId,
            rawMessage = entity.rawMessage,
            availableBalance = entity.availableBalance,
            billingCycle = entity.billingCycle,
            lastPaymentDate = entity.lastPaymentDate,
            referenceId = entity.referenceId,
            upiId = entity.upiId,
            accountNumberLast4 = entity.accountNumberLast4,
            originalTransactionId = entity.transactionId,
            smsSender = entity.smsSender,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun toEntity(dto: TransactionDto, localId: Long = 0): TransactionEntity {
        return TransactionEntity(
            id = localId,
            documentId = dto.transactionId,
            userId = dto.userId,
            merchantName = dto.title,
            description = dto.description,
            amount = dto.amount,
            type = dto.type,
            category = dto.category,
            subcategory = dto.subcategory,
            paymentMethod = dto.paymentMethod,
            date = dto.date,
            notes = dto.notes,
            source = dto.source,
            bankName = dto.bankName,
            smsId = dto.smsId,
            rawMessage = dto.rawMessage,
            availableBalance = dto.availableBalance,
            billingCycle = dto.billingCycle,
            lastPaymentDate = dto.lastPaymentDate,
            referenceId = dto.referenceId,
            upiId = dto.upiId,
            accountNumberLast4 = dto.accountNumberLast4,
            transactionId = dto.originalTransactionId,
            smsSender = dto.smsSender,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }
}
