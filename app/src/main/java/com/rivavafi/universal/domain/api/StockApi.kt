package com.rivavafi.universal.domain.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class AlphaResponse(
    @SerializedName("Global Quote")
    val quote: Quote?
)

data class Quote(
    @SerializedName("01. symbol")
    val symbol: String?,

    @SerializedName("05. price")
    val price: String?,

    @SerializedName("10. change percent")
    val changePercent: String?
)

interface StockApi {
    @GET("query")
    suspend fun getStockPrice(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String = com.rivavafi.universal.BuildConfig.ALPHA_VANTAGE_API_KEY
    ): AlphaResponse
}
