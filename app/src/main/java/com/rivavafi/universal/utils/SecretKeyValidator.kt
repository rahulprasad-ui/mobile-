package com.rivavafi.universal.utils

object SecretKeyValidator {
    /**
     * Sanitizes and normalizes a secret key string.
     */
    fun normalize(key: String): String {
        return key.trim().uppercase().replace("\\s+".toRegex(), "-")
    }

    /**
     * Formats user input as RIV-XXXX-XXXX-XXXX automatically as typed.
     */
    fun formatAsYouType(input: String): String {
        val clean = input.replace("[^A-Za-z0-9]".toRegex(), "").uppercase()
        val builder = StringBuilder()
        for (i in clean.indices) {
            if (i > 0 && (i == 3 || i == 7 || i == 11 || i == 15)) {
                builder.append('-')
            }
            builder.append(clean[i])
        }
        return builder.toString()
    }

    /**
     * Validates minimum structural length before sending to backend for verification.
     */
    fun isValidFormat(key: String): Boolean {
        val clean = key.trim()
        return clean.length >= 6
    }

    @Deprecated("Use asynchronous verifyAndRedeemSecretKey via PremiumViewModel instead")
    fun isValid(key: String): Boolean {
        return isValidFormat(key)
    }
}
