package com.rivavafi.universal.domain.usecase

import android.content.Context
import android.net.Uri
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ImportCsvUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            var importedCount = 0
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Read header
                    val headerLine = reader.readLine() ?: return@withContext Result.failure(Exception("Empty file"))
                    if (!headerLine.contains("date") || !headerLine.contains("amount")) {
                        return@withContext Result.failure(Exception("Invalid CSV format"))
                    }

                    var line = reader.readLine()
                    while (line != null) {
                        // Very naive CSV parsing for quotes and commas
                        // For a robust implementation, a real CSV parser (e.g. OpenCSV) is recommended
                        // But for offline manual app, regex split by comma ignoring commas inside quotes
                        val tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim('\"') }

                        if (tokens.size >= 8) {
                            val dateStr = tokens[0]
                            val merchant = tokens[1]
                            val amountStr = tokens[2]
                            val type = tokens[3]
                            val category = tokens[4]
                            val subcategory = tokens[5].takeIf { it.isNotBlank() }
                            val bank = tokens[6].takeIf { it.isNotBlank() }
                            val message = tokens[7].takeIf { it.isNotBlank() }

                            try {
                                val date = dateFormatter.parse(dateStr)?.time ?: System.currentTimeMillis()
                                val amount = amountStr.toDouble()

                                // Prevent duplicates
                                if (!repository.doesTransactionExist(date, amount, merchant, userId)) {
                                    val transaction = TransactionEntity(
                            userId = userId,
                                        merchantName = merchant,
                                        amount = amount,
                                        type = type,
                                        category = category,
                                        subcategory = subcategory,
                                        date = date,
                                        bankName = bank,
                                        rawMessage = message
                                    )
                                    repository.addTransaction(transaction)
                                    importedCount++
                                }
                            } catch (e: Exception) {
                                // Skip invalid lines
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
