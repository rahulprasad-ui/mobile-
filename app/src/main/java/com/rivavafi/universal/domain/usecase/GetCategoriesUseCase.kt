package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<CategoryEntity>> {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return repository.getAllCategories(userId)
    }

    suspend fun initialize() {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.initializeDefaultCategories(userId)
    }
}
