package com.rivavafi.universal.domain.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class StockResponse(
    val c: Double,
    val h: Double,
    val l: Double,
    val o: Double,
    val pc: Double
)

data class FinnhubCompanyProfileResponse(
    val name: String?,
    val ticker: String?,
    val logo: String?,
    val finnhubIndustry: String?,
    val marketCapitalization: Double?
)

data class FinnhubNewsResponse(
    val id: Long,
    val headline: String,
    val summary: String,
    val url: String,
    val image: String,
    val source: String,
    val datetime: Long
)

interface StockApiService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): Response<StockResponse>

    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubCompanyProfileResponse

    @GET("news")
    suspend fun getMarketNews(
        @Query("category") category: String = "general",
        @Query("token") apiKey: String
    ): List<FinnhubNewsResponse>

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") fromDate: String,
        @Query("to") toDate: String,
        @Query("token") apiKey: String
    ): List<FinnhubNewsResponse>
}
