package com.rivavafi.universal.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.repository.PremiumState
import com.rivavafi.universal.data.repository.OrderResult
import com.rivavafi.universal.data.repository.UserEntitlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PaymentUiState {
    IDLE, CREATING_ORDER, CHECKOUT_READY, VERIFYING, SUCCESS, ERROR
}

data class PaymentState(
    val uiState: PaymentUiState = PaymentUiState.IDLE,
    val orderData: OrderResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: UserEntitlementRepository
) : ViewModel() {

    val premiumState: StateFlow<PremiumState> = repository.premiumState

    private val _paymentState = MutableStateFlow(PaymentState())
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.syncEntitlement()
        }
    }

    fun syncEntitlement() {
        viewModelScope.launch {
            repository.syncEntitlement()
        }
    }

    fun startPremiumPurchase(amountPaise: Int = 39900) {
        if (_paymentState.value.uiState == PaymentUiState.CREATING_ORDER ||
            _paymentState.value.uiState == PaymentUiState.VERIFYING) return

        _paymentState.value = PaymentState(uiState = PaymentUiState.CREATING_ORDER)
        _keyVerificationState.value = KeyVerificationState.Verifying

        viewModelScope.launch {
            val result = repository.createUroPayOrder(amountPaise)
            if (result.success && result.orderId != null) {
                _paymentState.value = PaymentState(uiState = PaymentUiState.VERIFYING, orderData = result)
                val verified = repository.verifyUroPayPayment(result.orderId)
                if (verified) {
                    _paymentState.value = PaymentState(uiState = PaymentUiState.SUCCESS)
                    _keyVerificationState.value = KeyVerificationState.Success("Premium unlocked successfully!")
                } else {
                    _paymentState.value = PaymentState(
                        uiState = PaymentUiState.ERROR,
                        errorMessage = "Payment verification pending. Please check connection."
                    )
                    _keyVerificationState.value = KeyVerificationState.Error("Payment verification failed. Please try again.")
                }
            } else {
                val err = result.error ?: "Failed to create order"
                _paymentState.value = PaymentState(
                    uiState = PaymentUiState.ERROR,
                    errorMessage = err
                )
                _keyVerificationState.value = KeyVerificationState.Error(err)
            }
        }
    }


    fun verifyUroPayPayment(orderId: String) {
        if (_paymentState.value.uiState == PaymentUiState.VERIFYING) return

        _paymentState.value = _paymentState.value.copy(uiState = PaymentUiState.VERIFYING)

        viewModelScope.launch {
            val success = repository.verifyUroPayPayment(orderId)
            if (success) {
                _paymentState.value = PaymentState(uiState = PaymentUiState.SUCCESS)
            } else {
                _paymentState.value = PaymentState(
                    uiState = PaymentUiState.ERROR,
                    errorMessage = "Payment verification failed"
                )
            }
        }
    }

    fun clearPaymentError() {
        if (_paymentState.value.uiState == PaymentUiState.ERROR || _paymentState.value.uiState == PaymentUiState.CHECKOUT_READY) {
            _paymentState.value = PaymentState(uiState = PaymentUiState.IDLE)
        }
    }

    private val _keyVerificationState = MutableStateFlow<KeyVerificationState>(KeyVerificationState.Idle)
    val keyVerificationState: StateFlow<KeyVerificationState> = _keyVerificationState.asStateFlow()

    fun verifyAndRedeemSecretKey(key: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (_keyVerificationState.value is KeyVerificationState.Verifying) return

        _keyVerificationState.value = KeyVerificationState.Verifying

        viewModelScope.launch {
            val result = repository.verifyAndRedeemSecretKey(key)
            if (result.isSuccess) {
                val msg = result.getOrNull() ?: "Premium unlocked successfully!"
                _keyVerificationState.value = KeyVerificationState.Success(msg)
                onResult(true, msg)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Invalid secret key"
                _keyVerificationState.value = KeyVerificationState.Error(errorMsg)
                onResult(false, errorMsg)
            }
        }
    }

    fun resetKeyVerificationState() {
        _keyVerificationState.value = KeyVerificationState.Idle
    }

    fun unlockWithSecretKey(key: String = "") {
        verifyAndRedeemSecretKey(key)
    }
}

sealed class KeyVerificationState {
    object Idle : KeyVerificationState()
    object Verifying : KeyVerificationState()
    data class Success(val message: String) : KeyVerificationState()
    data class Error(val errorMessage: String) : KeyVerificationState()
}
