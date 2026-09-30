package com.rivavafi.universal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserCorrectionDao {
    @Query("SELECT * FROM user_corrections WHERE merchantName = :merchantName AND userId = :userId COLLATE NOCASE")
    suspend fun getCorrectionsForMerchant(merchantName: String, userId: String): List<UserCorrectionEntity>

    @Query("SELECT * FROM user_corrections WHERE userId = :userId")
    suspend fun getAllCorrections(userId: String): List<UserCorrectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: UserCorrectionEntity)

    @Query("DELETE FROM user_corrections WHERE userId = :userId")
    suspend fun clearAllCorrections(userId: String)
}
