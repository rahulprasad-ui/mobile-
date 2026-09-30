package com.rivavafi.universal.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val actionType: String? = null // "CALL", "WHATSAPP", "UNLOCK_PORTFOLIO", "PAY_399", "NONE"
)

@HiltViewModel
class HelpChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! 👋 I'm your Rivava+ AI Financial Assistant. How can I help you today?",
                isUser = false,
                actionType = null
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    val quickPrompts = listOf(
        "🔑 Secret Key / Unlock",
        "💳 Pay ₹399 Access",
        "📱 SMS Tracking Setup",
        "👨‍💼 Elite Private Session",
        "📞 Speak to Live Advisor"
    )

    fun sendMessage(text: String, userProfileInfo: String = "") {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _messages.value = _messages.value + userMsg

        viewModelScope.launch {
            _isTyping.value = true
            delay(800) // Realistic typing simulation
            val botResponse = generateBotResponse(trimmed, userProfileInfo)
            _isTyping.value = false
            _messages.value = _messages.value + botResponse
        }
    }

    private fun generateBotResponse(input: String, userProfileInfo: String): ChatMessage {
        val lower = input.lowercase()

        return when {
            lower.contains("secret") || lower.contains("key") || lower.contains("unlock") || lower.contains("code") || lower.contains("license") -> {
                ChatMessage(
                    text = """
                        🔑 **Secret Access Key Guide:**
                        
                        * **Format:** Keys follow the pattern `RIV-XXXX-XXXX-XXXX-XXXX` (All Capital Letters & Digits 2-9).
                        * **Where to Enter:** Go to **Portfolio** tab, tap **Unlock**, paste your key, and tap **"Verify & Unlock"**.
                        * **Direct Unlock:** You can also unlock instantly via in-app payment of ₹399 without needing a secret key.
                    """.trimIndent(),
                    isUser = false,
                    actionType = "UNLOCK_PORTFOLIO"
                )
            }

            lower.contains("399") || lower.contains("pay") || lower.contains("payment") || lower.contains("price") || lower.contains("cost") || lower.contains("subscription") -> {
                ChatMessage(
                    text = """
                        💳 **Rivava+ ₹399 Special Access:**
                        
                        * **Included Benefits:**
                          • Full Portfolio & Stock PDF analytics unlocked
                          • 1-on-1 Private Wealth Consult with Fund Manager
                          • 600 monthly advisory minutes
                          • Real-time financial insights
                        * **Instant Activation:** When you complete payment via the app, your account upgrades immediately.
                    """.trimIndent(),
                    isUser = false,
                    actionType = "PAY_399"
                )
            }

            lower.contains("sms") || lower.contains("track") || lower.contains("expense") || lower.contains("scan") || lower.contains("bank") -> {
                ChatMessage(
                    text = """
                        📱 **Automated SMS & Expense Tracking:**
                        
                        * **How it works:** Rivava automatically reads bank transaction SMS messages on your device to log expenses instantly.
                        * **Privacy First:** Only financial OTPs and bank alerts are categorized locally. Your data is encrypted and never sold.
                        * **Manual Entry:** You can also tap the **'+' button** on Home to log cash transactions manually.
                    """.trimIndent(),
                    isUser = false,
                    actionType = null
                )
            }

            lower.contains("elite") || lower.contains("advisor") || lower.contains("manager") || lower.contains("session") || lower.contains("call") -> {
                ChatMessage(
                    text = """
                        👨‍💼 **Rivava Elite & Private Fund Manager:**
                        
                        * You get dedicated 1-on-1 private video consultations and portfolio reviews.
                        * **Live Helpline:** +91-8881176909
                        * **WhatsApp Support:** Available 24/7 for VIP members.
                    """.trimIndent(),
                    isUser = false,
                    actionType = "CALL"
                )
            }

            lower.contains("calc") || lower.contains("emi") || lower.contains("mdr") || lower.contains("ratio") || lower.contains("tool") -> {
                ChatMessage(
                    text = """
                        🧮 **Financial Tools & Calculators:**
                        
                        * Tap the **Tools** tab in the bottom navigation bar to access:
                          • **R&E Calculator** (Ratio & Equivalence)
                          • **MDR Calculator** (0.4% with ₹2,000 threshold waiver)
                          • **Loan EMI Calculator** (Principal vs Interest breakdown)
                          • **Compound Interest, SIP, GST, FD/RD & Inflation**
                    """.trimIndent(),
                    isUser = false,
                    actionType = null
                )
            }

            lower.contains("hi") || lower.contains("hello") || lower.contains("hey") -> {
                ChatMessage(
                    text = "Hello! 😊 How can I assist you with Rivava TrackFi today? You can ask about unlocking your Portfolio, payments, SMS tracking, or connect with our financial advisory team.",
                    isUser = false,
                    actionType = null
                )
            }

            else -> {
                ChatMessage(
                    text = """
                        I understand your query. For specialized assistance or personalized account queries:
                        
                        📞 **Helpline:** +91 8881176909
                        💬 **WhatsApp:** Available 24/7 for instant chat
                        📧 **Email:** support@rivava.in
                        
                        You can tap below to connect with a live advisor immediately!
                    """.trimIndent(),
                    isUser = false,
                    actionType = "WHATSAPP"
                )
            }
        }
    }
}

