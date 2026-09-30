package com.rivavafi.universal.ui.onboarding

import android.util.Log
import androidx.lifecycle.SavedStateHandle

import com.rivavafi.universal.sms.SmsInboxScanner
import com.rivavafi.universal.sms.SmsTrackingMode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.Context
import com.rivavafi.universal.data.repository.EmailService
import com.rivavafi.universal.utils.PrefsManager

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val emailService: EmailService,
    private val smsInboxScanner: com.rivavafi.universal.sms.SmsInboxScanner,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    val userName = preferencesRepository.userNameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastPhoneNumber: String? = null


    fun setErrorMessage(msg: String) {
        _errorMessage.value = msg
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveName(name: String) {
        val cleanedName = name.trim()
        if (cleanedName.isBlank()) return

        viewModelScope.launch {
            val generatedUsername = generateUsername(cleanedName)
            preferencesRepository.setUserName(cleanedName)
            preferencesRepository.saveUsername(generatedUsername)
            PrefsManager(context).updateName(cleanedName)
            PrefsManager(context).updateUsername(generatedUsername)

            FirebaseAuth.getInstance().currentUser?.let { user ->
                val uid = user.uid
                firestore.collection("users").document(uid)
                    .set(
                        mapOf(
                            "full_name" to cleanedName,
                            "display_name" to cleanedName,
                            "username" to generatedUsername
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )

                authRepository.sendUserToSheet(user, "Google", cleanedName)
            }
        }
    }


    fun savePhoneAndCompleteOnboarding(phoneNumber: String, onSuccess: () -> Unit) {
        lastPhoneNumber = phoneNumber
        _isLoading.value = true
        saveUserToFirestoreAndComplete(onSuccess)
    }

    private fun saveUserToFirestoreAndComplete(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _errorMessage.value = "User not found"
                    _isLoading.value = false
                    return@launch
                }
                val uid = user.uid
                val email = user.email ?: ""
                val photoUrl = user.photoUrl?.toString() ?: ""
                val name = preferencesRepository.getUserName() ?: ""
                val phone = lastPhoneNumber ?: ""

                val generatedUsername = generateUsername(name)
                val userData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "username" to generatedUsername,
                    "email" to email,
                    "phone" to phone,
                    "phoneNumber" to phone,
                    "phoneno" to phone,
                    "preference" to "No",
                    "premiumStatus" to false,
                    "profileImage" to photoUrl,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastLoginAt" to com.google.firebase.Timestamp.now(),
                    "authProvider" to "google",
                    "loginProvider" to "google",
                    "isPhoneVerified" to true
                )

                firestore.collection("therivavadata").document(uid)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())

                // Sync to backend Turso database
                try {
                    val syncReq = com.rivavafi.universal.data.network.SyncUserRequest(
                        uid = uid,
                        email = email.ifBlank { null },
                        phone = phone.ifBlank { null },
                        name = name.ifBlank { null }
                    )
                    com.rivavafi.universal.data.network.RetrofitClient.apiService.syncUser(syncReq)
                    Log.d("OnboardingViewModel", "Synced user onboarding details to Turso DB: $uid")
                } catch (syncEx: Exception) {
                    Log.w("OnboardingViewModel", "Failed to sync user to Turso DB: $uid", syncEx)
                }

                // Also save to the newly created therivdata collection
                val theRivDocRef = firestore.collection("therivdata").document(uid)
                theRivDocRef.get().addOnSuccessListener { docSnap ->
                    if (docSnap.exists()) {
                        val updateData = mutableMapOf<String, Any>()
                        updateData["lastLoginAt"] = com.google.firebase.Timestamp.now()
                        if (phone.isNotBlank() && docSnap.getString("phoneno") != phone) {
                            updateData["phoneno"] = phone
                        }
                        if (name.isNotBlank() && docSnap.getString("username") != generatedUsername) {
                            updateData["username"] = generatedUsername
                        }
                        if (name.isNotBlank() && docSnap.getString("name") != name) {
                            updateData["name"] = name
                        }
                        theRivDocRef.set(updateData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModelScope.launch {
                                        _isLoading.value = false
                                        onSuccess()
                                    }
                                } else {
                                    _errorMessage.value = task.exception?.message ?: "Failed to save data"
                                    _isLoading.value = false
                                }
                            }
                    } else {
                        val theRivData = hashMapOf<String, Any?>()
                        theRivData["email"] = email
                        theRivData["phoneno"] = phone
                        theRivData["phone"] = phone
                        theRivData["phoneNumber"] = phone
                        theRivData["name"] = name
                        theRivData["username"] = generatedUsername
                        theRivData["createdAt"] = com.google.firebase.Timestamp.now()
                        theRivData["premiumStatus"] = false
                        theRivData["lastLoginAt"] = com.google.firebase.Timestamp.now()

                        theRivDocRef.set(theRivData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModelScope.launch {
                                        _isLoading.value = false
                                        onSuccess()
                                    }
                                } else {
                                    _errorMessage.value = task.exception?.message ?: "Failed to save data"
                                    _isLoading.value = false
                                }
                            }
                    }
                }.addOnFailureListener {
                    _errorMessage.value = it.message ?: "Failed to check existing data"
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to complete onboarding"
                _isLoading.value = false
            }
        }
    }

    fun savePhoneNumber(phoneNumber: String, onSuccess: () -> Unit) {
        val cleanedPhoneNumber = phoneNumber.trim()
        if (cleanedPhoneNumber.isBlank()) {
            _errorMessage.value = "Invalid phone number"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            preferencesRepository.setUserPhone(cleanedPhoneNumber)
            PrefsManager(context).updatePhone(cleanedPhoneNumber)

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _isLoading.value = false
                onSuccess()
                return@launch
            }

            firestore.collection("users").document(user.uid)
                .set(mapOf(
                    "phone_number" to cleanedPhoneNumber
                ), com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e("OnboardingViewModel", "Failed to save phone number", task.exception)
                    }
                    _isLoading.value = false
                    onSuccess()
                }
        }
    }

    private fun generateUsername(name: String): String {
        return name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "rivava_user" }
    }


    fun setSmsTrackingMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setSmsTrackingMode(mode)
        }
    }

    fun scanSmsInbox(context: Context, mode: SmsTrackingMode, onSuccess: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    onSuccess()
                    return@launch
                }
                smsInboxScanner.scanInbox(context, mode, userId)
                preferencesRepository.setSmsScanCompleted(true)
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSmsTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSmsTrackingEnabled(enabled)
            FirebaseAuth.getInstance().currentUser?.let { user ->
                val uid = user.uid
                firestore.collection("users").document(uid)
                    .set(mapOf("auto_message_tracking_enabled" to enabled), com.google.firebase.firestore.SetOptions.merge())
            }
        }
    }


    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            FirebaseAuth.getInstance().currentUser?.let { user ->
                val uid = user.uid

                val docSnap = firestore.collection("users").document(uid).get().await()
                val alreadyCompleted = docSnap.getBoolean("onboarding_completed") ?: false

                if (!alreadyCompleted) {
                    firestore.collection("users").document(uid)
                        .set(mapOf("onboarding_completed" to true), com.google.firebase.firestore.SetOptions.merge())

                    user.email?.let { email ->
                        val name = preferencesRepository.getUserName() ?: ""
                        emailService.sendWelcomeEmail(email, name)
                    }
                }
            }
        }
    }
}
