package com.rivavafi.universal.utils

/**
 * Configuration for secret key prefix and metadata.
 * Note: Real license validation and key generation logic is securely managed
 * exclusively on the Node.js backend.
 */
object SecretConfig {
    const val KEY_PREFIX = "RIV"
    const val MIN_KEY_LENGTH = 6
}
