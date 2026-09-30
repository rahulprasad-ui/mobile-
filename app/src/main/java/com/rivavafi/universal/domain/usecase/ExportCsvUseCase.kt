package com.rivavafi.universal.domain.usecase

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.rivavafi.universal.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ExportCsvUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(Exception("Not logged in"))
            val transactions = repository.getAllTransactionsSync(userId)
            if (transactions.isEmpty()) {
                return@withContext Result.failure(Exception("No transactions to export."))
            }

            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out ->
                // Write header
                out.write("date,merchant,amount,type,category,subcategory,bank,message\n")

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

                for (t in transactions) {
                    val dateStr = dateFormatter.format(Date(t.date))
                    val merchant = t.merchantName.replace("\"", "\"\"")
                    val amount = t.amount.toString()
                    val type = t.type
                    val category = t.category.replace("\"", "\"\"")
                    val subcategory = (t.subcategory ?: "").replace("\"", "\"\"")
                    val bank = (t.bankName ?: "").replace("\"", "\"\"")
                    val message = (t.rawMessage ?: "").replace("\"", "\"\"").replace("\n", " ")

                    out.write("\"$dateStr\",\"$merchant\",\"$amount\",\"$type\",\"$category\",\"$subcategory\",\"$bank\",\"$message\"\n")
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream for URI."))

            Result.success(uri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
