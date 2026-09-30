package com.rivavafi.universal.domain.api

import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoApiService {
    @GET("quote")
    suspend fun getCryptoQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): StockResponse
}
