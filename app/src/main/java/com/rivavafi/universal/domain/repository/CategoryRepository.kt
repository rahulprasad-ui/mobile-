package com.rivavafi.universal.domain.repository

import com.rivavafi.universal.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>
    suspend fun addCategory(category: CategoryEntity)
    suspend fun initializeDefaultCategories(userId: String)
}
