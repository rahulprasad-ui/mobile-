package com.rivavafi.universal.domain.repository

import com.rivavafi.universal.domain.api.ChatApiService
import com.rivavafi.universal.domain.api.ChatRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ChatApiService
) {
    fun sendMessage(message: String): Flow<Result<String>> = flow {
        try {
            Log.d("ChatbotAPI", "Request: $message")
            val response = apiService.sendMessage(ChatRequest(message))
            Log.d("ChatbotAPI", "Response: ${response.reply}")
            emit(Result.success(response.reply))
        } catch (e: Exception) {
            Log.e("ChatbotAPI", "Error: ${e.message}")
            emit(Result.failure(e))
        }
    }
}
