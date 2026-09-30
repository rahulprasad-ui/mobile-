package com.rivavafi.universal.data.repository

import com.rivavafi.universal.data.local.CategoryDao
import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(userId: String): Flow<List<CategoryEntity>> {
        return dao.getAllCategories(userId)
    }

    override suspend fun addCategory(category: CategoryEntity) {
        dao.insertCategory(category)
    }

    override suspend fun initializeDefaultCategories(userId: String) {
        if (dao.getCount(userId) == 0) {
            val defaults = listOf(
                CategoryEntity(name = "Food & Dining", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Shopping", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Transport", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Bills & Utilities", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Entertainment", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Health", type = "DEBIT", userId = userId),
                CategoryEntity(name = "Salary", type = "CREDIT", userId = userId),
                CategoryEntity(name = "Refund", type = "CREDIT", userId = userId),
                CategoryEntity(name = "General", type = "DEBIT", userId = userId)
            )
            defaults.forEach { dao.insertCategory(it) }
        }
    }
}
