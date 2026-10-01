package com.rivavafi.universal.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.rivavafi.universal.domain.api.AuthApiService
import com.rivavafi.universal.domain.api.SendVerificationRequest
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService
) {
    val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sendOtp(phone: String): Result<String> {
        return try {
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.sendOtp(
                com.rivavafi.universal.data.network.OtpRequest(phone)
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "OTP sent")
            } else {
                val errorMsg = try {
                    JSONObject(response.errorBody()?.string() ?: "").optString("error", "Failed to send OTP")
                } catch (e: Exception) {
                    "Failed to send OTP"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: com.google.firebase.auth.PhoneAuthCredential): Result<String> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to retrieve Firebase UID")
            Result.success(uid)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase Phone Credential sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun verifyOtpAndSignIn(phone: String, otp: String): Result<String> {
        return try {
            val response = com.rivavafi.universal.data.network.RetrofitClient.apiService.verifyOtp(
                com.rivavafi.universal.data.network.VerifyOtpRequest(phone, otp)
            )
            val body = response.body()
            val token = body?.token ?: body?.data?.token
            val fallbackUid = body?.data?.uid ?: phone

            if (response.isSuccessful && (!token.isNullOrBlank() || !fallbackUid.isNullOrBlank())) {
                val uid = if (!token.isNullOrBlank()) {
                    try {
                        val authResult = auth.signInWithCustomToken(token).await()
                        authResult.user?.uid ?: fallbackUid
                    } catch (tokenErr: Exception) {
                        Log.w("AuthRepository", "Custom token sign-in warning: ${tokenErr.message}, using phone UID")
                        fallbackUid
                    }
                } else {
                    fallbackUid
                }
                Result.success(uid)
            } else {
                val errorMsg = try {
                    val rawErr = response.errorBody()?.string() ?: ""
                    if (rawErr.isNotBlank()) {
                        val json = JSONObject(rawErr)
                        json.optString("message", json.optString("error", "OTP verification failed"))
                    } else {
                        body?.message ?: "OTP verification failed"
                    }
                } catch (e: Exception) {
                    body?.message ?: "OTP verification failed"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendUserToSheet(user: FirebaseUser, provider: String, name: String? = null) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("name", name ?: user.displayName ?: "")
        json.put("email", user.email ?: "")
        json.put("phone", user.phoneNumber ?: "")
        json.put("uid", user.uid)
        json.put("provider", provider)

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://script.google.com/macros/s/AKfycbx2EmeSjsbcD_bGTZQBmG7xwhUBEdvjL33k4GqqcH8lv-b4mmzzjAOtZt7FwQksVvhF/exec")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AuthRepository", "Failed to send user to sheet", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("AuthRepository", "Successfully sent user to sheet")
                } else {
                    Log.e("AuthRepository", "Failed to send user to sheet. Code: ${response.code}")
                }
                response.close()
            }
        })
    }

    data class UserSessionState(
        val isNewUser: Boolean,
        val existingName: String?,
        val onboardingCompleted: Boolean,
        val photoUrl: String?
    )

    suspend fun saveUserToFirestore(
        uid: String,
        name: String?,
        email: String?,
        phoneNumber: String?,
        authProvider: String,
        isVerified: Boolean = false,
        photoUrl: String? = null
    ): UserSessionState {
        val userData = hashMapOf<String, Any?>()
        userData["uid"] = uid
        userData["google_uid"] = uid
        userData["auth_provider"] = authProvider
        name?.takeIf { it.isNotBlank() }?.let {
            userData["full_name"] = it
            userData["display_name"] = it
        }
        email?.takeIf { it.isNotBlank() }?.let { userData["email"] = it }
        phoneNumber?.takeIf { it.isNotBlank() }?.let { userData["phone_number"] = it }
        if (isVerified) {
            userData["isVerified"] = true
            userData["phone_verified"] = true
            if (authProvider == "email" || authProvider == "google") {
                userData["email_verified"] = true
            }
        }
        photoUrl?.takeIf { it.isNotBlank() }?.let {
            userData["photo_url"] = it
            userData["profile_photo_url"] = it
        }
        userData["updated_at"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
        userData["last_login_at"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

        return runCatching {
            saveToTheRivData(uid, name, email, phoneNumber)

            val docRef = firestore.collection("users").document(uid)
            val docSnap = docRef.get().await()
            val existingName = docSnap.getString("full_name") ?: docSnap.getString("display_name")
            val existingPhotoUrl = docSnap.getString("photo_url") ?: docSnap.getString("profile_photo_url")

            val onboardingCompleted = docSnap.getBoolean("onboarding_completed") ?: false
            val isNewUser = !docSnap.exists() || !onboardingCompleted

            if (!docSnap.exists()) {
                userData["created_at"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                userData["onboarding_completed"] = false
                userData["auto_message_tracking_enabled"] = false
                docRef.set(userData).await()
                UserSessionState(true, null, false, photoUrl)
            } else {
                // We use SetOptions.merge() so fields omitted from userData will not overwrite existing Firestore data.
                // If new valid data is provided (e.g. from a Google login), we DO want to update Firestore.
                // However, we only update if the new data is present, which is already handled above (e.g. name?.takeIf { it.isNotBlank() }?.let { ... }).
                // If a user logs in via phone, name is null, so it's not in userData, and SetOptions.merge() preserves the existing name.
                // We don't remove fields from userData here, so valid new data CAN update the profile.

                docRef.set(userData, SetOptions.merge()).await()
                
                // Synchronize with Turso Cloud SQLite backend
                try {
                    val syncReq = com.rivavafi.universal.data.network.SyncUserRequest(
                        uid = uid,
                        email = email,
                        phone = phoneNumber,
                        name = name,
                        isPremium = null
                    )
                    com.rivavafi.universal.data.network.RetrofitClient.apiService.syncUser(syncReq)
                    Log.d("AuthRepository", "Synced user to Turso DB for UID: $uid")
                } catch (syncEx: Exception) {
                    Log.w("AuthRepository", "Failed to sync user to Turso DB: $uid", syncEx)
                }

                UserSessionState(isNewUser, existingName, onboardingCompleted, existingPhotoUrl)
            }
        }.getOrElse { firestoreError ->
            Log.w("AuthRepository", "Firestore sync failed. Continuing authenticated session.", firestoreError)
            UserSessionState(false, null, true, null) // Default to treating them as returning if Firestore fails
        }
    }

    suspend fun sendVerificationEmail(email: String, uid: String): Boolean {
        return try {
            val response = authApiService.sendVerificationEmail(SendVerificationRequest(email, uid))
            if (response.isSuccessful) {
                true
            } else {
                Log.w("AuthRepository", "API verification failed with ${response.code()}. Falling back to Firebase Auth.")
                auth.currentUser?.sendEmailVerification()?.await()
                true
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to send verification email via API. Falling back to Firebase Auth.", e)
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                true
            } catch (fallbackEx: Exception) {
                Log.e("AuthRepository", "Firebase Auth fallback failed", fallbackEx)
                false
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Boolean {
        val cleanEmail = email.trim()
        // 1. Primary: Branded Rivava Template via verified domain support@rivava.in (Resend)
        try {
            val response = authApiService.forgotPassword(com.rivavafi.universal.domain.api.ForgotPasswordRequest(cleanEmail))
            if (response.isSuccessful) {
                Log.d("AuthRepository", "Password reset email dispatched successfully via verified domain support@rivava.in")
                return true
            } else {
                Log.w("AuthRepository", "Backend forgotPassword returned code ${response.code()}, trying Firebase Auth fallback...")
            }
        } catch (backendErr: Exception) {
            Log.w("AuthRepository", "Backend forgotPassword notice: ${backendErr.message}, trying Firebase Auth fallback...")
        }

        // 2. Secondary Fallback: Firebase Auth direct password reset email
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.d("AuthRepository", "Password reset email dispatched successfully via Firebase Auth fallback to $cleanEmail")
            true
        } catch (firebaseEx: Exception) {
            Log.e("AuthRepository", "Firebase Auth sendPasswordResetEmail fallback failed: ${firebaseEx.message}", firebaseEx)
            false
        }
    }

    suspend fun resetPasswordWithBackend(email: String, token: String, newPassword: String): Result<String> {
        return try {
            val response = authApiService.resetPassword(
                com.rivavafi.universal.domain.api.ResetPasswordRequest(
                    email = email,
                    token = token,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Log.d("AuthRepository", "Password successfully reset via backend")
                Result.success("Password reset successfully.")
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    JSONObject(errorBody ?: "").optString("message", "Failed to reset password.")
                } catch (e: Exception) {
                    "Failed to reset password."
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to reset password via backend API", e)
            Result.failure(e)
        }
    }

    suspend fun checkVerificationStatus(uid: String): Boolean {
        return try {
            val response = authApiService.checkVerification(uid)
            if (response.isSuccessful) {
                response.body()?.isVerified ?: false
            } else {
                Log.w("AuthRepository", "API check failed with ${response.code()}. Falling back to Firebase Auth.")
                auth.currentUser?.reload()?.await()
                auth.currentUser?.isEmailVerified == true
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to check verification status via API. Falling back to Firebase Auth.", e)
            try {
                auth.currentUser?.reload()?.await()
                auth.currentUser?.isEmailVerified == true
            } catch (fallbackEx: Exception) {
                Log.e("AuthRepository", "Firebase Auth fallback failed", fallbackEx)
                false
            }
        }
    }

    suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            !result.signInMethods.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to check if email exists", e)
            false
        }
    }

    private suspend fun saveToTheRivData(
        uid: String,
        name: String?,
        email: String?,
        phoneNumber: String?
    ) {
        try {
            val docRef = firestore.collection("therivdata").document(uid)
            val docSnap = docRef.get().await()

            if (docSnap.exists()) {
                val updateData = mutableMapOf<String, Any>()
                updateData["lastLoginAt"] = com.google.firebase.Timestamp.now()

                docRef.set(updateData, SetOptions.merge())
                    .addOnSuccessListener { Log.d("AuthRepository", "Successfully updated therivdata for user $uid") }
                    .addOnFailureListener { e -> Log.e("AuthRepository", "Failed to update therivdata", e) }
            } else {
                val newData = hashMapOf<String, Any?>()
                newData["email"] = email
                newData["phoneno"] = phoneNumber
                newData["username"] = name
                newData["createdAt"] = com.google.firebase.Timestamp.now()
                newData["premiumStatus"] = false
                newData["lastLoginAt"] = com.google.firebase.Timestamp.now()

                docRef.set(newData)
                    .addOnSuccessListener { Log.d("AuthRepository", "Successfully created therivdata document for user $uid") }
                    .addOnFailureListener { e -> Log.e("AuthRepository", "Failed to create therivdata document", e) }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in saveToTheRivData for user $uid", e)
        }
    }
}
