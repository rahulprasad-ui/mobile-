package com.rivavafi.universal.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.rivavafi.universal.data.model.UserModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val userModel: UserModel? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val TAG = "ProfileViewModel"
    private val COLLECTION_NAME = "therivdata"

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        startListeningToProfile()
    }

    private fun startListeningToProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            _profileState.value = ProfileState(isLoading = false, error = "User not logged in")
            Log.e(TAG, "No logged in user found.")
            return
        }

        Log.d(TAG, "Starting to listen for profile updates for UID: $uid")

        listenerRegistration = firestore.collection(COLLECTION_NAME).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed.", error)
                    _profileState.value = _profileState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: ${snapshot.data}")

                    val createdAtAny = snapshot.get("createdAt")
                    val createdAtVal = if (createdAtAny is com.google.firebase.Timestamp) createdAtAny.toDate().time else snapshot.getLong("createdAt") ?: System.currentTimeMillis()

                    val lastLoginAtAny = snapshot.get("lastLoginAt")
                    val lastLoginAtVal = if (lastLoginAtAny is com.google.firebase.Timestamp) lastLoginAtAny.toDate().time else snapshot.getLong("lastLoginAt") ?: System.currentTimeMillis()

                    val user = UserModel(
                        uid = snapshot.getString("uid") ?: uid,
                        name = snapshot.getString("name"),
                        username = snapshot.getString("username"),
                        email = snapshot.getString("email"),
                        phone = snapshot.getString("phone") ?: snapshot.getString("phoneno") ?: snapshot.getString("phoneNumber"),
                        phoneno = snapshot.getString("phoneno") ?: snapshot.getString("phone"),
                        preference = snapshot.getString("preference"),
                        profileImage = snapshot.getString("profileImage"),
                        createdAt = createdAtVal,
                        lastLoginAt = lastLoginAtVal,
                        loginProvider = snapshot.getString("loginProvider") ?: "",
                        isPhoneVerified = snapshot.getBoolean("isPhoneVerified") ?: false,
                        premiumStatus = snapshot.getBoolean("premiumStatus") ?: false
                    )
                    _profileState.value = ProfileState(isLoading = false, userModel = user)
                } else {
                    Log.d(TAG, "Current data: null")
                    _profileState.value = ProfileState(isLoading = false, error = "Profile not found")
                }
            }
    }

    fun updateProfileField(field: String, value: Any) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.e(TAG, "Cannot update profile, uid is null")
            return
        }

        // Restrict editable fields to only allowed fields
        val allowedFields = setOf("name", "username", "phone", "phoneno", "phoneNumber", "profileImage")
        if (field !in allowedFields) {
            Log.e(TAG, "Attempt to update restricted field: $field")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating profile field $field for UID: $uid")
                firestore.collection(COLLECTION_NAME).document(uid)
                    .set(mapOf(field to value), SetOptions.merge())
                Log.d(TAG, "Successfully updated $field")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update $field", e)
            }
        }
    }

    fun updateName(name: String) {
        updateProfileField("name", name)
    }

    fun updateProfileImage(profileImage: String) {
        updateProfileField("profileImage", profileImage)
    }

    fun updateUsername(username: String) {
        updateProfileField("username", username)
    }

    fun updatePhone(phone: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) return

        viewModelScope.launch {
            try {
                firestore.collection(COLLECTION_NAME).document(uid)
                    .set(mapOf("phone" to phone, "phoneno" to phone, "phoneNumber" to phone), SetOptions.merge())
                Log.d(TAG, "Successfully updated phone numbers")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update phone", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        Log.d(TAG, "Removed Firestore listener on clear")
    }
}
