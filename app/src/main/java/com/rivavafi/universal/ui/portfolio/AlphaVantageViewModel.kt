package com.rivavafi.universal.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.domain.api.Quote
import com.rivavafi.universal.domain.repository.AlphaVantageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlphaVantageViewModel @Inject constructor() : ViewModel() {

    private val repo = AlphaVantageRepository()

    private val _stockData = MutableStateFlow<List<Quote>>(emptyList())
    val stockData: StateFlow<List<Quote>> = _stockData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadStocks(symbols: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = mutableListOf<Quote>()

            for (symbol in symbols) {
                val data = repo.getStock(symbol)

                if (data != null) {
                    result.add(data)
                }

                delay(12000) // avoid rate limit
            }

            _stockData.value = result
            _isLoading.value = false
        }
    }

    fun startAutoRefresh(symbols: List<String>) {
        viewModelScope.launch {
            while (true) {
                loadStocks(symbols)
                delay(60000) // 60 sec
            }
        }
    }
}
