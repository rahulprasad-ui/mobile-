package com.rivavafi.universal.domain.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinGeckoApiService {
    @GET("simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "inr",
        @Query("include_24hr_change") include24hChange: Boolean = true
    ): Map<String, CoinGeckoPriceResponse>
}

data class CoinGeckoPriceResponse(
    @SerializedName("inr")
    val inrPrice: Double?,
    @SerializedName("inr_24h_change")
    val inr24hChange: Double?
)
