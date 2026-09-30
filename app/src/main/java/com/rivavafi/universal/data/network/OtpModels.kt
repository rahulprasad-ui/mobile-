package com.rivavafi.universal.data.network

import com.google.gson.annotations.SerializedName

data class OtpRequest(
    @SerializedName("phone") val phone: String
)

data class OtpResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

data class VerifyOtpRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("otp") val otp: String
)

data class VerifyOtpResponse(
    @SerializedName("token") val token: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)
