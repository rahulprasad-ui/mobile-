package com.rivavafi.universal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculatorHistoryDao {

    @Query("SELECT * FROM calculator_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistory(userId: String): Flow<List<CalculatorHistoryEntity>>

    @Query("SELECT * FROM calculator_history WHERE userId = :userId AND calculatorType = :type ORDER BY timestamp DESC")
    fun getHistoryByType(userId: String, type: String): Flow<List<CalculatorHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculatorHistoryEntity): Long

    @Query("DELETE FROM calculator_history WHERE id = :id AND userId = :userId")
    suspend fun deleteHistoryById(id: Long, userId: String)

    @Query("DELETE FROM calculator_history WHERE userId = :userId")
    suspend fun clearAllHistory(userId: String)

    @Query("DELETE FROM calculator_history WHERE userId = :userId AND calculatorType = :type")
    suspend fun clearHistoryByType(userId: String, type: String)
}
