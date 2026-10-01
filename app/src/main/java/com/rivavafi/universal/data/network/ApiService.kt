package com.rivavafi.universal.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class RedeemKeyRequest(
    @SerializedName("secretKey") val secretKey: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("userEmail") val userEmail: String? = null
)

data class RedeemKeyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: RedeemKeyData? = null,
    @SerializedName("message") val message: String? = null
)

data class RedeemKeyData(
    @SerializedName("tier") val tier: String? = null,
    @SerializedName("code") val code: String? = null
)

data class CreatePaymentOrderRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("userEmail") val userEmail: String? = null,
    @SerializedName("plan") val plan: String = "portfolio_premium",
    @SerializedName("amountPaise") val amountPaise: Int = 1100
)

data class CreatePaymentOrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: PaymentOrderData? = null,
    @SerializedName("message") val message: String? = null
)

data class PaymentOrderData(
    @SerializedName("orderId") val orderId: String? = null,
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("amountPaise") val amountPaise: Int? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("keyId") val keyId: String? = null,
    @SerializedName("paymentUrl") val paymentUrl: String? = null,
    @SerializedName("plan") val plan: String? = null
)

data class VerifyPaymentRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("orderId") val orderId: String,
    @SerializedName("paymentId") val paymentId: String? = null,
    @SerializedName("signature") val signature: String? = null
)

data class VerifyPaymentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: VerifyPaymentData? = null,
    @SerializedName("message") val message: String? = null
)

data class VerifyPaymentData(
    @SerializedName("isPremium") val isPremium: Boolean? = null,
    @SerializedName("orderId") val orderId: String? = null,
    @SerializedName("paymentId") val paymentId: String? = null,
    @SerializedName("plan") val plan: String? = null
)

data class SyncUserRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("is_premium") val isPremium: Boolean? = null,
    @SerializedName("premium_status") val premiumStatus: String? = null,
    @SerializedName("premium_plan") val premiumPlan: String? = null
)

data class SyncUserResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

data class BookSessionRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("date") val date: Long,
    @SerializedName("time") val time: String
)

data class BookSessionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

data class CancelSubscriptionRequest(
    @SerializedName("uid") val uid: String
)

data class CancelSubscriptionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null
)

interface ApiService {
    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<VerifyOtpResponse>

    @POST("keys/redeem")
    suspend fun redeemSecretKey(@Body request: RedeemKeyRequest): Response<RedeemKeyResponse>

    @POST("payments/create-order")
    suspend fun createPaymentOrder(@Body request: CreatePaymentOrderRequest): Response<CreatePaymentOrderResponse>

    @POST("payments/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequest): Response<VerifyPaymentResponse>

    @POST("users/sync")
    suspend fun syncUser(@Body request: SyncUserRequest): Response<SyncUserResponse>

    @POST("api/v1/elite/book-session")
    suspend fun bookEliteSession(@Body request: BookSessionRequest): Response<BookSessionResponse>

    @POST("api/v1/elite/cancel-subscription")
    suspend fun cancelEliteSubscription(@Body request: CancelSubscriptionRequest): Response<CancelSubscriptionResponse>
}
