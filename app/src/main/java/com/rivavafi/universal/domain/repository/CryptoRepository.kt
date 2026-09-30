package com.rivavafi.universal.domain.repository

import com.rivavafi.universal.BuildConfig
import com.rivavafi.universal.domain.api.CoinGeckoApiService
import com.rivavafi.universal.domain.api.CryptoApiService
import com.rivavafi.universal.domain.api.StockResponse
import com.rivavafi.universal.domain.api.YahooCryptoApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepository @Inject constructor(
    private val coinGeckoApiService: CoinGeckoApiService,
    private val finnhubApiService: CryptoApiService,
    private val yahooApiService: YahooCryptoApiService
) {
    private val apiKey = BuildConfig.FINNHUB_API_KEY

    fun getCryptoQuote(cryptoId: String): Flow<Result<StockResponse>> = flow {
        val coinGeckoQuote = fetchFromCoinGecko(cryptoId)
        if (coinGeckoQuote != null) {
            emit(Result.success(coinGeckoQuote))
            return@flow
        }

        val symbols = resolveSymbols(cryptoId)

        val finnhubQuote = fetchFromFinnhub(symbols.finnhubSymbol)
        if (finnhubQuote != null) {
            emit(Result.success(finnhubQuote))
            return@flow
        }

        val yahooQuote = fetchFromYahoo(symbols.yahooSymbol)
        if (yahooQuote != null) {
            emit(Result.success(yahooQuote))
            return@flow
        }

        emit(Result.failure(Exception("Unable to load crypto quote from CoinGecko, Finnhub or Yahoo.")))
    }

    private suspend fun fetchFromCoinGecko(cryptoId: String): StockResponse? {
        return try {
            val response = coinGeckoApiService.getSimplePrice(ids = cryptoId.lowercase())
            val quote = response[cryptoId.lowercase()] ?: return null
            val currentPrice = quote.inrPrice ?: return null
            val changePercent = quote.inr24hChange ?: 0.0
            val previousClose = if (changePercent == 0.0) {
                currentPrice
            } else {
                currentPrice / (1 + (changePercent / 100))
            }

            StockResponse(
                c = currentPrice,
                h = currentPrice,
                l = currentPrice,
                o = previousClose,
                pc = previousClose
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchFromFinnhub(symbol: String): StockResponse? {
        if (apiKey.isBlank()) return null

        return try {
            val response = finnhubApiService.getCryptoQuote(symbol, apiKey)
            if (response.c > 0) response else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchFromYahoo(symbol: String): StockResponse? {
        return try {
            val response = yahooApiService.getCryptoChart(symbol = symbol)
            val result = response.chart?.result?.firstOrNull() ?: return null
            val meta = result.meta ?: return null

            val price = meta.regularMarketPrice ?: meta.previousClose ?: return null
            val previousClose = meta.previousClose ?: meta.chartPreviousClose ?: price
            val open = meta.regularMarketOpen ?: previousClose
            val dayHigh = meta.regularMarketDayHigh ?: price
            val dayLow = meta.regularMarketDayLow ?: price

            StockResponse(
                c = price,
                h = dayHigh,
                l = dayLow,
                o = open,
                pc = previousClose
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSymbols(cryptoId: String): CryptoSymbols {
        return when (cryptoId.lowercase()) {
            "bitcoin" -> CryptoSymbols("BINANCE:BTCUSDT", "BTC-USD")
            "ethereum" -> CryptoSymbols("BINANCE:ETHUSDT", "ETH-USD")
            "solana" -> CryptoSymbols("BINANCE:SOLUSDT", "SOL-USD")
            else -> CryptoSymbols("BINANCE:BTCUSDT", "BTC-USD")
        }
    }

    private data class CryptoSymbols(
        val finnhubSymbol: String,
        val yahooSymbol: String
    )
}
