package com.nadavariel.dietapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// At the top level of your file (outside any class)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        // ⭐ NEW: Add the key for dark mode preference
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }

    val userEmailFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] ?: ""
        }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] ?: false
        }

    // ⭐ NEW: Flow to expose the dark mode preference
    val darkModeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] ?: false // Default to false (light mode)
        }

    suspend fun saveUserPreferences(email: String, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearUserPreferences() {
        context.dataStore.edit { preferences ->
            // This will clear everything in DataStore, which is fine since it only holds email/rememberMe now
            // Ensure dark mode preference isn't accidentally cleared if you want it to persist across sign-outs.
            // For now, it clears everything, so dark mode will reset to default.
            // If you want dark mode to persist independently, you'd need a separate DataStore or selective clearing.
            preferences.clear()
        }
    }

    // ⭐ NEW: Function to save dark mode preference
    suspend fun saveDarkModePreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }
}