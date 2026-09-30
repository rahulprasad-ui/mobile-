package com.rivavafi.universal.ui.elite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.repository.EliteConfig
import com.rivavafi.universal.data.repository.EliteRepository
import com.rivavafi.universal.data.repository.EliteSession
import com.rivavafi.universal.data.repository.EliteSubscription
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EliteViewModel @Inject constructor(
    private val eliteRepository: EliteRepository
) : ViewModel() {

    private val _eliteConfig = MutableStateFlow(EliteConfig())
    val eliteConfig: StateFlow<EliteConfig> = _eliteConfig

    private val _subscription = MutableStateFlow(EliteSubscription())
    val subscription: StateFlow<EliteSubscription> = _subscription

    private val _sessions = MutableStateFlow<List<EliteSession>>(emptyList())
    val sessions: StateFlow<List<EliteSession>> = _sessions

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            eliteRepository.getEliteConfig().collectLatest {
                _eliteConfig.value = it
            }
        }
        viewModelScope.launch {
            eliteRepository.getUserSubscription().collectLatest {
                _subscription.value = it
            }
        }
        viewModelScope.launch {
            eliteRepository.getUserSessions().collectLatest {
                _sessions.value = it
            }
        }
    }

    fun bookSession(duration: Int, dateMillis: Long, time: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val success = eliteRepository.bookSession(duration, dateMillis, time)
            if (!success) {
                _error.value = "Failed to book session. Please ensure you have enough minutes."
            }
            _isLoading.value = false
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val success = eliteRepository.cancelSubscription()
            if (!success) {
                _error.value = "Failed to cancel subscription."
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
