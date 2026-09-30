package com.rivavafi.universal.utils

import android.content.Context
import android.content.SharedPreferences
import com.rivavafi.universal.data.model.User

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_UID = "uid"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO = "photo"
        private const val KEY_PHONE = "phone"
        private const val KEY_USERNAME = "username"
    }

    fun saveUser(user: User) {
        val existingUid = prefs.getString(KEY_UID, null)
        val sameUser = existingUid == user.uid

        prefs.edit().apply {
            putString(KEY_UID, user.uid)
            putString(KEY_EMAIL, user.email)
            putString(KEY_NAME, chooseCachedValue(sameUser, KEY_NAME, user.name))
            putString(KEY_PHOTO, chooseCachedValue(sameUser, KEY_PHOTO, user.photo))
            putString(KEY_PHONE, chooseCachedValue(sameUser, KEY_PHONE, user.phone))
            apply()
        }
    }

    fun updateName(name: String) {
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun updatePhone(phone: String) {
        prefs.edit().putString(KEY_PHONE, phone).apply()
    }

    fun updatePhoto(photo: String) {
        prefs.edit().putString(KEY_PHOTO, photo).apply()
    }

    fun updateUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getUser(): User? {
        val uid = prefs.getString(KEY_UID, null) ?: return null
        return User(
            uid = uid,
            name = prefs.getString(KEY_NAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            photo = prefs.getString(KEY_PHOTO, null),
            phone = prefs.getString(KEY_PHONE, null)
        )
    }

    private fun chooseCachedValue(sameUser: Boolean, key: String, incomingValue: String?): String? {
        val cachedValue = prefs.getString(key, null)
        return if (sameUser && !cachedValue.isNullOrBlank()) {
            cachedValue
        } else {
            incomingValue
        }
    }
}
