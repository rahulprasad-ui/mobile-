package com.rivavafi.universal.domain.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

data class MassiveResponse(
    @SerializedName("data") val data: MassiveData?
)

data class MassiveData(
    @SerializedName("price") val price: Double?,
    @SerializedName("open") val open: Double?,
    @SerializedName("high") val high: Double?,
    @SerializedName("low") val low: Double?,
    @SerializedName("previousClose") val previousClose: Double?
)

interface MassiveApi {
    @GET("v1/stock/price")
    suspend fun getStockPrice(
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String = com.rivavafi.universal.BuildConfig.MASSIVE_API_KEY
    ): MassiveResponse
}
