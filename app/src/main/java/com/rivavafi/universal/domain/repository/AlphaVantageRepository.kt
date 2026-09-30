package com.rivavafi.universal.domain.repository

import com.rivavafi.universal.domain.api.StockApi
import com.rivavafi.universal.domain.api.Quote
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AlphaVantageRepository {

    private val api = Retrofit.Builder()
        .baseUrl("https://www.alphavantage.co/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(StockApi::class.java)

    private val cache = mutableMapOf<String, Quote>()

    suspend fun getStock(symbol: String): Quote? {
        return try {
            val response = api.getStockPrice(symbol = symbol)
            val data = response.quote

            if (data != null && data.price != null) {
                cache[symbol] = data
                data
            } else {
                cache[symbol]
            }

        } catch (e: Exception) {
            cache[symbol]
        }
    }
}
