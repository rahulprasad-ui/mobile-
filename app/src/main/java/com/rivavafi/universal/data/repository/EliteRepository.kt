package com.rivavafi.universal.data.repository

import android.util.Log
import com.rivavafi.universal.data.repository.OrderResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class EliteConfig(
    val totalSeats: Int = 100,
    val occupiedSeats: Int = 0
)

data class EliteSubscription(
    val isElite: Boolean = false,
    val plan: String = "",
    val minutesRemaining: Int = 0,
    val monthlyMinutes: Int = 0,
    val autoRenew: Boolean = false,
    val nextBillingDate: Long = 0,
    val paymentStatus: String = ""
)

data class EliteSession(
    val id: String,
    val selectedDate: Long,
    val selectedTime: String,
    val status: String,
    val minutesBooked: Int,
    val meetingLink: String?
)

@Singleton
class EliteRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    private val auth = FirebaseAuth.getInstance()

    private val TAG = "EliteRepository"

    fun getEliteConfig(): Flow<EliteConfig> = callbackFlow {
        val listener = firestore.collection("elite_membership_meta").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching elite config", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val totalSeats = snapshot.getLong("totalSeats")?.toInt() ?: 100
                    val occupiedSeats = snapshot.getLong("occupiedSeats")?.toInt() ?: 0
                    trySend(EliteConfig(totalSeats, occupiedSeats))
                } else {
                    trySend(EliteConfig(100, 0))
                }
            }

        awaitClose { listener.remove() }
    }

    fun getUserSubscription(): Flow<EliteSubscription> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(EliteSubscription())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(uid).collection("subscription").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user subscription", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val isElite = snapshot.getBoolean("isElite") ?: false
                    val plan = snapshot.getString("plan") ?: ""
                    val minutesRemaining = snapshot.getLong("minutesRemaining")?.toInt() ?: 0
                    val monthlyMinutes = snapshot.getLong("monthlyMinutes")?.toInt() ?: 0
                    val autoRenew = snapshot.getBoolean("autoRenew") ?: false
                    val nextBillingDate = snapshot.getTimestamp("nextBillingDate")?.seconds?.times(1000) ?: 0L
                    val paymentStatus = snapshot.getString("paymentStatus") ?: ""

                    trySend(EliteSubscription(isElite, plan, minutesRemaining, monthlyMinutes, autoRenew, nextBillingDate, paymentStatus))
                } else {
                    trySend(EliteSubscription())
                }
            }

        awaitClose { listener.remove() }
    }

    fun getUserSessions(): Flow<List<EliteSession>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("elite_sessions")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user sessions", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val selectedDate = doc.getTimestamp("selectedDate")?.seconds?.times(1000) ?: 0L
                        val selectedTime = doc.getString("selectedTime") ?: ""
                        val status = doc.getString("status") ?: ""
                        val minutesBooked = doc.getLong("minutesBooked")?.toInt() ?: 0
                        val meetingLink = doc.getString("meetingLink")

                        EliteSession(id, selectedDate, selectedTime, status, minutesBooked, meetingLink)
                    }.sortedByDescending { it.selectedDate }

                    trySend(sessions)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createEliteOrder(): OrderResult {
        val uid = auth.currentUser?.uid ?: return OrderResult(false, error = "User not authenticated")
        val email = auth.currentUser?.email

        return try {
            val req = com.rivavafi.universal.data.network.CreatePaymentOrderRequest(
                userId = uid,
                userEmail = email,
                plan = "elite_399",
                amountPaise = 39900 // 399 INR
            )
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.createPaymentOrder(req)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                val orderId = data?.orderId
                val paymentUrl = data?.paymentUrl
                if (orderId != null) {
                    OrderResult(true, orderId, paymentUrl)
                } else {
                    OrderResult(false, error = "Invalid response from payment gateway")
                }
            } else {
                OrderResult(false, error = response.body()?.message ?: "Failed to create payment order")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Elite order via backend", e)
            OrderResult(false, error = e.message ?: "Network error connecting to payment gateway")
        }
    }

    suspend fun verifyElitePayment(orderId: String, paymentId: String? = null, signature: String? = null): Boolean {
        val uid = auth.currentUser?.uid ?: return false

        return try {
            val req = com.rivavafi.universal.data.network.VerifyPaymentRequest(
                userId = uid,
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            )
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.verifyPayment(req)
            val isSuccess = response.isSuccessful && response.body()?.success == true

            if (isSuccess) {
                val subData = hashMapOf(
                    "isElite" to true,
                    "plan" to "elite_399",
                    "minutesRemaining" to 600,
                    "monthlyMinutes" to 600,
                    "autoRenew" to false,
                    "nextBillingDate" to com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)),
                    "paymentStatus" to "success",
                    "orderId" to orderId
                )
                firestore.collection("users").document(uid).collection("subscription").document("current")
                    .set(subData, com.google.firebase.firestore.SetOptions.merge())

                firestore.collection("therivdata").document(uid)
                    .set(mapOf("premiumStatus" to true, "isElite" to true), com.google.firebase.firestore.SetOptions.merge())

                firestore.collection("therivavadata").document(uid)
                    .set(mapOf("premiumStatus" to true, "isElite" to true), com.google.firebase.firestore.SetOptions.merge())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Elite payment", e)
            false
        }
    }

    suspend fun bookSession(duration: Int, dateMillis: Long, time: String): Boolean {
        if (auth.currentUser?.uid == null) return false

        return try {
            val data = hashMapOf(
                "duration" to duration,
                "date" to dateMillis,
                "time" to time
            )
            val result = functions
                .getHttpsCallable("bookEliteSession")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
            resultData?.get("success") == true
        } catch (e: Exception) {
            Log.e(TAG, "Error booking session", e)
            false
        }
    }

    suspend fun cancelSubscription(): Boolean {
         if (auth.currentUser?.uid == null) return false

        return try {
            val result = functions
                .getHttpsCallable("cancelEliteSubscription")
                .call()
                .await()

            val resultData = result.data as? Map<*, *>
            resultData?.get("success") == true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling subscription", e)
            false
        }
    }
}