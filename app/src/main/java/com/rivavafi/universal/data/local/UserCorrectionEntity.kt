package com.rivavafi.universal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_corrections")
data class UserCorrectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val merchantName: String,
    val keyword: String? = null,
    val category: String,
    val subcategory: String? = null
)
