package com.rivavafi.universal.ui.settings

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.domain.repository.TransactionRepository
import com.rivavafi.universal.domain.usecase.ExportCsvUseCase
import com.rivavafi.universal.domain.usecase.ImportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.rivavafi.universal.sms.SmsInboxScanner
import com.rivavafi.universal.sms.SmsTrackingMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val exportCsvUseCase: ExportCsvUseCase,

    private val importCsvUseCase: ImportCsvUseCase,
    private val smsInboxScanner: SmsInboxScanner,
    private val userCorrectionDao: com.rivavafi.universal.data.local.UserCorrectionDao,

    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoading: kotlinx.coroutines.flow.StateFlow<Boolean> = _isLoading.asStateFlow()

    val homeLayoutPreset = preferencesRepository.homeLayoutPresetFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Daily Tracker"
    )

    val showSmsDetails = preferencesRepository.showSmsDetailsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val terminologyMode = preferencesRepository.terminologyModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "CREDIT_DEBIT"
    )

    fun toggleTerminology() {
        viewModelScope.launch {
            val current = terminologyMode.value
            preferencesRepository.setTerminologyMode(if (current == "CREDIT_DEBIT") "INCOME_EXPENSE" else "CREDIT_DEBIT")
        }
    }

    private val _banksDetected = MutableStateFlow<List<String>>(emptyList())
    val banksDetected: StateFlow<List<String>> = _banksDetected

    init {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            transactionRepository.getAllTransactions(userId).collectLatest { transactions ->
                _banksDetected.value = transactions.mapNotNull { it.bankName }.distinct().sorted()
            }
        }
    }


    val smsTrackingMode = preferencesRepository.smsTrackingModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "OFF"
    )

    fun setSmsTrackingMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setSmsTrackingMode(mode)
        }
    }

    fun rescanSms(context: Context) {
        viewModelScope.launch {
            val modeStr = smsTrackingMode.value
            val mode = runCatching { SmsTrackingMode.valueOf(modeStr) }.getOrDefault(SmsTrackingMode.OFF)
            if (mode != SmsTrackingMode.OFF) {
                try {
                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                    _isLoading.value = true
                    smsInboxScanner.scanInbox(context, mode, userId)
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun setHomeLayoutPreset(preset: String) {
        viewModelScope.launch {
            preferencesRepository.setHomeLayoutPreset(preset)
        }
    }

    fun setShowSmsDetails(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowSmsDetails(show)
        }
    }

    fun exportCsv(context: Context, uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = exportCsvUseCase(context, uri)
            onResult(result)
        }
    }

    fun importCsv(context: Context, uri: Uri, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = importCsvUseCase(context, uri)
            onResult(result)
        }
    }

    fun logout() {
        viewModelScope.launch {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, googleSignInOptions).signOut()
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
            // Keep locally cached profile/onboarding data so returning users
            // don't lose their displayed name after sign out.
        }
    }

    fun clearTransactionHistory() {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            transactionRepository.deleteAllTransactions(userId)
        }
    }

    fun clearAiLearning() {
        viewModelScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            userCorrectionDao.clearAllCorrections(userId)
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("therivdata").document(uid).delete().await()
                    db.collection("therivavadata").document(uid).delete().await()

                    transactionRepository.deleteAllTransactions(uid)
                    userCorrectionDao.clearAllCorrections(uid)

                    auth.currentUser?.delete()?.await()

                } catch (e: Exception) {
                    // Ignore errors to ensure logout is attempted at least
                }
            }
            logout()
            onComplete()
        }
    }
}
