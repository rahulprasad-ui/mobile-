package com.rivavafi.universal.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rivavafi.universal.data.model.UserModel
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserManager @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseUserManager"
    private val COLLECTION_NAME = "therivdata"


    suspend fun migrateLegacyDataIfNeeded(uid: String) {
        try {
            val newDoc = firestore.collection(COLLECTION_NAME).document(uid).get().await()
            if (!newDoc.exists()) {
                val legacyDoc = firestore.collection("THEDATA").document(uid).get().await()
                if (legacyDoc.exists()) {
                    val data = legacyDoc.data
                    if (data != null) {
                        firestore.collection(COLLECTION_NAME).document(uid).set(data).await()
                        Log.d(TAG, "Successfully migrated legacy THEDATA for UID: $uid to $COLLECTION_NAME")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during legacy data migration for UID: $uid", e)
        }
    }

    suspend fun saveUserToFirestore(userModel: UserModel) {
        migrateLegacyDataIfNeeded(userModel.uid)
        if (userModel.uid.isBlank()) {
            Log.e(TAG, "Cannot save user, uid is blank")
            return
        }
        try {
            val userMap = hashMapOf<String, Any?>()
            userMap["uid"] = userModel.uid
            userModel.name?.let { userMap["name"] = it }
            userModel.username?.let { userMap["username"] = it }
            userModel.email?.let { userMap["email"] = it }
            userModel.phone?.let { userMap["phone"] = it }
            userModel.phoneno?.let { userMap["phoneno"] = it }
            userModel.preference?.let { userMap["preference"] = it }
            userModel.profileImage?.let { userMap["profileImage"] = it }
            userMap["createdAt"] = userModel.createdAt
            userMap["lastLoginAt"] = userModel.lastLoginAt
            userMap["loginProvider"] = userModel.loginProvider
            userMap["isPhoneVerified"] = userModel.isPhoneVerified
            userMap["premiumStatus"] = userModel.premiumStatus

            val exists = checkIfUserExists(userModel.uid)

            if (!exists) {
                firestore.collection(COLLECTION_NAME).document(userModel.uid)
                    .set(userMap)
                    .await()
                Log.d(TAG, "THEDATA document created for UID: ${userModel.uid}")
            } else {
                // Remove createdAt and isPhoneVerified to avoid overwriting existing data
                userMap.remove("createdAt")
                // Only update isPhoneVerified if it's true, don't overwrite true with false
                if (!userModel.isPhoneVerified) {
                    userMap.remove("isPhoneVerified")
                }
                updateLastLogin(userModel.uid)
                firestore.collection(COLLECTION_NAME).document(userModel.uid)
                    .set(userMap, SetOptions.merge())
                    .await()
                Log.d(TAG, "Existing user updated in THEDATA for UID: ${userModel.uid}")
            }

            // Also synchronize user profile with Turso Cloud SQLite backend
            try {
                val syncRequest = com.rivavafi.universal.data.network.SyncUserRequest(
                    uid = userModel.uid,
                    email = userModel.email,
                    phone = userModel.phone ?: userModel.phoneno,
                    name = userModel.name ?: userModel.username,
                    isPremium = userModel.premiumStatus
                )
                com.rivavafi.universal.data.network.RetrofitClient.apiService.syncUser(syncRequest)
                Log.d(TAG, "User successfully synced to backend Turso DB for UID: ${userModel.uid}")
            } catch (syncEx: Exception) {
                Log.w(TAG, "Backend Turso DB sync failed for UID: ${userModel.uid}", syncEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore save failed in THEDATA for UID: ${userModel.uid}", e)
        }
    }

    suspend fun updateLastLogin(uid: String) {
        migrateLegacyDataIfNeeded(uid)
        if (uid.isBlank()) return
        try {
            val updateMap = mapOf("lastLoginAt" to System.currentTimeMillis())
            firestore.collection(COLLECTION_NAME).document(uid)
                .update(updateMap)
                .await()
            Log.d(TAG, "Successfully updated lastLoginAt for UID: $uid in THEDATA")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update lastLoginAt for UID: $uid in THEDATA", e)
        }
    }

    suspend fun getCurrentUserData(uid: String): UserModel? {
        migrateLegacyDataIfNeeded(uid)
        if (uid.isBlank()) return null
        return try {
            val document = firestore.collection(COLLECTION_NAME).document(uid).get().await()
            if (document.exists()) {
                val user = UserModel(
                    uid = document.getString("uid") ?: uid,
                    name = document.getString("name"),
                    username = document.getString("username"),
                    email = document.getString("email"),
                    phone = document.getString("phone"),
                    phoneno = document.getString("phoneno"),
                    preference = document.getString("preference"),
                    profileImage = document.getString("profileImage"),
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                    lastLoginAt = document.getLong("lastLoginAt") ?: System.currentTimeMillis(),
                    loginProvider = document.getString("loginProvider") ?: "",
                    isPhoneVerified = document.getBoolean("isPhoneVerified") ?: false,
                    premiumStatus = document.getBoolean("premiumStatus") ?: false
                )
                Log.d(TAG, "Successfully fetched user from THEDATA for UID: $uid")
                user
            } else {
                Log.d(TAG, "User document does not exist in THEDATA for UID: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user data from THEDATA for UID: $uid", e)
            null
        }
    }

    suspend fun checkIfUserExists(uid: String): Boolean {
        migrateLegacyDataIfNeeded(uid)
        if (uid.isBlank()) return false
        return try {
            val document = firestore.collection(COLLECTION_NAME).document(uid).get().await()
            document.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if user exists in THEDATA for UID: $uid", e)
            false
        }
    }
}
