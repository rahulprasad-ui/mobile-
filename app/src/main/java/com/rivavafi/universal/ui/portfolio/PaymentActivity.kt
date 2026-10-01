package com.rivavafi.universal.ui.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.rivavafi.universal.data.network.CreatePaymentOrderRequest
import com.rivavafi.universal.data.network.RetrofitClient
import com.rivavafi.universal.data.network.VerifyPaymentRequest
import com.rivavafi.universal.data.repository.EliteRepository
import com.rivavafi.universal.data.repository.UserEntitlementRepository
import com.rivavafi.universal.ui.theme.AmoledBlack
import com.rivavafi.universal.ui.theme.PrimarySky
import com.rivavafi.universal.ui.theme.RivavaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class PaymentActivity : ComponentActivity(), PaymentResultWithDataListener {

    @Inject
    lateinit var entitlementRepository: UserEntitlementRepository

    @Inject
    lateinit var eliteRepository: EliteRepository

    private var currentOrderId: String? = null
    private var plan: String = "portfolio_premium"
    private var amountPaise: Int = 39900
    private var titleText: String = "Rivava Portfolio Premium"

    private var statusMessage = mutableStateOf("Initializing secure payment...")
    private var isLoading = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Checkout.preload(applicationContext)

        plan = intent.getStringExtra("plan") ?: "portfolio_premium"
        amountPaise = intent.getIntExtra("amountPaise", 39900)
        titleText = intent.getStringExtra("title") ?: if (plan.contains("elite")) "Rivava Elite Membership" else "Rivava Portfolio Premium"

        setContent {
            RivavaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AmoledBlack
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (isLoading.value) {
                                CircularProgressIndicator(color = PrimarySky, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            Text(
                                text = statusMessage.value,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        initiateRazorpayPayment()
    }

    private fun initiateRazorpayPayment() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        val email = auth.currentUser?.email ?: "user@rivava.in"

        if (uid == null) {
            Toast.makeText(this, "Please log in before completing payment.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                statusMessage.value = "Creating payment order..."
                val req = CreatePaymentOrderRequest(
                    userId = uid,
                    userEmail = email,
                    plan = plan,
                    amountPaise = amountPaise
                )
                val response = RetrofitClient.apiService.createPaymentOrder(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val orderId = data?.orderId
                    val keyId = data?.keyId?.takeIf { it.isNotBlank() && !it.contains("mock") } ?: "rzp_test_1DP5mmOlF5G5ag"
                    currentOrderId = orderId

                    if (orderId != null) {
                        val isTestKey = keyId.startsWith("rzp_test_")
                        statusMessage.value = if (isTestKey) "Opening Razorpay Gateway (Test Mode)..." else "Opening Secure Payment Gateway..."
                        launchRazorpayCheckout(orderId, keyId, email, amountPaise)
                    } else {
                        failPayment("Invalid order response from payment gateway.")
                    }
                } else {
                    failPayment(response.body()?.message ?: "Failed to create payment order.")
                }
            } catch (e: Exception) {
                failPayment("Payment connection error: ${e.localizedMessage}")
            }
        }
    }

    private fun launchRazorpayCheckout(orderId: String, keyId: String, email: String, amountPaise: Int) {
        val checkout = Checkout()
        val effectiveKey = if (keyId.isBlank() || keyId.contains("mock")) "rzp_test_1DP5mmOlF5G5ag" else keyId
        checkout.setKeyID(effectiveKey)

        try {
            val isOfficialRazorpayOrder = orderId.startsWith("order_") && 
                orderId.substring(6).matches(Regex("^[a-zA-Z0-9]{14,24}$"))

            val options = JSONObject().apply {
                put("name", "Rivava TrackFi")
                put("description", titleText)
                put("image", "https://rivava.in/logo.png")
                put("currency", "INR")
                put("amount", amountPaise)
                put("prefill.email", email)

                // Only attach order_id if generated by Razorpay Orders API
                if (isOfficialRazorpayOrder) {
                    put("order_id", orderId)
                }

                val theme = JSONObject().apply {
                    put("color", "#D4AF37")
                }
                put("theme", theme)

                val retryObj = JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                }
                put("retry", retryObj)
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            failPayment("Failed to open Razorpay checkout: ${e.message}")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = currentOrderId ?: paymentData?.orderId
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId
        val signature = paymentData?.signature

        if (orderId == null) {
            failPayment("Payment completed but order details missing.")
            return
        }

        statusMessage.value = "Verifying payment with secure server..."
        isLoading.value = true

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        lifecycleScope.launch {
            try {
                val req = VerifyPaymentRequest(
                    userId = uid,
                    orderId = orderId,
                    paymentId = paymentId,
                    signature = signature
                )
                val response = RetrofitClient.apiService.verifyPayment(req)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Update entitlement locally and in Firestore
                    entitlementRepository.syncEntitlement()
                    if (plan.contains("elite")) {
                        eliteRepository.verifyElitePayment(orderId, paymentId, signature)
                    }

                    Toast.makeText(this@PaymentActivity, "🎉 Payment Successful! Access Unlocked.", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra("orderId", orderId)
                        putExtra("paymentId", paymentId)
                    })
                    finish()
                } else {
                    failPayment("Payment verification failed on server. Please contact support.")
                }
            } catch (e: Exception) {
                failPayment("Error verifying payment: ${e.message}")
            }
        }
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        val message = when (errorCode) {
            Checkout.NETWORK_ERROR -> "Network error during payment. Please check your internet."
            Checkout.INVALID_OPTIONS -> {
                if (response?.contains("key_id", ignoreCase = true) == true || response?.contains("exist", ignoreCase = true) == true) {
                    "Payment setup: Razorpay Key ID is missing or invalid in server config."
                } else {
                    response ?: "Invalid payment options."
                }
            }
            Checkout.PAYMENT_CANCELED -> "Payment was cancelled."
            Checkout.TLS_ERROR -> "Device does not support TLS 1.2+."
            else -> response ?: "Payment failed or cancelled."
        }
        failPayment(message)
    }

    private fun failPayment(msg: String) {
        isLoading.value = false
        statusMessage.value = msg
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_CANCELED, Intent().apply {
            putExtra("error", msg)
        })
        finish()
    }
}
