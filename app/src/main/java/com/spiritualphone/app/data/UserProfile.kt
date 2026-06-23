package com.spiritualphone.app.data

/**
 * Persisted user profile and app settings.
 */
data class UserProfile(
    val nickname: String = "",
    val age: String = "",
    val avatarPath: String? = null,
    val notificationsEnabled: Boolean = true,
)
