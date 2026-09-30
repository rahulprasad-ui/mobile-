package com.rivavafi.universal.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SmsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.success()
        val body = inputData.getString("body") ?: return Result.success()
        val timestamp = inputData.getLong("timestamp", 0L)
        val smsId = inputData.getString("smsId") ?: return Result.success()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        val modeString = preferencesRepository.smsTrackingModeFlow.first()
        val mode = runCatching { SmsTrackingMode.valueOf(modeString) }.getOrDefault(SmsTrackingMode.OFF)
        if (mode == SmsTrackingMode.OFF) return Result.success()

        val parsed = SmsTransactionParser.parseMessage(sender, body, timestamp, smsId, mode)
        if (parsed != null) {
            val duplicate = transactionRepository.findDuplicate(
                userId = userId,
                transactionId = parsed.transactionId,
                referenceId = parsed.referenceId,
                date = parsed.date,
                amount = parsed.amount,
                type = parsed.type,
                merchantName = parsed.merchantName,
                smsSender = parsed.smsSender
            )
            if (duplicate == null) {
                val entity = TransactionEntity(
                    userId = userId,
                    bankName = parsed.bankName,
                    merchantName = parsed.merchantName,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = parsed.category,
                    date = parsed.date,
                    smsId = parsed.smsId,
                    rawMessage = parsed.rawMessage,
                    availableBalance = parsed.availableBalance,
                    referenceId = parsed.referenceId,
                    subcategory = parsed.subcategory,
                    upiId = parsed.upiId,
                    accountNumberLast4 = parsed.accountNumberLast4,
                    transactionId = parsed.transactionId,
                    smsSender = parsed.smsSender,
                    source = parsed.source
                )
                transactionRepository.addTransaction(entity)
            }
        }

        return Result.success()
    }
}
