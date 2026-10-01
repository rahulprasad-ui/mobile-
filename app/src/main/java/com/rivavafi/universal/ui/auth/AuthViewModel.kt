package com.rivavafi.universal.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.data.repository.AuthRepository
import com.rivavafi.universal.data.repository.UserEntitlementRepository
import java.util.concurrent.TimeUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.rivavafi.universal.data.model.User
import com.rivavafi.universal.data.repository.UserRepository
import com.rivavafi.universal.data.repository.FirebaseUserManager
import com.rivavafi.universal.data.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthFormState(
    val emailError: String? = null,
    val phoneError: String? = null,
    val isFormValid: Boolean = false
)

enum class AuthState {
    IDLE,
    LOADING,
    SUCCESS,
    ERROR
}

enum class PhoneAuthState {
    IDLE,
    CODE_SENT,
    SUCCESS,
    ERROR
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userEntitlementRepository: UserEntitlementRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context,
    private val firebaseUserManager: FirebaseUserManager,
    private val emailService: com.rivavafi.universal.data.repository.EmailService
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.IDLE)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _phoneAuthState = MutableStateFlow(PhoneAuthState.IDLE)
    val phoneAuthState: StateFlow<PhoneAuthState> = _phoneAuthState.asStateFlow()




    private val _isNewUser = MutableStateFlow<Boolean?>(null)
    val isNewUser: StateFlow<Boolean?> = _isNewUser.asStateFlow()

    private val _authFormState = MutableStateFlow(AuthFormState())
    val authFormState: StateFlow<AuthFormState> = _authFormState.asStateFlow()

    private fun restoreUserSession(sessionState: com.rivavafi.universal.data.repository.AuthRepository.UserSessionState) {
        viewModelScope.launch {
            if (!sessionState.existingName.isNullOrBlank()) {
                userPreferencesRepository.saveUserName(sessionState.existingName)
            }
            if (!sessionState.photoUrl.isNullOrBlank()) {
                userPreferencesRepository.setProfileImageUri(sessionState.photoUrl)
            }
            if (sessionState.onboardingCompleted) {
                userPreferencesRepository.setOnboardingCompleted(true)
            }
            userEntitlementRepository.syncEntitlement()
            _authState.value = AuthState.SUCCESS
        }
    }

    init {
        viewModelScope.launch {
            val user = repository.auth.currentUser
            if (user != null) {
                // APP START LOGIC: If logged in, update cache and sync
                val appUser = User(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    photo = user.photoUrl?.toString(),
                    phone = user.phoneNumber
                )
                userRepository.cacheUserLocally(context, appUser)
                userRepository.saveUserToFirestore(appUser)

                try {
                    val docSnap = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("therivdata").document(user.uid).get().await()
                    _isNewUser.value = !docSnap.exists()
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Failed to check existing user data", e)
                    _isNewUser.value = false // Default to false if network fails to avoid blocking login flow
                }

                val providerId = user.providerData.firstOrNull()?.providerId
                userEntitlementRepository.syncEntitlement()
                _authState.value = AuthState.SUCCESS
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.IDLE
        _phoneAuthState.value = PhoneAuthState.IDLE
        _errorMessage.value = null
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
        _authState.value = AuthState.IDLE // Maintain IDLE state so UI fields remain accessible
    }

    fun getCachedUser(): User? {
        return userRepository.getCachedUser(context)
    }


    fun onGoogleSignInSuccess(idToken: String, name: String, email: String, photoUrl: String = "") {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                Log.d("AuthViewModel", "Successfully extracted ID Token, exchanging with Firebase...")
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = repository.auth.signInWithCredential(firebaseCredential).await()

                val uid = authResult.user?.uid ?: throw Exception("Failed to retrieve UID")

                val doc = firebaseUserManager.getCurrentUserData(uid)
                val phoneToSave = doc?.phone

                val sessionState = repository.saveUserToFirestore(
                    uid = uid,
                    name = name,
                    email = email,
                    phoneNumber = phoneToSave,
                    authProvider = "google",
                    isVerified = true,
                    photoUrl = photoUrl
                )

                val isNew = !sessionState.onboardingCompleted
                _isNewUser.value = isNew

                userPreferencesRepository.saveUserName(name)
                if (photoUrl.isNotBlank()) {
                    userPreferencesRepository.setProfileImageUri(photoUrl)
                }

                val appUser = User(
                    uid = uid,
                    name = name,
                    email = email,
                    photo = photoUrl.ifBlank { null },
                    phone = phoneToSave
                )
                userRepository.saveUserToFirestore(appUser)
                userRepository.cacheUserLocally(context, appUser)

                if (sessionState.onboardingCompleted) {
                    userPreferencesRepository.setOnboardingCompleted(true)
                }

                userEntitlementRepository.syncEntitlement()
                _authState.value = AuthState.SUCCESS

                // Send Welcome email if new user, or Security Login Alert if existing user
                try {
                    if (isNew) {
                        emailService.sendWelcomeEmail(email, name)
                    } else {
                        emailService.sendLoginAlert(email, name)
                    }
                } catch (emailErr: Exception) {
                    Log.w("AuthViewModel", "Failed to send auth email: ${emailErr.message}")
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-in failed", e)
                _errorMessage.value = e.message ?: "Login failed"
                _authState.value = AuthState.IDLE
            }
        }
    }


    fun onEmailLogin(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            Log.w("AuthViewModel", "Email login rejected: Email or password was blank")
            _errorMessage.value = "Email and Password cannot be empty."
            _authState.value = AuthState.IDLE
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                Log.d("AuthViewModel", "Attempting email login for: $email")
                val authRes = repository.auth.signInWithEmailAndPassword(email, pass).await()
                Log.d("AuthViewModel", "Email login successful")

                val uid = authRes.user?.uid ?: repository.auth.currentUser?.uid ?: throw Exception("Failed to retrieve UID")

                // SAVE TO THEDATA (handles both create if not exists and update lastLoginAt if exists)
                val theDataUser = UserModel(
                    uid = uid,
                    email = email,
                    loginProvider = "email",
                    isPhoneVerified = false
                )
                firebaseUserManager.saveUserToFirestore(theDataUser)

                val sessionState = repository.saveUserToFirestore(
                    uid = uid,
                    name = authRes.user?.displayName,
                    email = email,
                    phoneNumber = null,
                    authProvider = "email",
                    isVerified = true
                )
                val isNew = !sessionState.onboardingCompleted
                _isNewUser.value = isNew
                if (isNew) {
                    repository.auth.currentUser?.let { repository.sendUserToSheet(it, "Email") }
                } else {
                    if (!sessionState.existingName.isNullOrBlank()) {
                        userPreferencesRepository.saveUserName(sessionState.existingName)
                    }
                    if (!sessionState.photoUrl.isNullOrBlank()) {
                        userPreferencesRepository.setProfileImageUri(sessionState.photoUrl)
                    }
                }
                if (sessionState.onboardingCompleted) {
                    userPreferencesRepository.setOnboardingCompleted(true)
                }

                val appUser = User(
                    uid = uid,
                    name = sessionState.existingName ?: authRes.user?.displayName,
                    email = email,
                    photo = sessionState.photoUrl,
                    phone = null
                )
                userRepository.saveUserToFirestore(appUser)
                userRepository.cacheUserLocally(context, appUser)

                userEntitlementRepository.syncEntitlement()
                _authState.value = AuthState.SUCCESS

                // Send Welcome email if new user, or Security Login Alert if existing user
                try {
                    if (isNew) {
                        emailService.sendWelcomeEmail(email, sessionState.existingName ?: "")
                    } else {
                        emailService.sendLoginAlert(email, sessionState.existingName ?: "")
                    }
                } catch (emailErr: Exception) {
                    Log.w("AuthViewModel", "Failed to send auth email: ${emailErr.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email login failed", e)
                _errorMessage.value = e.message ?: "Login failed"
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun onForgotPassword(email: String, onSuccess: () -> Unit) {
        if (email.isBlank()) {
            _errorMessage.value = "Please enter your email to reset your password."
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val sent = repository.sendPasswordReset(email)
                if (sent) {
                    _errorMessage.value = "Reset link sent to your email"
                    _authState.value = AuthState.IDLE
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to send reset email. Please try again."
                    _authState.value = AuthState.IDLE
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Password reset failed", e)
                _errorMessage.value = e.message ?: "Failed to send reset email."
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun onEmailRegister(email: String, pass: String, name: String, onVerificationSent: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            Log.w("AuthViewModel", "Email register rejected: Email or password was blank")
            _errorMessage.value = "Email and Password cannot be empty."
            _authState.value = AuthState.IDLE
            return
        }
        if (pass.length < 6) {
            Log.w("AuthViewModel", "Email register rejected: Password too short")
            _errorMessage.value = "Password must be at least 6 characters."
            _authState.value = AuthState.IDLE
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            if (repository.checkEmailExists(email)) {
                _errorMessage.value = "Email is already registered. Please sign in."
                _authState.value = AuthState.IDLE
                return@launch
            }

            try {
                Log.d("AuthViewModel", "Attempting email registration for: $email")
                val result = repository.auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw Exception("Failed to retrieve UID")

                Log.d("AuthViewModel", "Registration successful. Saving to Firestore and syncing session...")

                // SAVE TO THEDATA
                val theDataUser = UserModel(
                    uid = uid,
                    name = name.ifBlank { null },
                    email = email,
                    loginProvider = "email",
                    isPhoneVerified = false
                )
                firebaseUserManager.saveUserToFirestore(theDataUser)

                val sessionState = repository.saveUserToFirestore(
                    uid = uid,
                    name = name.ifBlank { null },
                    email = email,
                    phoneNumber = null,
                    authProvider = "email",
                    isVerified = true
                )
                userPreferencesRepository.saveUserName(name)

                val appUser = User(
                    uid = uid,
                    name = name.ifBlank { null },
                    email = email,
                    photo = null,
                    phone = null
                )
                userRepository.saveUserToFirestore(appUser)
                userRepository.cacheUserLocally(context, appUser)

                _isNewUser.value = true
                userEntitlementRepository.syncEntitlement()
                _authState.value = AuthState.SUCCESS

                try {
                    emailService.sendWelcomeEmail(email, name)
                } catch (emailErr: Exception) {
                    Log.w("AuthViewModel", "Welcome email failed: ${emailErr.message}")
                }
                try {
                    repository.sendVerificationEmail(email, uid)
                } catch (e: Exception) {}

                onVerificationSent()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Email registration failed", e)
                _errorMessage.value = e.message ?: "Registration failed"
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun verifyEmailActionCode(oobCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                repository.auth.applyActionCode(oobCode).await()
                _authState.value = AuthState.IDLE
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to verify email with oobCode", e)
                _errorMessage.value = "Failed to verify email: ${e.message}"
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun resetPasswordWithActionCode(oobCode: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                repository.auth.confirmPasswordReset(oobCode, newPassword).await()
                _authState.value = AuthState.IDLE
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to reset password with oobCode", e)
                _errorMessage.value = "Failed to reset password: ${e.message}"
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun resetPasswordWithCustomToken(email: String, token: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            val result = repository.resetPasswordWithBackend(email, token, newPassword)
            if (result.isSuccess) {
                _authState.value = AuthState.IDLE
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to reset password."
                _errorMessage.value = errorMsg
                _authState.value = AuthState.IDLE
            }
        }
    }

    fun checkEmailVerified(onVerified: () -> Unit, onNotVerified: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val uid = repository.auth.currentUser?.uid
                if (uid != null) {
                    val isVerified = repository.checkVerificationStatus(uid)
                    if (isVerified) {

                        val sessionState = repository.saveUserToFirestore(
                            uid = uid,
                            name = null,
                            email = repository.auth.currentUser?.email ?: "",
                            phoneNumber = null,
                            authProvider = "email",
                            isVerified = true
                        )
                        val firebaseNew = false
                        val isNew = !sessionState.onboardingCompleted
                        _isNewUser.value = isNew
                        if (isNew) {
                            repository.auth.currentUser?.let { repository.sendUserToSheet(it, "Email") }
                        } else {
                            if (!sessionState.existingName.isNullOrBlank()) {
                                userPreferencesRepository.saveUserName(sessionState.existingName)
                            }
                            if (!sessionState.photoUrl.isNullOrBlank()) {
                                userPreferencesRepository.setProfileImageUri(sessionState.photoUrl)
                            }
                        }
                        if (sessionState.onboardingCompleted) {
                            userPreferencesRepository.setOnboardingCompleted(true)
                        }

                        userEntitlementRepository.syncEntitlement()
                        _authState.value = AuthState.SUCCESS
                        onVerified()
                    } else {
                        _errorMessage.value = "Email not verified yet. Please check your inbox."
                        _authState.value = AuthState.IDLE
                        onNotVerified()
                    }
                } else {
                    _errorMessage.value = "Failed to get user. Please try logging in again."
                    _authState.value = AuthState.IDLE
                    onNotVerified()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to check email verification", e)
                _errorMessage.value = "Failed to verify email status: ${e.message}"
                _authState.value = AuthState.IDLE
                onNotVerified()
            }
        }
    }

    private var storedVerificationId: String? = null
    private var storedResendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun startPhoneVerification(
        activity: Activity?,
        phoneNumber: String,
        onCodeSentCallback: (String) -> Unit
    ) {
        if (phoneNumber.isBlank()) {
            _errorMessage.value = "Invalid phone number"
            return
        }

        if (activity == null) {
            // Fallback to backend API if activity is not available
            _authState.value = AuthState.LOADING
            viewModelScope.launch {
                try {
                    val result = repository.sendOtp(phoneNumber)
                    if (result.isSuccess) {
                        _phoneAuthState.value = PhoneAuthState.CODE_SENT
                        _authState.value = AuthState.IDLE
                        onCodeSentCallback(phoneNumber)
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Failed to send OTP"
                        _errorMessage.value = errorMsg
                        _phoneAuthState.value = PhoneAuthState.ERROR
                        _authState.value = AuthState.IDLE
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Unable to send OTP. Please try again."
                    _phoneAuthState.value = PhoneAuthState.ERROR
                    _authState.value = AuthState.IDLE
                }
            }
            return
        }

        _authState.value = AuthState.LOADING
        Log.d("AuthViewModel", "Initiating Firebase Phone Auth OTP request for: $phoneNumber")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("AuthViewModel", "Firebase instant phone verification completed")
                signInWithPhoneCredential(credential, phoneNumber, null, onSuccess = {}, onError = {})
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthViewModel", "Firebase Phone Verification failed: ${e.message}", e)
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number format. Please check the number."
                    is FirebaseTooManyRequestsException ->
                        "Too many OTP requests. Please wait a few minutes."
                    else -> e.localizedMessage ?: "Failed to send OTP via Firebase"
                }
                _errorMessage.value = msg
                _phoneAuthState.value = PhoneAuthState.ERROR
                _authState.value = AuthState.IDLE
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("AuthViewModel", "Firebase OTP code sent successfully to $phoneNumber")
                storedVerificationId = verificationId
                storedResendToken = token
                _phoneAuthState.value = PhoneAuthState.CODE_SENT
                _authState.value = AuthState.IDLE
                onCodeSentCallback(phoneNumber)
            }
        }

        val options = PhoneAuthOptions.newBuilder(repository.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendOtp(
        activity: Activity?,
        phoneNumber: String,
        onCodeSentCallback: (String) -> Unit
    ) {
        if (phoneNumber.isBlank()) {
            _errorMessage.value = "Invalid phone number"
            return
        }

        if (activity == null) {
            startPhoneVerification(null, phoneNumber, onCodeSentCallback)
            return
        }

        _authState.value = AuthState.LOADING
        Log.d("AuthViewModel", "Resending Firebase Phone Auth OTP for: $phoneNumber")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("AuthViewModel", "Firebase instant phone verification completed on resend")
                signInWithPhoneCredential(credential, phoneNumber, null, onSuccess = {}, onError = {})
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthViewModel", "Firebase Phone resend failed: ${e.message}", e)
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number format."
                    is FirebaseTooManyRequestsException ->
                        "Too many OTP requests. Please wait a few minutes."
                    else -> e.localizedMessage ?: "Failed to resend OTP"
                }
                _errorMessage.value = msg
                _phoneAuthState.value = PhoneAuthState.ERROR
                _authState.value = AuthState.IDLE
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("AuthViewModel", "Firebase OTP code resent to $phoneNumber")
                storedVerificationId = verificationId
                storedResendToken = token
                _phoneAuthState.value = PhoneAuthState.CODE_SENT
                _authState.value = AuthState.IDLE
                onCodeSentCallback(phoneNumber)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(repository.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        storedResendToken?.let { builder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun normalizePhoneNumber(input: String): String? {
        val digits = input.replace(Regex("\\D"), "")
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length > 10 -> "+$digits"
            else -> null
        }
    }

    fun validatePhone(phone: String) {
        val e164Regex = Regex("^\\+[1-9]\\d{1,14}$")
        if (phone.isNotEmpty() && !e164Regex.matches(phone)) {
            _authFormState.value = _authFormState.value.copy(phoneError = "Invalid E.164 phone format (e.g. +14155552671)")
        } else {
            _authFormState.value = _authFormState.value.copy(phoneError = null)
        }
        updateFormValidity()
    }

    fun validateEmail(email: String) {
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authFormState.value = _authFormState.value.copy(emailError = "Invalid email address")
        } else {
            _authFormState.value = _authFormState.value.copy(emailError = null)
        }
        updateFormValidity()
    }

    private fun updateFormValidity() {
        val state = _authFormState.value
        _authFormState.value = state.copy(isFormValid = state.emailError == null && state.phoneError == null)
    }

    fun verifyOtp(otp: String, phoneNumber: String, email: String?, onSuccess: () -> Unit, onError: () -> Unit) {
        if (phoneNumber.isBlank() || otp.isBlank()) {
            _errorMessage.value = "Invalid OTP or missing phone number."
            onError()
            return
        }

        val verId = storedVerificationId
        if (verId != null) {
            // Verify via Firebase PhoneAuthCredential
            _authState.value = AuthState.LOADING
            try {
                val credential = PhoneAuthProvider.getCredential(verId, otp)
                signInWithPhoneCredential(credential, phoneNumber, email, onSuccess, onError)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to construct PhoneAuthCredential", e)
                _errorMessage.value = e.message ?: "Invalid OTP"
                _phoneAuthState.value = PhoneAuthState.ERROR
                _authState.value = AuthState.IDLE
                onError()
            }
        } else {
            // Fallback to backend verification
            _authState.value = AuthState.LOADING
            viewModelScope.launch {
                try {
                    Log.d("AuthViewModel", "Starting backend OTP verification for $phoneNumber")
                    val result = repository.verifyOtpAndSignIn(phoneNumber, otp)
                    if (result.isSuccess) {
                        val uid = result.getOrNull() ?: throw Exception("Failed to retrieve UID")
                        completePhoneSignIn(uid, phoneNumber, email, onSuccess)
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Verification failed"
                        _errorMessage.value = errorMsg
                        _phoneAuthState.value = PhoneAuthState.ERROR
                        _authState.value = AuthState.IDLE
                        onError()
                    }
                } catch (e: Exception) {
                    _errorMessage.value = e.message ?: "Invalid OTP"
                    _phoneAuthState.value = PhoneAuthState.ERROR
                    _authState.value = AuthState.IDLE
                    onError()
                }
            }
        }
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        phoneNumber: String,
        email: String?,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Signing in with Firebase Phone Credential for $phoneNumber")
                val result = repository.signInWithPhoneCredential(credential)
                if (result.isSuccess) {
                    val uid = result.getOrNull() ?: throw Exception("Failed to retrieve UID")
                    completePhoneSignIn(uid, phoneNumber, email, onSuccess)
                } else {
                    val ex = result.exceptionOrNull()
                    Log.e("AuthViewModel", "Phone Credential Sign-in failed: ${ex?.message}", ex)
                    _errorMessage.value = ex?.localizedMessage ?: "Invalid OTP verification code"
                    _phoneAuthState.value = PhoneAuthState.ERROR
                    _authState.value = AuthState.IDLE
                    onError()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Phone Credential unexpected error: ${e.message}", e)
                _errorMessage.value = e.localizedMessage ?: "Invalid OTP verification code"
                _phoneAuthState.value = PhoneAuthState.ERROR
                _authState.value = AuthState.IDLE
                onError()
            }
        }
    }

    private suspend fun completePhoneSignIn(
        uid: String,
        phoneNumber: String,
        email: String?,
        onSuccess: () -> Unit
    ) {
        Log.d("AuthViewModel", "Completing Phone Sign-in for UID: $uid")

        // SAVE TO THEDATA
        val theDataUser = UserModel(
            uid = uid,
            email = email,
            phone = phoneNumber,
            phoneno = phoneNumber,
            loginProvider = "phone",
            isPhoneVerified = true
        )
        firebaseUserManager.saveUserToFirestore(theDataUser)

        val sessionState = repository.saveUserToFirestore(
            uid = uid,
            name = null,
            email = email,
            phoneNumber = phoneNumber,
            authProvider = "phone",
            isVerified = true
        )

        val isNew = !sessionState.onboardingCompleted
        _isNewUser.value = isNew
        if (phoneNumber.isNotBlank()) {
            userPreferencesRepository.saveUserPhone(phoneNumber)
        }
        if (isNew) {
            repository.auth.currentUser?.let { repository.sendUserToSheet(it, "Phone") }
        } else {
            if (!sessionState.existingName.isNullOrBlank()) {
                userPreferencesRepository.saveUserName(sessionState.existingName)
            }
            if (!sessionState.photoUrl.isNullOrBlank()) {
                userPreferencesRepository.setProfileImageUri(sessionState.photoUrl)
            }
        }
        if (sessionState.onboardingCompleted) {
            userPreferencesRepository.setOnboardingCompleted(true)
        }

        Log.d("AuthViewModel", "User session saved. Syncing entitlements...")
        _phoneAuthState.value = PhoneAuthState.SUCCESS
        userEntitlementRepository.syncEntitlement()
        _authState.value = AuthState.SUCCESS

        // Send Welcome email or Login Alert if email exists
        if (!email.isNullOrBlank()) {
            try {
                if (isNew) {
                    emailService.sendWelcomeEmail(email, sessionState.existingName ?: "")
                } else {
                    emailService.sendLoginAlert(email, sessionState.existingName ?: "")
                }
            } catch (emailErr: Exception) {
                Log.w("AuthViewModel", "Failed to send auth email for phone login: ${emailErr.message}")
            }
        }

        Log.d("AuthViewModel", "Sign-in complete. Executing success callback.")
        onSuccess()
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING
            try {
                val user = repository.auth.currentUser
                if (user != null && user.email != null) {
                    repository.sendVerificationEmail(user.email!!, user.uid)
                    _errorMessage.value = "Verification email resent successfully."
                } else {
                    _errorMessage.value = "User not found. Please log in again."
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to resend verification email", e)
                _errorMessage.value = "Failed to resend verification email."
            } finally {
                _authState.value = AuthState.IDLE
            }
        }
    }
}
