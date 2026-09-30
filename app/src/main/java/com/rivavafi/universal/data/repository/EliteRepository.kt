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
        if (auth.currentUser?.uid == null) return OrderResult(false, error = "User not authenticated")

        return try {
            val data = hashMapOf("amount" to 330000) // 3300 INR in paise
            val result = functions
                .getHttpsCallable("createEliteOrder")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
            val orderId = resultData?.get("order_id") as? String
            val paymentUrl = resultData?.get("payment_url") as? String

            if (orderId != null && paymentUrl != null) {
                OrderResult(true, orderId, paymentUrl)
            } else {
                OrderResult(false, error = "Invalid response from server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Elite order", e)
            OrderResult(false, error = e.message ?: "Unknown error")
        }
    }

    suspend fun verifyElitePayment(orderId: String): Boolean {
        if (auth.currentUser?.uid == null) return false

        return try {
            val data = hashMapOf("order_id" to orderId)
            val result = functions
                .getHttpsCallable("verifyElitePayment")
                .call(data)
                .await()

            val resultData = result.data as? Map<*, *>
            resultData?.get("success") == true
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