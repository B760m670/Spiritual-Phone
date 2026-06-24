package com.spiritualphone.app.data

/**
 * Persisted user profile and app settings.
 */
data class UserProfile(
    val nickname: String = "",
    val age: String = "",
    val username: String = "",      // local @позывной
    val bio: String = "",           // "О себе"
    val avatarPath: String? = null,
    val notificationsEnabled: Boolean = true,
    /** SHA-256 of the app-lock PIN; null = lock disabled. */
    val appLockHash: String? = null,
    /** Stable local identity, generated on first use; becomes the account id
     *  once a real account is linked. Encoded in the profile QR code. */
    val userId: String? = null,
)
