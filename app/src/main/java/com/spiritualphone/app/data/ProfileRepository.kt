package com.spiritualphone.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.profileDataStore by preferencesDataStore(name = "profile")

/**
 * DataStore-backed store for the user profile and settings. A single underlying
 * DataStore is shared process-wide, so multiple instances stay consistent.
 */
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.profileDataStore

    val profile: Flow<UserProfile> = store.data.map { prefs ->
        UserProfile(
            nickname = prefs[NICK].orEmpty(),
            age = prefs[AGE].orEmpty(),
            username = prefs[USERNAME].orEmpty(),
            bio = prefs[BIO].orEmpty(),
            avatarPath = prefs[AVATAR],
            notificationsEnabled = prefs[NOTIFICATIONS] ?: true,
            soundEnabled = prefs[SOUND] ?: true,
            vibrationEnabled = prefs[VIBRATION] ?: true,
            tabLabels = prefs[TAB_LABELS] ?: true,
            appLockHash = prefs[APP_LOCK],
            userId = prefs[USER_ID],
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) = store.edit { it[SOUND] = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean) = store.edit { it[VIBRATION] = enabled }
    suspend fun setTabLabels(enabled: Boolean) = store.edit { it[TAB_LABELS] = enabled }

    /** SHA-256 hex of a PIN — what we persist (never the PIN itself). */
    fun hashPin(pin: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun setAppLock(pin: String) = store.edit { it[APP_LOCK] = hashPin(pin) }
    suspend fun clearAppLock() = store.edit { it.remove(APP_LOCK) }

    suspend fun setNickname(value: String) = store.edit { it[NICK] = value }
    suspend fun setAge(value: String) = store.edit { it[AGE] = value }
    suspend fun setUsername(value: String) = store.edit { it[USERNAME] = value }
    suspend fun setBio(value: String) = store.edit { it[BIO] = value }
    suspend fun setNotificationsEnabled(enabled: Boolean) =
        store.edit { it[NOTIFICATIONS] = enabled }

    suspend fun setAvatarPath(path: String?) = store.edit {
        if (path == null) it.remove(AVATAR) else it[AVATAR] = path
    }

    /** Returns the stable local id, generating + persisting one on first call. */
    suspend fun ensureUserId(): String {
        store.data.first()[USER_ID]?.let { return it }
        val id = UUID.randomUUID().toString()
        store.edit { it[USER_ID] = id }
        return id
    }

    private companion object {
        val NICK = stringPreferencesKey("nickname")
        val AGE = stringPreferencesKey("age")
        val USERNAME = stringPreferencesKey("username")
        val BIO = stringPreferencesKey("bio")
        val AVATAR = stringPreferencesKey("avatar_path")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val TAB_LABELS = booleanPreferencesKey("tab_labels")
        val APP_LOCK = stringPreferencesKey("app_lock_hash")
        val USER_ID = stringPreferencesKey("user_id")
    }
}
