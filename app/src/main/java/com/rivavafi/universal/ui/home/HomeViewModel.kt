package com.rivavafi.universal.ui.home

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.lifecycle.viewModelScope
import com.rivavafi.universal.data.local.TransactionEntity
import com.rivavafi.universal.domain.usecase.AddTransactionUseCase
import com.rivavafi.universal.domain.usecase.FinancialSummaryState
import com.rivavafi.universal.domain.usecase.GetFinancialSummaryUseCase
import com.rivavafi.universal.data.local.CategoryEntity
import com.rivavafi.universal.domain.usecase.AddCategoryUseCase
import com.rivavafi.universal.data.preferences.UserPreferencesRepository
import com.rivavafi.universal.domain.usecase.GetCategoriesUseCase
import com.rivavafi.universal.domain.usecase.GetTransactionsUseCase
import com.rivavafi.universal.data.repository.UserEntitlementRepository
import com.rivavafi.universal.data.repository.EliteRepository
import com.rivavafi.universal.data.repository.EliteConfig
import com.rivavafi.universal.data.repository.EliteSubscription
import com.rivavafi.universal.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getFinancialSummaryUseCase: GetFinancialSummaryUseCase,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userEntitlementRepository: UserEntitlementRepository,
    private val eliteRepository: EliteRepository,
    private val transactionRepository: com.rivavafi.universal.domain.repository.TransactionRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _dailyBudget = MutableStateFlow(800.0)
    val dailyBudget: StateFlow<Double> = _dailyBudget

    private val _homeLayoutPreset = MutableStateFlow("Daily Tracker")
    val homeLayoutPreset: StateFlow<String> = _homeLayoutPreset

    private val _summary = MutableStateFlow(FinancialSummaryState())
    val summary: StateFlow<FinancialSummaryState> = _summary

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories

    val showSmsDetails = userPreferencesRepository.showSmsDetailsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val userName = userPreferencesRepository.userNameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val userPhone = userPreferencesRepository.userPhoneFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val username = userPreferencesRepository.usernameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val isPremiumUser = userPreferencesRepository.isPremiumUserFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val profileImageUri = userPreferencesRepository.profileImageUriFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _eliteConfig = MutableStateFlow(EliteConfig())
    val eliteConfig: StateFlow<EliteConfig> = _eliteConfig

    private val _eliteSubscription = MutableStateFlow(EliteSubscription())
    val eliteSubscription: StateFlow<EliteSubscription> = _eliteSubscription

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            transactionRepository.startSync(currentUser.uid)
        }
        viewModelScope.launch {
            eliteRepository.getEliteConfig().collectLatest {
                _eliteConfig.value = it
            }
        }
        viewModelScope.launch {
            eliteRepository.getUserSubscription().collectLatest {
                _eliteSubscription.value = it
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.dailyBudgetFlow.collectLatest {
                _dailyBudget.value = it
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.homeLayoutPresetFlow.collectLatest {
                _homeLayoutPreset.value = it
            }
        }
        viewModelScope.launch {
            getCategoriesUseCase.initialize()
            getCategoriesUseCase().collectLatest {
                _categories.value = it
            }
        }
        viewModelScope.launch {
            getTransactionsUseCase().collectLatest {
                _transactions.value = it
            }
        }
        viewModelScope.launch {
            getFinancialSummaryUseCase().collectLatest {
                _summary.value = it
            }
        }
    }

    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            addCategoryUseCase(
                CategoryEntity(
                    name = name,
                    type = type
                )
            )
        }
    }

    fun updateDailyBudget(budget: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyBudget(budget)
        }
    }

    fun updateHomeLayoutPreset(preset: String) {
        viewModelScope.launch {
            userPreferencesRepository.setHomeLayoutPreset(preset)
        }
    }

    fun setPremiumUser(isPremium: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPremiumUserForCurrent(isPremium)
        }
    }

    fun setProfileImageUri(uri: String) {
        viewModelScope.launch {
            userPreferencesRepository.setProfileImageUri(uri)
            PrefsManager(context).updatePhoto(uri)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
            PrefsManager(context).updateName(name)
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveUsername(username)
            PrefsManager(context).updateUsername(username)
        }
    }

    fun updateUserPhone(phone: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserPhone(phone)
            PrefsManager(context).updatePhone(phone)
        }
    }

    fun setSmsTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSmsTrackingEnabled(enabled)
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String, subcategory: String? = null, date: Long) {
        viewModelScope.launch {
            addTransactionUseCase(
                TransactionEntity(
                    merchantName = title,
                    amount = amount,
                    type = type,
                    category = category,
                    subcategory = subcategory,
                    date = date
                )
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, googleSignInOptions).signOut()
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
            // Clear cached profile data to ensure stale state is wiped
            userPreferencesRepository.clearAllData()
            userEntitlementRepository.clearEntitlement()
            transactionRepository.stopSync()
        }
    }
}
