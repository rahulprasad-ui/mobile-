package com.rivavafi.universal.domain.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooCryptoApiService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getCryptoChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "2d"
    ): YahooChartResponse
}

data class YahooChartResponse(
    val chart: YahooChartContainer?
)

data class YahooChartContainer(
    val result: List<YahooChartResult>?
)

data class YahooChartResult(
    val meta: YahooChartMeta?
)

data class YahooChartMeta(
    @SerializedName("regularMarketPrice")
    val regularMarketPrice: Double?,
    @SerializedName("previousClose")
    val previousClose: Double?,
    @SerializedName("chartPreviousClose")
    val chartPreviousClose: Double?,
    @SerializedName("regularMarketOpen")
    val regularMarketOpen: Double?,
    @SerializedName("regularMarketDayHigh")
    val regularMarketDayHigh: Double?,
    @SerializedName("regularMarketDayLow")
    val regularMarketDayLow: Double?
)
