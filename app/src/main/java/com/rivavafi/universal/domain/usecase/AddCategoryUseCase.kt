package com.rivavafi.universal.domain.usecase

import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: CategoryEntity) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        repository.addCategory(category.copy(userId = userId))
    }
}
