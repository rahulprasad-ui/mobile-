package com.rivavafi.universal.sms

import android.content.Context
import android.provider.Telephony
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SmsCatchupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val modeStr = inputData.getString("mode") ?: return Result.failure()
        val mode = runCatching { SmsTrackingMode.valueOf(modeStr) }.getOrDefault(SmsTrackingMode.OFF)

        if (mode == SmsTrackingMode.OFF) return Result.success()

        val lastScanDate = preferencesRepository.lastSmsScanDateFlow.first()
        var latestScanDate = lastScanDate

        var processedCount = 0
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(lastScanDate.toString()),
            "${Telephony.Sms.DATE} ASC LIMIT 200"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                processedCount++
                val smsId = it.getString(idIndex)
                val sender = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val timestamp = it.getLong(dateIndex)

                if (timestamp > latestScanDate) {
                    latestScanDate = timestamp
                }

                val parsed = SmsTransactionParser.parseMessage(sender, body, timestamp, smsId, mode)
                if (parsed != null) {
                    val isProcessed = transactionRepository.isSmsIdProcessed(smsId, userId)
                    if (!isProcessed) {
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
                }
            }
        }

        if (latestScanDate > lastScanDate) {
            preferencesRepository.setLastSmsScanDate(latestScanDate)
        }

        if (processedCount == 200) {
            val request = androidx.work.OneTimeWorkRequestBuilder<SmsCatchupWorker>()
                .setInputData(inputData)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueue(request)
        }

        return Result.success()
    }
}
