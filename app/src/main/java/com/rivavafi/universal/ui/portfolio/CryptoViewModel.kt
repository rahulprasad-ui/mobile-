package com.rivavafi.universal.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CryptoData(
    val price: Double,
    val change24h: Double
)

@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {
    private val dummyCryptoData = mapOf(
        "bitcoin" to CryptoData(price = 7330590.0, change24h = 3.47),
        "ethereum" to CryptoData(price = 224359.0, change24h = 3.77),
        "solana" to CryptoData(price = 8252.12, change24h = 3.11)
    )

    private val _cryptoStates = MutableStateFlow<Map<String, CryptoData>>(emptyMap())
    val cryptoStates: StateFlow<Map<String, CryptoData>> = _cryptoStates

    private var currentIds: List<String> = emptyList()
    private var pollingJob: kotlinx.coroutines.Job? = null

    fun startPolling(ids: List<String>) {
        currentIds = ids
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchCrypto(ids)
                delay(30000) // Poll crypto every 30 seconds
            }
        }
    }

    fun refresh() {
        if (currentIds.isNotEmpty()) {
            viewModelScope.launch {
                fetchCrypto(currentIds)
            }
        }
    }

    private suspend fun fetchCrypto(ids: List<String>) {
        val updated = _cryptoStates.value.toMutableMap()
        val defaultCryptoData = CryptoData(price = 0.0, change24h = 0.0)
        ids.forEach { id ->
            repository.getCryptoQuote(id).collect { result ->
                result.onSuccess { quote ->
                    if (quote.c > 0.0) {
                        val changePercent = if (quote.pc != 0.0) ((quote.c - quote.pc) / quote.pc) * 100 else 0.0
                        updated[id] = CryptoData(
                            price = quote.c,
                            change24h = changePercent
                        )
                    } else {
                        updated[id] = dummyCryptoData[id] ?: defaultCryptoData
                    }
                }.onFailure {
                    if (updated[id] == null) {
                        updated[id] = dummyCryptoData[id] ?: defaultCryptoData
                    }
                }
            }
        }

        if (updated.isEmpty() || updated.values.all { it.price <= 0.0 }) {
            _cryptoStates.value = showDummyCryptoData(ids)
            return
        }

        ids.forEach { id ->
            if (updated[id] == null) {
                updated[id] = dummyCryptoData[id] ?: defaultCryptoData
            }
        }
        _cryptoStates.value = updated
    }

    private fun showDummyCryptoData(ids: List<String>): Map<String, CryptoData> {
        return ids.associateWith { id -> dummyCryptoData[id] ?: CryptoData(price = 0.0, change24h = 0.0) }
    }
}
