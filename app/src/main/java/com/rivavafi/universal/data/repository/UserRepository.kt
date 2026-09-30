package com.rivavafi.universal.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rivavafi.universal.data.model.User
import com.rivavafi.universal.utils.PrefsManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "UserRepository"

    suspend fun saveUserToFirestore(user: User) {
        try {
            val userData = hashMapOf<String, Any?>()
            user.name?.let {
                userData["name"] = it
                userData["display_name"] = it
            }
            user.email?.let { userData["email"] = it }
            user.photo?.let {
                userData["photo"] = it
                userData["profile_photo_url"] = it
            }
            user.phone?.let { userData["phone_number"] = it }
            userData["lastLogin"] = com.google.firebase.firestore.FieldValue.serverTimestamp()

            firestore.collection("users").document(user.uid)
                .set(userData, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully saved user to Firestore: ${user.uid}")

            // Synchronize with backend Turso DB
            try {
                val syncReq = com.rivavafi.universal.data.network.SyncUserRequest(
                    uid = user.uid,
                    email = user.email,
                    phone = user.phone,
                    name = user.name
                )
                com.rivavafi.universal.data.network.RetrofitClient.apiService.syncUser(syncReq)
                Log.d(TAG, "Successfully synced user to Turso DB: ${user.uid}")
            } catch (syncEx: Exception) {
                Log.w(TAG, "Failed to sync user to Turso DB: ${user.uid}", syncEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user to Firestore", e)
        }
    }

    fun cacheUserLocally(context: Context, user: User) {
        try {
            val prefsManager = PrefsManager(context)
            prefsManager.saveUser(user)
            Log.d(TAG, "Successfully cached user locally: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache user locally", e)
        }
    }

    fun getCachedUser(context: Context): User? {
        return try {
            val prefsManager = PrefsManager(context)
            val user = prefsManager.getUser()
            Log.d(TAG, "Retrieved cached user: ${user?.uid}")
            user
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached user", e)
            null
        }
    }

    suspend fun fetchUserFromFirestore(uid: String): User? {
        return try {
            val docSnap = firestore.collection("users").document(uid).get().await()
            if (docSnap.exists()) {
                val user = User(
                    uid = uid,
                    name = docSnap.getString("name") ?: docSnap.getString("display_name"),
                    email = docSnap.getString("email"),
                    photo = docSnap.getString("photo") ?: docSnap.getString("profile_photo_url"),
                    phone = docSnap.getString("phone_number")
                )
                Log.d(TAG, "Successfully fetched user from Firestore: $uid")
                user
            } else {
                Log.d(TAG, "User document does not exist in Firestore: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user from Firestore", e)
            null
        }
    }
}
