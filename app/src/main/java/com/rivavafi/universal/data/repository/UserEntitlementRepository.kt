package com.rivavafi.universal.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class EntitlementStatus { LOADING, UNLOCKED, LOCKED, ERROR }

data class PremiumState(
    val status: EntitlementStatus = EntitlementStatus.LOADING,
    val isPremium: Boolean = false,
    val source: String? = null
)

data class OrderResult(
    val success: Boolean,
    val orderId: String? = null,
    val paymentUrl: String? = null,
    val error: String? = null
)

@Singleton
class UserEntitlementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    private val auth = FirebaseAuth.getInstance()

    private val _premiumState = MutableStateFlow(PremiumState())
    val premiumState: StateFlow<PremiumState> = _premiumState.asStateFlow()
    private var snapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    suspend fun syncEntitlement() {
        val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", Context.MODE_PRIVATE)
        val localPremium = prefs.getBoolean("isPremium", false)
        val localPremiumSource = prefs.getString("premium_source", null)
        val hasLocalKeyUnlock = localPremium && localPremiumSource == "access_key"

        if (localPremium) {
            _premiumState.value = PremiumState(EntitlementStatus.UNLOCKED, true, "cache")
        } else {
            _premiumState.value = PremiumState(EntitlementStatus.LOADING, false, null)
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            if (hasLocalKeyUnlock) {
                _premiumState.value = PremiumState(EntitlementStatus.UNLOCKED, true, "access_key")
                userPreferencesRepository.setPremiumUserForCurrent(true)
            } else {
                _premiumState.value = PremiumState(EntitlementStatus.LOCKED, false, null)
                prefs.edit().putBoolean("isPremium", false).putBoolean("portfolio_unlocked", false).remove("premium_source").apply()
                userPreferencesRepository.setPremiumUserForCurrent(false)
            }
            snapshotListener?.remove()
            return
        }

        try {
            snapshotListener?.remove()
            snapshotListener = firestore.collection("therivdata").document(uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("UserEntitlement", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val isPremium = snapshot.getBoolean("premiumStatus") ?: false

                        val validPremium = isPremium
                        val effectivePremium = validPremium || hasLocalKeyUnlock
                        val effectiveSource = if (validPremium) "therivdata" else "access_key"

                        _premiumState.value = if (effectivePremium) {
                            PremiumState(EntitlementStatus.UNLOCKED, true, effectiveSource)
                        } else {
                            PremiumState(EntitlementStatus.LOCKED, false, null)
                        }

                        val edit = context.getSharedPreferences("RivavaPortfolioPrefs", Context.MODE_PRIVATE).edit()
                        edit.putBoolean("isPremium", effectivePremium)
                            .putBoolean("portfolio_unlocked", effectivePremium)
                        if (effectivePremium) {
                            edit.putString("premium_source", effectiveSource)
                        } else {
                            edit.remove("premium_source")
                        }
                        edit.apply()

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            userPreferencesRepository.setPremiumUserForCurrent(effectivePremium)
                        }
                    }
                }
            val docSnap = firestore.collection("therivdata").document(uid).get().await()
            val isPremium = docSnap.getBoolean("premiumStatus") ?: false

            val validPremium = isPremium
            val effectivePremium = validPremium || hasLocalKeyUnlock
            val effectiveSource = if (validPremium) "therivdata" else "access_key"

            _premiumState.value = if (effectivePremium) {
                PremiumState(EntitlementStatus.UNLOCKED, true, effectiveSource)
            } else {
                PremiumState(EntitlementStatus.LOCKED, false, null)
            }

            val edit = prefs.edit()
                .putBoolean("isPremium", effectivePremium)
                .putBoolean("portfolio_unlocked", effectivePremium)
            if (effectivePremium) {
                edit.putString("premium_source", effectiveSource)
            } else {
                edit.remove("premium_source")
            }
            edit.apply()
            userPreferencesRepository.setPremiumUserForCurrent(effectivePremium)

        } catch (e: Exception) {
            Log.e("UserEntitlement", "Error syncing entitlement", e)
            _premiumState.value = PremiumState(EntitlementStatus.ERROR, localPremium, "cache")
        }
    }

    suspend fun createPaymentOrder(amountPaise: Int = 1100, plan: String = "portfolio_premium"): OrderResult {
        val uid = auth.currentUser?.uid ?: return OrderResult(false, error = "User not authenticated")
        val userEmail = auth.currentUser?.email

        return try {
            Log.i("UserEntitlement", "PAYMENT_CREATE_STARTED on Node.js REST API")
            val req = com.rivavafi.universal.data.network.CreatePaymentOrderRequest(
                userId = uid,
                userEmail = userEmail,
                plan = plan,
                amountPaise = amountPaise
            )
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.createPaymentOrder(req)

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val orderId = data?.orderId
                val paymentUrl = data?.paymentUrl

                if (orderId != null) {
                    Log.i("UserEntitlement", "PAYMENT_CREATE_SUCCESS: $orderId")
                    OrderResult(true, orderId, paymentUrl = paymentUrl)
                } else {
                    OrderResult(false, error = "Invalid response from payment server")
                }
            } else {
                val errorMsg = response.body()?.message ?: "Failed to initialize payment with server"
                OrderResult(false, error = errorMsg)
            }
        } catch (e: Exception) {
            Log.e("UserEntitlement", "Error creating payment order via Node.js backend", e)
            OrderResult(false, error = e.message ?: "Network error connecting to payment gateway")
        }
    }

    suspend fun createUroPayOrder(amountPaise: Int): OrderResult {
        return createPaymentOrder(amountPaise)
    }

    suspend fun verifyPayment(orderId: String, paymentId: String? = null, signature: String? = null): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            Log.i("UserEntitlement", "PAYMENT_VERIFY_STARTED on Node.js REST API for order $orderId")
            val req = com.rivavafi.universal.data.network.VerifyPaymentRequest(
                userId = uid,
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            )
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.verifyPayment(req)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.i("UserEntitlement", "PAYMENT_VERIFY_SUCCESS on Node.js backend")
                _premiumState.value = PremiumState(EntitlementStatus.UNLOCKED, true, "payment_gateway")
                val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("isPremium", true)
                    .putBoolean("portfolio_unlocked", true)
                    .putString("premium_source", "payment_gateway")
                    .apply()
                userPreferencesRepository.setPremiumUserForCurrent(true)
                true
            } else {
                Log.w("UserEntitlement", "PAYMENT_VERIFY_FAILED: Server returned failure")
                _premiumState.value = PremiumState(EntitlementStatus.ERROR, false, null)
                false
            }
        } catch (e: Exception) {
            Log.e("UserEntitlement", "Failed to verify payment via Node.js REST API", e)
            _premiumState.value = PremiumState(EntitlementStatus.ERROR, false, null)
            false
        }
    }

    suspend fun verifyUroPayPayment(orderId: String): Boolean {
        return verifyPayment(orderId)
    }



    suspend fun clearEntitlement() {
        val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("isPremium", false)
            .putBoolean("portfolio_unlocked", false)
            .remove("premium_source")
            .apply()
        userPreferencesRepository.setPremiumUserForCurrent(false)
        _premiumState.value = PremiumState(EntitlementStatus.LOCKED, false, null)
    }

    suspend fun verifyAndRedeemSecretKey(rawKey: String): Result<String> {
        val cleanKey = rawKey.trim().uppercase()
        if (cleanKey.isBlank() || cleanKey.length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid secret key."))
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            return Result.failure(IllegalStateException("Please log in to your account before activating a key."))
        }

        val userEmail = auth.currentUser?.email

        return try {
            Log.i("UserEntitlement", "SECRET_KEY_VERIFY_STARTED for user $uid")

            var verificationSuccess = false
            var failureReason = "Invalid secret key. Please verify and try again."

            // 1. Primary: Call Node.js Express REST API Backend directly
            try {
                val apiReq = com.rivavafi.universal.data.network.RedeemKeyRequest(
                    secretKey = cleanKey,
                    userId = uid,
                    userEmail = userEmail
                )
                val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.redeemSecretKey(apiReq)

                if (response.isSuccessful && response.body()?.success == true) {
                    verificationSuccess = true
                    Log.i("UserEntitlement", "Node.js REST API verified secret key successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val parsedMsg = try {
                        val json = org.json.JSONObject(errorBody ?: "{}")
                        if (json.has("message")) json.getString("message") else null
                    } catch (e: Exception) {
                        null
                    }
                    failureReason = response.body()?.message ?: parsedMsg ?: "Invalid secret key. Please check and try again."
                }
            } catch (netEx: Exception) {
                Log.w("UserEntitlement", "Node.js API call encountered exception, attempting secure fallback", netEx)

                // 2. Secure Firestore atomic transaction fallback if backend offline
                val keyHash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(cleanKey.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }

                val keyRef = firestore.collection("secret_keys").document(keyHash)

                verificationSuccess = firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(keyRef)
                    if (!snapshot.exists()) {
                        failureReason = "Invalid secret key. Please check and try again."
                        return@runTransaction false
                    }

                    val isRevoked = snapshot.getBoolean("isRevoked") ?: false
                    val status = snapshot.getString("status") ?: "active"
                    if (isRevoked || status == "revoked") {
                        val reason = snapshot.getString("revokedReason")
                        failureReason = if (!reason.isNullOrBlank()) "Key revoked: $reason" else "This secret key has been revoked."
                        return@runTransaction false
                    }

                    val expiresAt = snapshot.getDate("expiresAt")
                    if (expiresAt != null && java.util.Date().after(expiresAt)) {
                        failureReason = "This secret key has expired."
                        return@runTransaction false
                    }

                    val assignedEmail = snapshot.getString("assignedEmail")
                    if (!assignedEmail.isNullOrBlank() && !userEmail.isNullOrBlank() && !assignedEmail.equals(userEmail, ignoreCase = true)) {
                        failureReason = "This key was issued for a different account."
                        return@runTransaction false
                    }

                    val useCount = snapshot.getLong("useCount") ?: 0
                    val maxUses = snapshot.getLong("maxUses") ?: 1
                    if (useCount >= maxUses || status == "redeemed") {
                        failureReason = "This secret key has already been redeemed and reached maximum uses."
                        return@runTransaction false
                    }

                    val newUseCount = useCount + 1
                    val newStatus = if (newUseCount >= maxUses) "redeemed" else "active"
                    val redemptionEntry = mapOf(
                        "userId" to uid,
                        "userEmail" to (userEmail ?: ""),
                        "redeemedAt" to java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
                    )

                    transaction.update(keyRef, mapOf(
                        "useCount" to newUseCount,
                        "status" to newStatus,
                        "redeemedBy" to FieldValue.arrayUnion(redemptionEntry),
                        "lastRedeemedAt" to FieldValue.serverTimestamp()
                    ))

                    val userRef = firestore.collection("users").document(uid)
                    transaction.set(userRef, mapOf(
                        "is_premium" to true,
                        "premium_status" to "active",
                        "premium_source" to "secret_key",
                        "premium_unlocked_at" to FieldValue.serverTimestamp(),
                        "unlocked_key_id" to keyHash
                    ), com.google.firebase.firestore.SetOptions.merge())

                    val therivRef = firestore.collection("therivdata").document(uid)
                    val therivavaRef = firestore.collection("therivavadata").document(uid)
                    transaction.set(therivRef, mapOf("premiumStatus" to true), com.google.firebase.firestore.SetOptions.merge())
                    transaction.set(therivavaRef, mapOf("premiumStatus" to true), com.google.firebase.firestore.SetOptions.merge())

                    true
                }.await()
            }

            if (verificationSuccess) {
                val prefs = context.getSharedPreferences("RivavaPortfolioPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("isPremium", true)
                    .putBoolean("portfolio_unlocked", true)
                    .putString("premium_source", "secret_key")
                    .apply()

                userPreferencesRepository.setPremiumUserForCurrent(true)
                _premiumState.value = PremiumState(EntitlementStatus.UNLOCKED, true, "secret_key")
                Log.i("UserEntitlement", "SECRET_KEY_VERIFY_SUCCESS for user $uid")
                Result.success("Premium access unlocked successfully!")
            } else {
                Log.w("UserEntitlement", "SECRET_KEY_VERIFY_FAILED: $failureReason")
                Result.failure(Exception(failureReason))
            }
        } catch (e: Exception) {
            Log.e("UserEntitlement", "Exception during secret key verification", e)
            Result.failure(Exception(e.message ?: "Failed to verify secret key with server. Please try again."))
        }
    }

    suspend fun unlockWithSecretKey(key: String = ""): Boolean {
        return if (key.isNotBlank()) {
            verifyAndRedeemSecretKey(key).isSuccess
        } else {
            false
        }
    }
}
