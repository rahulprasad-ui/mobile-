package com.rivavafi.universal.domain.api

import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(val message: String)
data class ChatResponse(val reply: String)

interface ChatApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}
