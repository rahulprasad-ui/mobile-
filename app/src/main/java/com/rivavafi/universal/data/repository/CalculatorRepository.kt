package com.rivavafi.universal.data.repository

import com.rivavafi.universal.data.local.CalculatorHistoryDao
import com.rivavafi.universal.data.local.CalculatorHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatorRepository @Inject constructor(
    private val calculatorHistoryDao: CalculatorHistoryDao
) {
    private fun getUserId(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "local_user"
    }

    fun getAllHistory(): Flow<List<CalculatorHistoryEntity>> {
        val uid = getUserId()
        return calculatorHistoryDao.getAllHistory(uid)
    }

    fun getHistoryByType(type: String): Flow<List<CalculatorHistoryEntity>> {
        val uid = getUserId()
        return calculatorHistoryDao.getHistoryByType(uid, type)
    }

    suspend fun saveCalculation(
        calculatorType: String,
        title: String,
        inputValuesJson: String,
        primaryResult: String,
        secondaryResult: String = ""
    ): Long {
        val item = CalculatorHistoryEntity(
            userId = getUserId(),
            calculatorType = calculatorType,
            title = title,
            inputValuesJson = inputValuesJson,
            primaryResult = primaryResult,
            secondaryResult = secondaryResult,
            timestamp = System.currentTimeMillis()
        )
        return calculatorHistoryDao.insertHistory(item)
    }

    suspend fun deleteHistory(id: Long) {
        calculatorHistoryDao.deleteHistoryById(id, getUserId())
    }

    suspend fun clearAllHistory() {
        calculatorHistoryDao.clearAllHistory(getUserId())
    }

    suspend fun clearHistoryByType(type: String) {
        calculatorHistoryDao.clearHistoryByType(getUserId(), type)
    }
}
