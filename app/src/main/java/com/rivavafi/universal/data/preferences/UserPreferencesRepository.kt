package com.rivavafi.universal.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        val IS_SMS_TRACKING_ENABLED_KEY = booleanPreferencesKey("is_sms_tracking_enabled")
        val SMS_SCAN_COMPLETED_KEY = booleanPreferencesKey("sms_scan_completed")
        val DAILY_BUDGET_KEY = stringPreferencesKey("daily_budget")
        val HOME_LAYOUT_PRESET_KEY = stringPreferencesKey("home_layout_preset")
        val SHOW_SMS_DETAILS_KEY = booleanPreferencesKey("show_sms_details")
        val UNLOCKED_PREMIUM_USERS_KEY = stringPreferencesKey("unlocked_premium_users")
        val IS_PREMIUM_GLOBAL_KEY = booleanPreferencesKey("is_premium_global")
        val PROFILE_IMAGE_URI_KEY = stringPreferencesKey("profile_image_uri")
        val USER_USERNAME_KEY = stringPreferencesKey("user_username")
        val SMS_TRACKING_MODE_KEY = stringPreferencesKey("sms_tracking_mode")
        val TERMINOLOGY_MODE_KEY = stringPreferencesKey("terminology_mode")
        val LAST_SMS_SCAN_DATE_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_sms_scan_date")
    }


    val terminologyModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[TERMINOLOGY_MODE_KEY] ?: "CREDIT_DEBIT"
    }

    suspend fun setTerminologyMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[TERMINOLOGY_MODE_KEY] = mode
        }
    }


    val profileImageUriFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PROFILE_IMAGE_URI_KEY]
    }

    suspend fun setProfileImageUri(uri: String) {
        dataStore.edit { preferences ->
            val existingUri = preferences[PROFILE_IMAGE_URI_KEY]
            val existingIsLocal = existingUri?.startsWith("/") == true || existingUri?.startsWith("file:") == true
            val newIsRemote = uri.startsWith("http://") || uri.startsWith("https://")

            if (existingIsLocal && newIsRemote) {
                return@edit
            }

            preferences[PROFILE_IMAGE_URI_KEY] = uri
        }
    }

    val isPremiumUserFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        val currentUser = preferences[USER_NAME_KEY]?.lowercase()?.trim()
        val unlockedUsersStr = preferences[UNLOCKED_PREMIUM_USERS_KEY] ?: ""
        val unlockedUsers = unlockedUsersStr.split(",").filter { it.isNotBlank() }.toSet()

        when {
            !currentUser.isNullOrBlank() -> unlockedUsers.contains(currentUser)
            else -> preferences[IS_PREMIUM_GLOBAL_KEY] ?: false
        }
    }

    suspend fun setPremiumUserForCurrent(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM_GLOBAL_KEY] = isPremium

            val currentUser = preferences[USER_NAME_KEY]?.lowercase()?.trim()
            if (!currentUser.isNullOrBlank()) {
                val unlockedUsersStr = preferences[UNLOCKED_PREMIUM_USERS_KEY] ?: ""
                val unlockedUsers = unlockedUsersStr.split(",").filter { it.isNotBlank() }.toMutableSet()

                if (isPremium) {
                    unlockedUsers.add(currentUser)
                } else {
                    unlockedUsers.remove(currentUser)
                }

                preferences[UNLOCKED_PREMIUM_USERS_KEY] = unlockedUsers.joinToString(",")
            }
        }
    }

    val showSmsDetailsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_SMS_DETAILS_KEY] ?: true
    }

    suspend fun setShowSmsDetails(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_SMS_DETAILS_KEY] = show
        }
    }

    val homeLayoutPresetFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[HOME_LAYOUT_PRESET_KEY] ?: "Daily Tracker"
    }

    suspend fun setHomeLayoutPreset(preset: String) {
        dataStore.edit { preferences ->
            preferences[HOME_LAYOUT_PRESET_KEY] = preset
        }
    }

    val dailyBudgetFlow: Flow<Double> = dataStore.data.map { preferences ->
        preferences[DAILY_BUDGET_KEY]?.toDoubleOrNull() ?: 800.0
    }

    suspend fun setDailyBudget(budget: Double) {
        dataStore.edit { preferences ->
            preferences[DAILY_BUDGET_KEY] = budget.toString()
        }
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userPhoneFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_PHONE_KEY]
    }

    val usernameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_USERNAME_KEY]
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ONBOARDING_KEY] ?: false
    }

    val isSmsTrackingEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_SMS_TRACKING_ENABLED_KEY] ?: false
    }


    val smsTrackingModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[SMS_TRACKING_MODE_KEY] ?: "OFF"
    }

    suspend fun setSmsTrackingMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[SMS_TRACKING_MODE_KEY] = mode
            preferences[IS_SMS_TRACKING_ENABLED_KEY] = mode != "OFF"
        }
    }

    val smsScanCompletedFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SMS_SCAN_COMPLETED_KEY] ?: false
    }

    suspend fun getUserName(): String? {
        val prefs = dataStore.data.first()
        return prefs[USER_NAME_KEY]
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { preferences ->
            if (preferences[USER_NAME_KEY].isNullOrBlank()) {
                preferences[USER_NAME_KEY] = name
            }
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun saveUserPhone(phone: String) {
        dataStore.edit { preferences ->
            if (preferences[USER_PHONE_KEY].isNullOrBlank()) {
                preferences[USER_PHONE_KEY] = phone
            }
        }
    }

    suspend fun setUserPhone(phone: String) {
        dataStore.edit { preferences ->
            preferences[USER_PHONE_KEY] = phone
        }
    }

    suspend fun saveUsername(username: String) {
        dataStore.edit { preferences ->
            preferences[USER_USERNAME_KEY] = username
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING_KEY] = completed
        }
    }

    suspend fun setSmsTrackingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_SMS_TRACKING_ENABLED_KEY] = enabled
        }
    }

    suspend fun setSmsScanCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[SMS_SCAN_COMPLETED_KEY] = completed
        }
    }

    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }


    suspend fun setLastSmsScanDate(date: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_SMS_SCAN_DATE_KEY] = date
        }
    }

    val lastSmsScanDateFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_SMS_SCAN_DATE_KEY] ?: 0L
    }
}
