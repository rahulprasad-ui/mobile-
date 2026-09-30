package com.rivavafi.universal.ui.portfolio

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.domain.api.FinnhubCompanyProfileResponse
import com.rivavafi.universal.domain.api.FinnhubNewsResponse
import com.rivavafi.universal.domain.api.StockResponse
import com.rivavafi.universal.domain.repository.QuoteSource
import com.rivavafi.universal.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockState(
    val isLoading: Boolean = false,
    val data: StockResponse,
    val source: QuoteSource = QuoteSource.DEFAULT,
    val error: String? = null,
    val diagnostics: String? = null
)

data class CompanyProfileState(
    val isLoading: Boolean = false,
    val profile: FinnhubCompanyProfileResponse? = null
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    private val defaultSymbols = listOf("IREDA.NS", "INDHOTEL.NS", "RTX", "NVDA")

    private val _stockStates = MutableStateFlow(defaultStockStateMap(defaultSymbols))
    val stockStates: StateFlow<Map<String, StockState>> = _stockStates

    private val _companyProfiles = MutableStateFlow<Map<String, CompanyProfileState>>(emptyMap())
    val companyProfiles: StateFlow<Map<String, CompanyProfileState>> = _companyProfiles

    private val _marketNews = MutableStateFlow<List<FinnhubNewsResponse>>(emptyList())
    val marketNews: StateFlow<List<FinnhubNewsResponse>> = _marketNews

    private val _companyNews = MutableStateFlow<Map<String, List<FinnhubNewsResponse>>>(emptyMap())
    val companyNews: StateFlow<Map<String, List<FinnhubNewsResponse>>> = _companyNews

    private var currentSymbols: List<String> = defaultSymbols
    private var pollingJob: Job? = null

    init {
        startPolling(defaultSymbols)
    }

    fun startPolling(symbols: List<String>) {
        val normalizedSymbols = symbols.map { normalizeSymbol(it) }.distinct()
        currentSymbols = normalizedSymbols

        // Ensure we always have a non-null state for every symbol that is being observed.
        _stockStates.value = _stockStates.value.toMutableMap().apply {
            normalizedSymbols.forEach { symbol ->
                putIfAbsent(symbol, defaultStockState(symbol))
            }
        }

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            fetchMarketNews()
            normalizedSymbols.forEach { symbol ->
                fetchCompanyProfile(symbol)
                fetchCompanyNews(symbol)
            }

            while (true) {
                fetchQuotes(normalizedSymbols)
                // Refresh every 10-15 seconds as per requirements
                delay(12_000)
            }
        }
    }

    fun refresh() {
        if (currentSymbols.isNotEmpty()) {
            viewModelScope.launch {
                fetchMarketNews()
                currentSymbols.forEach { symbol ->
                    fetchCompanyProfile(symbol)
                    fetchCompanyNews(symbol)
                }
                fetchQuotes(currentSymbols)
            }
        }
    }

    private suspend fun fetchQuotes(symbols: List<String>) {
        symbols.forEach { symbol ->
            _stockStates.value = _stockStates.value.toMutableMap().apply {
                put(symbol, get(symbol)?.copy(isLoading = true) ?: defaultStockState(symbol).copy(isLoading = true))
            }

            val resolved = repository.getGuaranteedQuote(symbol)
            Log.d(TAG, "Stock state update for $symbol using ${resolved.source}")
            val errorString = resolved.userSafeReason
            _stockStates.value = _stockStates.value.toMutableMap().apply {
                put(symbol, StockState(isLoading = false, data = resolved.quote, source = resolved.source, error = errorString, diagnostics = resolved.diagnostics))
            }
        }
    }

    private fun fetchCompanyProfile(symbol: String) {
        viewModelScope.launch {
            _companyProfiles.value = _companyProfiles.value.toMutableMap().apply {
                put(symbol, CompanyProfileState(isLoading = true))
            }
            repository.getCompanyProfile(symbol).collect { result ->
                result.onSuccess { profile ->
                    _companyProfiles.value = _companyProfiles.value.toMutableMap().apply {
                        put(symbol, CompanyProfileState(isLoading = false, profile = profile))
                    }
                }.onFailure {
                    _companyProfiles.value = _companyProfiles.value.toMutableMap().apply {
                        put(symbol, CompanyProfileState(isLoading = false, profile = null))
                    }
                }
            }
        }
    }

    private fun fetchMarketNews() {
        viewModelScope.launch {
            repository.getMarketNews().collect { result ->
                result.onSuccess { news ->
                    if (news.isNotEmpty()) {
                        _marketNews.value = news
                    } else {
                        _marketNews.value = generateMockNews()
                    }
                }.onFailure {
                    _marketNews.value = generateMockNews()
                }
            }
        }
    }

    private fun fetchCompanyNews(symbol: String) {
        viewModelScope.launch {
            repository.getCompanyNews(symbol).collect { result ->
                result.onSuccess { news ->
                    _companyNews.value = _companyNews.value.toMutableMap().apply {
                        put(symbol, news)
                    }
                }
            }
        }
    }

    private fun generateMockNews(): List<FinnhubNewsResponse> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            FinnhubNewsResponse(
                id = 1,
                headline = "Global markets rally as tech sectors see record investments.",
                summary = "Investors are pouring billions into emerging AI and tech startups, driving indexes higher.",
                url = "https://www.bloomberg.com/markets",
                image = "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=600&auto=format&fit=crop",
                source = "Bloomberg",
                datetime = now - 3600
            ),
            FinnhubNewsResponse(
                id = 2,
                headline = "Federal Reserve announces steady interest rates for Q3.",
                summary = "The central bank decided to pause rate hikes amid cooling inflation data.",
                url = "https://www.reuters.com/finance",
                image = "https://images.unsplash.com/photo-1526304640581-d334cdbbf45e?q=80&w=600&auto=format&fit=crop",
                source = "Reuters",
                datetime = now - 7200
            ),
            FinnhubNewsResponse(
                id = 3,
                headline = "Oil prices fluctuate ahead of OPEC+ meeting.",
                summary = "Energy markets remain volatile as traders anticipate new production quotas.",
                url = "https://www.wsj.com/markets",
                image = "https://images.unsplash.com/photo-1518186285589-2f7649de83e0?q=80&w=600&auto=format&fit=crop",
                source = "WSJ",
                datetime = now - 14400
            )
        )
    }

    private fun defaultStockStateMap(symbols: List<String>): Map<String, StockState> =
        symbols.associateWith { symbol -> defaultStockState(symbol) }

    private fun defaultStockState(symbol: String): StockState {
        val quote = if (symbol.equals("IREDA.NS", ignoreCase = true) || symbol.equals("IREDA", ignoreCase = true)) {
            StockResponse(c = 150.0, h = 150.0, l = 150.0, o = 148.0, pc = 148.0)
        } else if (symbol.equals("INDHOTEL.NS", ignoreCase = true) || symbol.equals("INDHOTEL", ignoreCase = true)) {
            StockResponse(c = 580.0, h = 580.0, l = 580.0, o = 575.0, pc = 575.0)
        } else if (symbol.equals("NVDA", ignoreCase = true)) {
            StockResponse(c = 120.0, h = 120.0, l = 120.0, o = 118.0, pc = 118.0)
        } else {
            StockResponse(c = 100.0, h = 100.0, l = 100.0, o = 99.0, pc = 99.0)
        }
        return StockState(isLoading = false, data = quote, source = QuoteSource.DEFAULT)
    }

    private fun normalizeSymbol(symbol: String): String = if (symbol.equals("IREDA", ignoreCase = true)) "IREDA.NS" else symbol

    companion object {
        private const val TAG = "StockViewModel"
    }
}
