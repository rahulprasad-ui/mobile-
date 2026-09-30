package com.rivavafi.universal.data.model

data class UserModel(
    val uid: String = "",
    val name: String? = null, // Kept for backward compatibility
    val username: String? = null, // Added to map correctly to therivdata
    val email: String? = null,
    val phone: String? = null, // Kept for backward compatibility
    val phoneno: String? = null, // Added to map correctly to therivdata
    val preference: String? = null,
    val profileImage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val loginProvider: String = "",
    val isPhoneVerified: Boolean = false,
    val premiumStatus: Boolean = false // Added to map correctly to therivdata
)
