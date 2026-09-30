package com.rivavafi.universal.domain.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class SendVerificationRequest(
    val email: String,
    val uid: String
)

data class VerificationStatusResponse(
    val isVerified: Boolean
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val newPassword: String
)

interface AuthApiService {
    @POST("sendVerificationEmail")
    suspend fun sendVerificationEmail(@Body request: SendVerificationRequest): Response<Unit>

    @GET("checkVerification")
    suspend fun checkVerification(@Query("uid") uid: String): Response<VerificationStatusResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>
}
