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

// Creates a DataStore instance for user preferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
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
            preferences.clear()
        }
    }

    suspend fun saveDarkModePreference(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }
}